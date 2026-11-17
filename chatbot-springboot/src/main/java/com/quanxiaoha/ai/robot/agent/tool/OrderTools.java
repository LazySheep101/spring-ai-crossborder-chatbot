package com.quanxiaoha.ai.robot.agent.tool;

import com.quanxiaoha.ai.robot.agent.AgentSideChannel;
import com.quanxiaoha.ai.robot.agent.ToolAuditService;
import com.quanxiaoha.ai.robot.commerce.CommerceDtos;
import com.quanxiaoha.ai.robot.commerce.CommerceService;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class OrderTools {

    @Resource
    private CommerceService commerceService;
    @Resource
    private ToolAuditService toolAuditService;

    @Tool(description = """
            Query order details by order number. Requires customer email or phone for identity verification. \
            Use when the customer asks about order status, contents, amount, or shipping address.
            """)
    public CommerceDtos.OrderQueryResult queryOrder(
            @ToolParam(description = "Order number, e.g. ORD-10001 or #ORD-10001") String orderId,
            @ToolParam(description = "Customer email or phone for ownership verification") String customerEmail) {
        return toolAuditService.record("queryOrder",
                params("orderId", orderId, "customerEmail", customerEmail),
                () -> commerceService.queryOrder(orderId, customerEmail));
    }

    @Tool(description = """
            Create an internal return request. Does NOT refund money or change the external order. \
            Creates a PENDING human-approval request.
            """)
    public CommerceDtos.ActionResult createReturnRequest(
            @ToolParam(description = "Order number") String orderId,
            @ToolParam(description = "Customer email or phone") String customerEmail,
            @ToolParam(description = "Reason for return") String reason,
            @ToolParam(description = "Items description", required = false) String items) {
        return toolAuditService.record("createReturnRequest",
                params("orderId", orderId, "customerEmail", customerEmail),
                () -> {
                    CommerceDtos.ActionResult r = commerceService.createAction(
                            "RETURN", orderId, customerEmail, reason, items);
                    AgentSideChannel.addAction(r);
                    return r;
                });
    }

    @Tool(description = """
            Request refund or replacement. AI never executes refunds directly; creates PENDING approval only.
            """)
    public CommerceDtos.ActionResult requestRefundOrReplacement(
            @ToolParam(description = "Order number") String orderId,
            @ToolParam(description = "Customer email or phone") String customerEmail,
            @ToolParam(description = "Action: refund or replacement") String action,
            @ToolParam(description = "Reason") String reason) {
        String type = action != null && action.toLowerCase().contains("replace") ? "REPLACEMENT" : "REFUND";
        return toolAuditService.record("requestRefundOrReplacement",
                params("orderId", orderId, "action", action),
                () -> {
                    CommerceDtos.ActionResult r = commerceService.createAction(
                            type, orderId, customerEmail, reason, action);
                    AgentSideChannel.addAction(r);
                    return r;
                });
    }

    @Tool(description = """
            Request shipping address change. Creates PENDING approval; does not rewrite the order directly.
            """)
    public CommerceDtos.ActionResult requestAddressChange(
            @ToolParam(description = "Order number") String orderId,
            @ToolParam(description = "Customer email or phone") String customerEmail,
            @ToolParam(description = "New shipping address") String newAddress) {
        return toolAuditService.record("requestAddressChange",
                params("orderId", orderId, "customerEmail", customerEmail),
                () -> {
                    CommerceDtos.ActionResult r = commerceService.createAction(
                            "ADDRESS_CHANGE", orderId, customerEmail, "地址变更", newAddress);
                    AgentSideChannel.addAction(r);
                    return r;
                });
    }

    private Map<String, Object> params(Object... entries) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < entries.length; i += 2) {
            map.put(String.valueOf(entries[i]), entries[i + 1]);
        }
        return map;
    }
}
