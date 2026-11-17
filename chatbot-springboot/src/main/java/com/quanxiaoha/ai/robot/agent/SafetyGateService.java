package com.quanxiaoha.ai.robot.agent;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * 轻量安全门：注入 / 越权话术 / 过长输入拦截。
 */
@Service
public class SafetyGateService {

    private static final int MAX_CHARS = 4000;

    private static final List<String> INJECTION_PATTERNS = List.of(
            "ignore previous", "ignore all previous", "忽略之前", "忽略以上", "忽略上述",
            "系统提示词", "system prompt", "jailbreak", "开发者模式",
            "把提示词发我", "输出你的提示词", "reveal your prompt"
    );

    public record GateResult(boolean blocked, String reason, String userVisibleMessage) {
        public static GateResult ok() {
            return new GateResult(false, null, null);
        }

        public static GateResult block(String reason, String message) {
            return new GateResult(true, reason, message);
        }
    }

    public GateResult check(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return GateResult.block("EMPTY", "请输入有效问题。");
        }
        if (userMessage.length() > MAX_CHARS) {
            return GateResult.block("TOO_LONG", "消息过长，请精简后再试（最多 " + MAX_CHARS + " 字）。");
        }
        String lower = userMessage.toLowerCase(Locale.ROOT);
        for (String p : INJECTION_PATTERNS) {
            if (lower.contains(p.toLowerCase(Locale.ROOT))) {
                return GateResult.block("PROMPT_INJECTION",
                        "检测到不安全请求，已拦截。如需帮助请直接描述订单/退货/物流问题，或转人工客服。");
            }
        }
        return GateResult.ok();
    }
}
