package com.quanxiaoha.ai.robot.service.impl;

import com.quanxiaoha.ai.robot.config.RagProperties;
import com.quanxiaoha.ai.robot.service.EsChunkService;
import com.quanxiaoha.ai.robot.service.HybridRetrievalService;
import com.quanxiaoha.ai.robot.service.RerankService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: lazysheep101
 * @Date: 2026/10/26
 * @Version: v1.0.0
 * @Description: 混合检索实现：双路召回 + RRF 融合 +（可选）Rerank 精排
 **/
@Service
@Slf4j
public class HybridRetrievalServiceImpl implements HybridRetrievalService {

    @Resource
    private VectorStore vectorStore;
    @Resource
    private EsChunkService esChunkService;
    @Resource
    private RagProperties ragProperties;
    @Resource
    private RerankService rerankService;

    @Override
    public List<Document> retrieve(String query) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }

        RagProperties.Hybrid hybrid = ragProperties.getHybrid();
        RagProperties.Rerank rerank = ragProperties.getRerank();
        boolean useRerank = rerank.isEnabled();
        int finalTopK = hybrid.getFinalTopK();
        // 开启 Rerank 时：召回与融合先多留候选，再精排截断；关闭时直接截到 finalTopK
        int poolTopK = useRerank ? Math.max(rerank.getCandidateTopK(), finalTopK) : finalTopK;
        int recallTopK = Math.max(hybrid.getCandidateTopK(), useRerank ? rerank.getCandidateTopK() : hybrid.getCandidateTopK());

        // 1) 向量路：语义召回
        List<Document> vectorDocs = vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(recallTopK)
                .build());
        log.info("## 向量召回完成, 命中: {}, recallTopK: {}", vectorDocs == null ? 0 : vectorDocs.size(), recallTopK);

        // 未开混合检索，或 ES 双写总开关关闭 → 纯向量候选后再决定是否 Rerank
        boolean useHybrid = hybrid.isEnabled() && ragProperties.getElasticsearch().isEnabled();
        if (!useHybrid) {
            return finalizeDocuments(query, limit(vectorDocs, poolTopK), finalTopK, useRerank);
        }

        // 2) 关键词路：ES match；异常时降级纯向量，不影响主对话
        List<Document> esDocs = List.of();
        try {
            esDocs = esChunkService.search(query, recallTopK);
            log.info("## ES 召回完成, 命中: {}", esDocs.size());
        } catch (Exception ex) {
            log.error("## ES 召回失败，降级为纯向量检索, query: {}", query, ex);
            return finalizeDocuments(query, limit(vectorDocs, poolTopK), finalTopK, useRerank);
        }

        if (CollectionUtils.isEmpty(esDocs)) {
            return finalizeDocuments(query, limit(vectorDocs, poolTopK), finalTopK, useRerank);
        }

        // 3) RRF 融合去重（开启 Rerank 时保留更大候选池）
        List<Document> fused = reciprocalRankFusion(vectorDocs, esDocs, hybrid.getRrfK(), poolTopK);
        log.info("## RRF 融合完成, 候选条数: {}, poolTopK: {}", fused.size(), poolTopK);

        // 4) 可选 Rerank 精排 → finalTopK
        return finalizeDocuments(query, fused, finalTopK, useRerank);
    }

    /**
     * 有 Rerank：精排后截断；失败或关闭：按原序截断 finalTopK
     */
    private List<Document> finalizeDocuments(String query,
                                             List<Document> candidates,
                                             int finalTopK,
                                             boolean useRerank) {
        if (CollectionUtils.isEmpty(candidates)) {
            return List.of();
        }
        if (!useRerank) {
            return limit(candidates, finalTopK);
        }
        if (candidates.size() == 1) {
            return candidates;
        }

        try {
            List<Document> reranked = rerankService.rerank(query, candidates, finalTopK);
            if (CollectionUtils.isEmpty(reranked)) {
                log.warn("## Rerank 返回空，降级为融合/召回原序截断");
                return limit(candidates, finalTopK);
            }
            log.info("## 全链路完成(召回→融合→Rerank), 最终进 Prompt: {}", reranked.size());
            return reranked;
        } catch (Exception ex) {
            log.error("## Rerank 失败，降级为融合/召回原序截断, query: {}", query, ex);
            return limit(candidates, finalTopK);
        }
    }

    /**
     * Reciprocal Rank Fusion：score(d) = Σ 1 / (k + rank)
     * 同一文档 ID 在多路出现时分数累加，天然完成去重与粗排。
     */
    private List<Document> reciprocalRankFusion(List<Document> vectorDocs,
                                                List<Document> esDocs,
                                                int rrfK,
                                                int poolTopK) {
        Map<String, Double> scoreById = new HashMap<>();
        // 保留正文：同 ID 优先保留向量路文档（语义块与入库一致）
        Map<String, Document> docById = new LinkedHashMap<>();

        accumulateRank(vectorDocs, rrfK, scoreById, docById, true);
        accumulateRank(esDocs, rrfK, scoreById, docById, false);

        return scoreById.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(poolTopK)
                .map(entry -> {
                    Document doc = docById.get(entry.getKey());
                    Map<String, Object> metadata = new HashMap<>(doc.getMetadata());
                    metadata.put("rrfScore", entry.getValue());
                    return Document.builder()
                            .id(doc.getId())
                            .text(doc.getText())
                            .metadata(metadata)
                            .build();
                })
                .toList();
    }

    private void accumulateRank(List<Document> docs,
                                int rrfK,
                                Map<String, Double> scoreById,
                                Map<String, Document> docById,
                                boolean preferAsCanonical) {
        if (CollectionUtils.isEmpty(docs)) {
            return;
        }
        for (int i = 0; i < docs.size(); i++) {
            Document doc = docs.get(i);
            String id = doc.getId();
            if (!StringUtils.hasText(id)) {
                continue;
            }
            // rank 从 1 开始
            double add = 1.0 / (rrfK + i + 1);
            scoreById.merge(id, add, Double::sum);

            if (preferAsCanonical || !docById.containsKey(id)) {
                docById.put(id, doc);
            }
        }
    }

    private List<Document> limit(List<Document> docs, int topK) {
        if (CollectionUtils.isEmpty(docs)) {
            return List.of();
        }
        if (docs.size() <= topK) {
            return docs;
        }
        return new ArrayList<>(docs.subList(0, topK));
    }
}
