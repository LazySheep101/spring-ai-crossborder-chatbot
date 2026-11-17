package com.quanxiaoha.ai.robot.eval;

import java.util.List;

public record AgentContractCase(
        String id,
        String intent,
        String userMessage,
        List<String> expectedToolsSubset,
        boolean expectApprovalTools,
        boolean expectHandoff
) {
}
