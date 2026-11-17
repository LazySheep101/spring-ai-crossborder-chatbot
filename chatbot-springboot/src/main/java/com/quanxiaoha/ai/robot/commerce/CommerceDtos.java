package com.quanxiaoha.ai.robot.commerce;

import java.util.List;

public final class CommerceDtos {
    private CommerceDtos() {
    }

    public record OrderQueryResult(
            String orderNo,
            String status,
            boolean verified,
            List<String> items,
            String totalAmount,
            String currency,
            String trackingNumber,
            String trackingCarrier,
            String trackingStatus,
            String shippingAddress,
            String message
    ) {
    }

    public record LogisticsResult(
            String orderNo,
            String trackingNumber,
            String carrier,
            String status,
            String latestEvent,
            String message
    ) {
    }

    public record ProductResult(
            String sku,
            String name,
            String description,
            String price,
            int stock,
            String category
    ) {
    }

    public record ActionResult(
            boolean accepted,
            String actionType,
            String orderNo,
            Long requestId,
            String status,
            String message
    ) {
        public static ActionResult rejected(String actionType, String orderNo, String message) {
            return new ActionResult(false, actionType, orderNo, null, "REJECTED", message);
        }

        public static ActionResult pending(String actionType, String orderNo, Long requestId, String message) {
            return new ActionResult(true, actionType, orderNo, requestId, "PENDING", message);
        }
    }

    public record PolicyChunk(String id, String text, Double rerankScore, Double rrfScore) {
    }

    public record PolicyAnswer(boolean ok, String answerHint, List<PolicyChunk> citations, String message) {
        public static PolicyAnswer error(String message) {
            return new PolicyAnswer(false, "", List.of(), message);
        }
    }

    public record EscalateResult(boolean ok, String ticketNo, String status, String message) {
    }
}
