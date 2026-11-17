package com.quanxiaoha.ai.robot.agent.tool;

import com.quanxiaoha.ai.robot.agent.AgentSideChannel;
import com.quanxiaoha.ai.robot.agent.ToolAuditService;
import com.quanxiaoha.ai.robot.commerce.CommerceDtos;
import com.quanxiaoha.ai.robot.i18n.FaqCorpus;
import com.quanxiaoha.ai.robot.service.HybridRetrievalService;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class PolicyTools {

    @Resource
    private HybridRetrievalService hybridRetrievalService;
    @Resource
    private ToolAuditService toolAuditService;
    @Resource
    private FaqCorpus faqCorpus;

    @Tool(description = """
            Search refund/return/exchange policy knowledge base via hybrid retrieval + rerank. \
            Use for policy questions: refunds, returns, warranties, return windows, shipping costs.
            """)
    public CommerceDtos.PolicyAnswer refundPolicyRAG(
            @ToolParam(description = "Customer policy question") String question) {
        return toolAuditService.record("refundPolicyRAG", Map.of("question", question == null ? "" : question), () -> {
            try {
            List<Document> docs = List.of();
            try {
                List<Document> retrieved = hybridRetrievalService.retrieve(question);
                if (retrieved != null) {
                    docs = retrieved;
                }
            } catch (Exception ignored) {
                docs = List.of();
            }
            List<CommerceDtos.PolicyChunk> citations = new ArrayList<>();
            StringBuilder hint = new StringBuilder();
            for (Document doc : docs) {
                Double rerank = toDouble(doc.getMetadata().get("rerankScore"));
                Double rrf = toDouble(doc.getMetadata().get("rrfScore"));
                citations.add(new CommerceDtos.PolicyChunk(doc.getId(), doc.getText(), rerank, rrf));
                hint.append(doc.getText()).append("\n---\n");
            }
            for (CommerceDtos.PolicyChunk faq : faqCorpus.search(question, 2)) {
                    boolean duplicate = citations.stream().anyMatch(c ->
                            c.text() != null && faq.text() != null && c.text().contains(faq.text().substring(0, Math.min(24, faq.text().length()))));
                    if (!duplicate) {
                        citations.add(faq);
                        hint.append(faq.text()).append("\n---\n");
                    }
                }
                if (citations.isEmpty()) {
                    return CommerceDtos.PolicyAnswer.error("未检索到相关政策，可转人工或请客户换个问法。");
                }
                CommerceDtos.PolicyAnswer answer = new CommerceDtos.PolicyAnswer(
                        true, hint.toString(), citations, "政策检索成功，请基于 citations 回答。");
                AgentSideChannel.addCitation(answer);
                return answer;
            } catch (Exception e) {
                return CommerceDtos.PolicyAnswer.error("政策检索失败: " + e.getMessage());
            }
        });
    }

    private static Double toDouble(Object v) {
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        return null;
    }
}
