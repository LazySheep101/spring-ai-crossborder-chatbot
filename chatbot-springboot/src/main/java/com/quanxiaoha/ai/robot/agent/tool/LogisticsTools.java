package com.quanxiaoha.ai.robot.agent.tool;

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
public class LogisticsTools {

    @Resource
    private CommerceService commerceService;
    @Resource
    private ToolAuditService toolAuditService;

    @Tool(description = """
            Track logistics for an order. Requires customer email or phone. \
            Use when the customer asks where the package is or delivery ETA.
            """)
    public CommerceDtos.LogisticsResult trackLogistics(
            @ToolParam(description = "Order number") String orderId,
            @ToolParam(description = "Customer email or phone") String customerEmail) {
        return toolAuditService.record("trackLogistics",
                Map.of("orderId", orderId, "customerEmail", customerEmail == null ? "" : customerEmail),
                () -> commerceService.trackLogistics(orderId, customerEmail));
    }
}
