package com.quanxiaoha.ai.robot.service.impl;

import com.quanxiaoha.ai.robot.config.RagProperties;
import com.quanxiaoha.ai.robot.service.RerankService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: lazysheep101
 * @Date: 2026/10/29
 * @Version: v1.0.0
 * @Description: 调用阿里云百炼 Text Rerank HTTP 接口做精排
 **/
@Service
@Slf4j
public class DashScopeRerankServiceImpl implements RerankService {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String RERANK_PATH = "/api/v1/services/rerank/text-rerank/text-rerank";

    @Resource
    private OkHttpClient okHttpClient;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private RagProperties ragProperties;

    @Value("${spring.ai.openai.base-url:}")
    private String openAiBaseUrl;

    @Value("${spring.ai.openai.api-key:}")
    private String openAiApiKey;

    @Override
    public List<Document> rerank(String query, List<Document> candidates, int topN) {
        if (!StringUtils.hasText(query) || CollectionUtils.isEmpty(candidates) || topN <= 0) {
            return List.of();
        }

        RagProperties.Rerank cfg = ragProperties.getRerank();
        String apiKey = resolveApiKey(cfg);
        String apiUrl = resolveApiUrl(cfg);
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(apiUrl)) {
            throw new IllegalStateException("Rerank 未配置 api-key 或 api-url，且无法从 spring.ai.openai 推导");
        }

        List<String> texts = new ArrayList<>(candidates.size());
        for (Document doc : candidates) {
            texts.add(truncate(doc.getText(), cfg.getMaxDocumentChars()));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", cfg.getModel());

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("query", query);
        input.put("documents", texts);
        body.put("input", input);

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("top_n", Math.min(topN, candidates.size()));
        parameters.put("return_documents", false);
        if (StringUtils.hasText(cfg.getInstruct())
                && cfg.getModel() != null
                && cfg.getModel().toLowerCase().contains("qwen")) {
            parameters.put("instruct", cfg.getInstruct());
        }
        body.put("parameters", parameters);

        String jsonBody = objectMapper.writeValueAsString(body);
        log.info("## Rerank 请求开始, model: {}, candidates: {}, topN: {}, url: {}",
                cfg.getModel(), candidates.size(), topN, apiUrl);

        Request request = new Request.Builder()
                .url(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, JSON))
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IllegalStateException("Rerank HTTP " + response.code() + ": " + responseBody);
            }

            JsonNode root = objectMapper.readValue(responseBody, JsonNode.class);
            if (root.hasNonNull("code")) {
                throw new IllegalStateException("Rerank 业务错误: "
                        + root.path("code").asText() + " / " + root.path("message").asText());
            }

            JsonNode results = root.path("output").path("results");
            if (!results.isArray() || results.size() == 0) {
                log.warn("## Rerank 响应无 results: {}", responseBody);
                return List.of();
            }

            double threshold = cfg.getScoreThreshold();
            List<Document> ranked = new ArrayList<>();
            for (JsonNode item : results) {
                int index = item.path("index").asInt(-1);
                double score = item.path("relevance_score").asDouble(0.0);
                if (index < 0 || index >= candidates.size()) {
                    continue;
                }
                if (threshold > 0 && score < threshold) {
                    log.info("## Rerank 过滤低分文档, index: {}, score: {}, threshold: {}",
                            index, score, threshold);
                    continue;
                }

                Document src = candidates.get(index);
                Map<String, Object> metadata = new HashMap<>(src.getMetadata());
                metadata.put("rerankScore", score);
                metadata.put("rerankIndex", index);
                ranked.add(Document.builder()
                        .id(src.getId())
                        .text(src.getText())
                        .metadata(metadata)
                        .build());

                if (ranked.size() >= topN) {
                    break;
                }
            }

            // 阈值过滤后为空时，至少保留分数最高的一条，避免客服完全无上下文
            if (ranked.isEmpty() && results.isArray()) {
                JsonNode first = results.get(0);
                int index = first.path("index").asInt(-1);
                if (index >= 0 && index < candidates.size()) {
                    Document src = candidates.get(index);
                    Map<String, Object> metadata = new HashMap<>(src.getMetadata());
                    metadata.put("rerankScore", first.path("relevance_score").asDouble(0.0));
                    ranked.add(Document.builder()
                            .id(src.getId())
                            .text(src.getText())
                            .metadata(metadata)
                            .build());
                }
            }

            log.info("## Rerank 完成, 返回条数: {}", ranked.size());
            return ranked;
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("调用 Rerank 接口失败: " + ex.getMessage(), ex);
        }
    }

    private String resolveApiKey(RagProperties.Rerank cfg) {
        if (StringUtils.hasText(cfg.getApiKey())) {
            return cfg.getApiKey();
        }
        return openAiApiKey;
    }

    /**
     * 优先用显式 api-url；否则从 OpenAI 兼容 base-url 推导 MaaS 文本排序地址：
     * https://{workspace}.cn-beijing.maas.aliyuncs.com/compatible-mode/v1
     * → https://{workspace}.cn-beijing.maas.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank
     */
    private String resolveApiUrl(RagProperties.Rerank cfg) {
        if (StringUtils.hasText(cfg.getApiUrl())) {
            return cfg.getApiUrl().trim();
        }
        if (!StringUtils.hasText(openAiBaseUrl)) {
            return "";
        }
        String base = openAiBaseUrl.trim();
        int compatibleIdx = base.indexOf("/compatible-mode");
        if (compatibleIdx > 0) {
            return base.substring(0, compatibleIdx) + RERANK_PATH;
        }
        // 已是 .../api/v1 之类
        if (base.endsWith("/api/v1")) {
            return base + "/services/rerank/text-rerank/text-rerank";
        }
        if (base.contains("maas.aliyuncs.com") && !base.contains(RERANK_PATH)) {
            // 去掉末尾路径只留 host 根，再拼标准 path 较难；退回 dashscope 公网入口
            return "https://dashscope.aliyuncs.com" + RERANK_PATH;
        }
        return "https://dashscope.aliyuncs.com" + RERANK_PATH;
    }

    private static String truncate(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        if (maxChars <= 0 || text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars);
    }
}
