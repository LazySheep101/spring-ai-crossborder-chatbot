package com.quanxiaoha.ai.robot.service;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * @Author: lazysheep101
 * @Date: 2026/10/26
 * @Version: v1.0.0
 * @Description: 混合检索（向量 + 关键词 → RRF 融合 → 可选 Rerank）
 **/
public interface HybridRetrievalService {

    /**
     * 按用户问题召回文档块：
     * 向量 /（可选）ES → RRF 融合 →（可选）Rerank 精排 → finalTopK
     *
     * @param query 用户问题
     * @return 最终用于拼 Prompt 的文档列表
     */
    List<Document> retrieve(String query);

}
