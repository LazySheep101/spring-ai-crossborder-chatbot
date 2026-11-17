package com.quanxiaoha.ai.robot;

import com.quanxiaoha.ai.robot.service.HybridRetrievalService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * @Author: lazysheep101
 * @Date: 2026/10/29
 * @Version: v1.0.0
 * @Description: 验证全链路检索（向量 + ES → RRF → Rerank）及 metadata 中的 rerankScore
 **/
@SpringBootTest
@Slf4j
public class RerankRetrievalTests {

    @Resource
    private HybridRetrievalService hybridRetrievalService;

    @Test
    void retrieveWithRerank() {
        String question = "怎么加入星球？";
        List<Document> documents = hybridRetrievalService.retrieve(question);

        log.info("==================================================");
        log.info("【问题】{}", question);
        log.info("【最终条数】{}", documents.size());
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            Object rerankScore = doc.getMetadata().get("rerankScore");
            Object rrfScore = doc.getMetadata().get("rrfScore");
            String preview = doc.getText() == null ? "" :
                    doc.getText().substring(0, Math.min(80, doc.getText().length())).replace("\n", " ");
            log.info("[{}] rerankScore={}, rrfScore={}, preview={}", i + 1, rerankScore, rrfScore, preview);
        }
        log.info("==================================================");
    }
}
