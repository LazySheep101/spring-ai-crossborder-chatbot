package com.quanxiaoha.ai.robot.commerce;

/**
 * 写工具门禁：通过校验后只能是 PENDING，不能从工具路径直接变成已退款或已改单。
 */
public final class ActionGate {

    private ActionGate() {
    }

    public record Decision(boolean accepted, String status, String message) {
    }

    public static Decision decide(boolean orderExists, boolean identityVerified, boolean contactPresent) {
        if (!orderExists) {
            return new Decision(false, "REJECTED", "订单不存在。");
        }
        if (!contactPresent) {
            return new Decision(false, "REJECTED", "高风险操作必须提供邮箱或手机号。");
        }
        if (!identityVerified) {
            return new Decision(false, "REJECTED", "身份校验失败，无法提交审批请求。");
        }
        return new Decision(true, "PENDING", "已创建待人工审批请求，AI 不会直接改单或退款。");
    }
}
