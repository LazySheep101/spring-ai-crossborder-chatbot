package com.quanxiaoha.ai.robot.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.quanxiaoha.ai.robot.config.RagProperties;
import com.quanxiaoha.ai.robot.service.EsChunkService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: lazysheep101
 * @Date: 2026/10/19 15:52
 * @Version: v1.0.0
 * @Description: Elasticsearch 文档块
 **/
@Service
@Slf4j
public class EsChunkServiceImpl implements EsChunkService {

    @Resource
    private ElasticsearchClient elasticsearchClient;
    @Resource
    private RagProperties ragProperties;

    /**
     * 确保 ES 索引存在：首次写入时检查，不存在则按预定义映射创建
     *
     * @throws IOException
     */
    @Override
    public void ensureIndex() throws IOException {
        // 索引名称
        String indexName = ragProperties.getElasticsearch().getIndexName();

        // 查询索引是否存在
        boolean exists = elasticsearchClient.indices()
                .exists(e -> e.index(indexName))
                .value();

        // 若存在，直接 return
        if (exists) {
            return;
        }

        // 创建索引与映射
        elasticsearchClient.indices().create(c -> c
                .index(indexName)
                // 本地环境中，设置 1 个主分片、0 个副本即可
                .settings(s -> s.numberOfShards("1").numberOfReplicas("0"))
                .mappings(m -> m
                        // 文档块正文：建索引用 ik_max_word（切得细），查询用 ik_smart（词干净，减少误命中）
                        .properties("content", p -> p.text(t -> t
                                .analyzer("ik_max_word")
                                .searchAnalyzer("ik_smart")))
                        .properties("file_id", p -> p.keyword(k -> k)) // 归属文件 ID（文件存储表主键）
                        .properties("original_file_name", p -> p.keyword(k -> k)))); // 原始文件名：用于按文件名过滤

        log.info("## ES 索引 [{}] 创建成功", indexName);
    }

    /**
     * 批量写入文档块
     *
     * @param documents
     * @throws IOException
     */
    @Override
    public void saveChunks(List<Document> documents) throws IOException {
        // 首次写入前，先确保索引和映射已建好
        ensureIndex();

        // 索引名称
        String indexName = ragProperties.getElasticsearch().getIndexName();

        // 批量写入文档
        BulkRequest.Builder bulkRequest = new BulkRequest.Builder();
        for (Document doc : documents) {
            Map<String, Object> metadata = doc.getMetadata();

            // 写入 ES 的文档字段：正文 + 归属文件信息
            Map<String, Object> esDoc = new HashMap<>();
            esDoc.put("content", doc.getText());
            esDoc.put("file_id", String.valueOf(metadata.get("mdStorageId")));
            esDoc.put("original_file_name", String.valueOf(metadata.get("originalFileName")));

            bulkRequest.operations(op -> op.index(idx -> idx
                    .index(indexName)
                    // 文档 _id 直接复用 PGVector 中的记录 ID
                    // 同一内容重复写入会覆盖同一条文档，保证幂等性
                    .id(doc.getId())
                    .document(esDoc)));
        }

        BulkResponse bulkResponse = elasticsearchClient.bulk(bulkRequest.build());

        // bulk 是 “整体请求成功、单条可能失败” 的模式，必须逐条检查错误
        if (bulkResponse.errors()) {
            bulkResponse.items().forEach(item -> {
                if (item.error() != null) {
                    log.error("## ES 写入失败, docId: {}, 原因: {}", item.id(), item.error().reason());
                }
            });
            throw new IOException("ES 批量写入存在失败项");
        }

        log.info("## ES 批量写入成功, 本批文档块数量: {}", documents.size());
    }

    /**
     * 按文件 ID 删除该文件写入 ES 的所有文档块
     *
     * @param fileId
     * @throws IOException
     */
    @Override
    public void deleteByFileId(Long fileId) throws IOException {
        String indexName = ragProperties.getElasticsearch().getIndexName();

        DeleteByQueryResponse response = elasticsearchClient.deleteByQuery(d -> d
                .index(indexName)
                .query(q -> q.term(t -> t
                        .field("file_id")
                        .value(String.valueOf(fileId))))
                // 删除后立即刷新索引，让变更立马生效
                .refresh(true));

        log.info("## ES 按文件删除完成, fileId: {}, 删除文档块数量: {}", fileId, response.deleted());
    }

    /**
     * 关键词检索：match content，按相关性得分取 topK
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Document> search(String query, int topK) throws IOException {
        String indexName = ragProperties.getElasticsearch().getIndexName();

        // 索引尚未创建时（从未双写过）直接返回空，避免 search 抛 index_not_found
        boolean exists = elasticsearchClient.indices()
                .exists(e -> e.index(indexName))
                .value();
        if (!exists) {
            log.warn("## ES 索引 [{}] 不存在，关键词检索返回空列表", indexName);
            return List.of();
        }

        SearchResponse<Map> response;
        try {
            response = elasticsearchClient.search(s -> s
                            .index(indexName)
                            .size(topK)
                            // content 查询侧走 mapping 里的 search_analyzer=ik_smart
                            .query(q -> q.match(m -> m
                                    .field("content")
                                    .query(query))),
                    Map.class);
        } catch (ElasticsearchException ex) {
            throw new IOException("ES 关键词检索失败: " + ex.getMessage(), ex);
        }

        List<Document> documents = new ArrayList<>();
        for (Hit<Map> hit : response.hits().hits()) {
            Map<String, Object> source = hit.source();
            if (source == null) {
                continue;
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("mdStorageId", source.get("file_id"));
            metadata.put("originalFileName", source.get("original_file_name"));
            // ES 相关性得分，融合阶段可观测；RRF 本身只用排名
            if (hit.score() != null) {
                metadata.put("score", hit.score());
            }

            documents.add(Document.builder()
                    .id(hit.id())
                    .text(String.valueOf(source.getOrDefault("content", "")))
                    .metadata(metadata)
                    .build());
        }

        log.info("## ES 关键词检索完成, query: {}, 命中: {}", query, documents.size());
        return documents;
    }
}
