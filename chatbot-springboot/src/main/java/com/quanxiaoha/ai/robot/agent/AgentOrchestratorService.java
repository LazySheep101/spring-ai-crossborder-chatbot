package com.quanxiaoha.ai.robot.agent;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 精简版意图分流：输出 specialist + 工具白名单。
 */
@Service
public class AgentOrchestratorService {

    public SpecialistPlan plan(String userMessage) {
        String message = userMessage == null ? "" : userMessage.toLowerCase(Locale.ROOT);
        boolean angry = containsAny(message, "投诉", "生气", "愤怒", "骗子", "差评", "人工", "客服");

        if (containsAny(message, "退货", "退款", "换货", "取消订单", "改地址", "修改地址", "收货地址", "改址")) {
            return new SpecialistPlan("return", "退货/退款智能体",
                    List.of("queryOrder", "createReturnRequest", "requestRefundOrReplacement",
                            "requestAddressChange", "escalateToHuman", "refundPolicyRAG", "translateSnippet"),
                    "HIGH", true, true, angry);
        }
        if (containsAny(message, "物流", "快递", "运单", "到哪了", "发货")) {
            return new SpecialistPlan("order", "订单/物流智能体",
                    angry ? List.of("queryOrder", "trackLogistics", "escalateToHuman")
                            : List.of("queryOrder", "trackLogistics"),
                    angry ? "HIGH" : "MEDIUM", true, false, angry);
        }
        if (containsAny(message, "订单", "查单", " ord-", "ord-")) {
            return new SpecialistPlan("order", "订单智能体",
                    angry ? List.of("queryOrder", "escalateToHuman") : List.of("queryOrder"),
                    "MEDIUM", true, false, angry);
        }
        if (containsAny(message, "商品", "库存", "多少钱", "推荐", "卫衣", "袜子", "保温杯", "sku")) {
            return new SpecialistPlan("product", "商品顾问智能体",
                    List.of("searchProductCatalog"), "LOW", false, false, false);
        }
        if (containsAny(message, "政策", "规则", "保修", "运费", "怎么退", "能否退", "可以退吗")) {
            return new SpecialistPlan("policy_rag", "政策 RAG 智能体",
                    List.of("refundPolicyRAG", "translateSnippet"), "MEDIUM", false, false, false);
        }
        if (angry || containsAny(message, "转人工", "找人工", "真人")) {
            return new SpecialistPlan("handoff", "人工交接智能体",
                    List.of("escalateToHuman"), "HIGH", false, false, true);
        }
        // 默认开放常用只读工具 + 政策，避免误分流导致无工具
        return new SpecialistPlan("triage", "综合客服智能体",
                List.of("queryOrder", "trackLogistics", "searchProductCatalog",
                        "refundPolicyRAG", "escalateToHuman", "translateSnippet"),
                "LOW", false, false, false);
    }

    public List<Map<String, Object>> catalog() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(spec("return", "退货/退款智能体", "HIGH",
                "退货、退款、换货、取消订单、改地址、收货地址",
                List.of("queryOrder", "createReturnRequest", "requestRefundOrReplacement",
                        "requestAddressChange", "refundPolicyRAG", "translateSnippet", "escalateToHuman"),
                "写操作只创建 PENDING 审批单，不直接改账"));
        rows.add(spec("order", "订单/物流智能体", "MEDIUM",
                "订单、物流、快递、运单、发货",
                List.of("queryOrder", "trackLogistics", "escalateToHuman"),
                "查单和物流必须校验邮箱或手机"));
        rows.add(spec("product", "商品顾问智能体", "LOW",
                "商品、库存、价格、SKU",
                List.of("searchProductCatalog"),
                "只读商品目录"));
        rows.add(spec("policy_rag", "政策 RAG 智能体", "MEDIUM",
                "政策、规则、保修、运费、能否退",
                List.of("refundPolicyRAG", "translateSnippet"),
                "混合检索 + 内置中英 FAQ，引用随回答返回"));
        rows.add(spec("handoff", "人工交接智能体", "HIGH",
                "投诉、转人工、找真人",
                List.of("escalateToHuman"),
                "工单绑定当前 chatId，收件箱回同一会话"));
        rows.add(spec("triage", "综合客服智能体", "LOW",
                "未命中以上关键词时的默认分流",
                List.of("queryOrder", "trackLogistics", "searchProductCatalog",
                        "refundPolicyRAG", "translateSnippet", "escalateToHuman"),
                "默认不开放写工具"));
        return rows;
    }

    private static Map<String, Object> spec(String key, String label, String risk, String when,
                                            List<String> tools, String boundary) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("specialistKey", key);
        m.put("label", label);
        m.put("riskLevel", risk);
        m.put("when", when);
        m.put("tools", tools);
        m.put("boundary", boundary);
        return m;
    }

    private boolean containsAny(String message, String... keywords) {
        for (String k : keywords) {
            if (message.contains(k.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public record SpecialistPlan(
            String specialistKey,
            String specialistLabel,
            List<String> toolAllowlist,
            String riskLevel,
            boolean requiresIdentityVerification,
            boolean requiresApproval,
            boolean recommendHumanHandoff) {
    }
}
