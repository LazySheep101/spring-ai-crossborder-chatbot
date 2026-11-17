package com.quanxiaoha.ai.robot.agent.tool;

import com.quanxiaoha.ai.robot.agent.AgentSideChannel;
import com.quanxiaoha.ai.robot.agent.ToolAuditService;
import com.quanxiaoha.ai.robot.commerce.CommerceDtos;
import com.quanxiaoha.ai.robot.helpdesk.HelpdeskService;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class EscalationTools {

    @Resource
    private HelpdeskService helpdeskService;
    @Resource
    private ToolAuditService toolAuditService;

    @Tool(description = """
            Escalate to human agent. Creates a helpdesk ticket with HUMAN_ASSIGNED status. \
            Use when customer requests a human, is angry, or tools cannot solve the issue.
            """)
    public CommerceDtos.EscalateResult escalateToHuman(
            @ToolParam(description = "Short subject") String subject,
            @ToolParam(description = "Conversation summary for human agent") String summary,
            @ToolParam(description = "Customer email or phone", required = false) String customerContact,
            @ToolParam(description = "Related order number", required = false) String orderNo) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("subject", subject);
        args.put("orderNo", orderNo);
        return toolAuditService.record("escalateToHuman", args, () -> {
            CommerceDtos.EscalateResult r = helpdeskService.escalate(
                    subject, summary, customerContact, orderNo, ToolAuditService.currentConversationId());
            AgentSideChannel.addTicket(r);
            return r;
        });
    }
}
