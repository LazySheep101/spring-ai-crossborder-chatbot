package com.quanxiaoha.ai.robot.agent.config;

import com.quanxiaoha.ai.robot.agent.tool.EscalationTools;
import com.quanxiaoha.ai.robot.agent.tool.LogisticsTools;
import com.quanxiaoha.ai.robot.agent.tool.OrderTools;
import com.quanxiaoha.ai.robot.agent.tool.PolicyTools;
import com.quanxiaoha.ai.robot.agent.tool.ProductTools;
import com.quanxiaoha.ai.robot.agent.tool.TranslationTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolCallbackConfig {

    @Bean
    public ToolCallbackProvider commerceToolCallbackProvider(
            PolicyTools policyTools,
            OrderTools orderTools,
            LogisticsTools logisticsTools,
            ProductTools productTools,
            EscalationTools escalationTools,
            TranslationTools translationTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(policyTools, orderTools, logisticsTools, productTools, escalationTools, translationTools)
                .build();
    }
}
