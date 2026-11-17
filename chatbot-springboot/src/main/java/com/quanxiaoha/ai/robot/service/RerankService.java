package com.quanxiaoha.ai.robot.service;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * @Author: lazysheep101
 * @Date: 2026/10/29
 * @Version: v1.0.0
 * @Description: 文档重排序（Rerank）：对召回候选按「问题-正文」相关度精排
 **/
public interface RerankService {

    /**
     * 对候选文档重排序，返回按相关度从高到低的前 topN 条
     *
     * @param query      用户问题
     * @param candidates 召回/融合后的候选（顺序可乱）
     * @param topN       最终需要的条数
     * @return 精排后的文档；失败时应由调用方降级，本方法可抛异常或返回空
     */
    List<Document> rerank(String query, List<Document> candidates, int topN);
}
