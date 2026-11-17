package com.quanxiaoha.ai.robot.agent;

import com.quanxiaoha.ai.robot.domain.dos.ToolCallLogDO;
import com.quanxiaoha.ai.robot.domain.mapper.ToolCallLogMapper;
import com.quanxiaoha.ai.robot.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Supplier;

@Service
@Slf4j
public class ToolAuditService {

    private static final ThreadLocal<String> CONVERSATION_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> RUN_ID = new ThreadLocal<>();

    @Resource
    private ToolCallLogMapper toolCallLogMapper;
    @Resource
    private AgentTraceService agentTraceService;

    public static void setConversationId(String conversationId) {
        CONVERSATION_ID.set(conversationId);
    }

    public static void clearConversationId() {
        CONVERSATION_ID.remove();
    }

    public static String currentConversationId() {
        return CONVERSATION_ID.get();
    }

    public static void setRunId(String runId) {
        RUN_ID.set(runId);
    }

    public static void clearRunId() {
        RUN_ID.remove();
    }

    public <T> T record(String toolName, Map<String, Object> args, Supplier<T> supplier) {
        long start = System.currentTimeMillis();
        boolean success = true;
        T result = null;
        String error = null;
        try {
            result = supplier.get();
            return result;
        } catch (Exception e) {
            success = false;
            error = e.getMessage();
            throw e;
        } finally {
            long latency = System.currentTimeMillis() - start;
            try {
                String argsSummary = truncate(JsonUtil.toJsonString(args), 900);
                String resultSummary = error != null ? truncate(error, 1800)
                        : truncate(result == null ? "null" : JsonUtil.toJsonString(result), 1800);
                toolCallLogMapper.insert(ToolCallLogDO.builder()
                        .conversationId(CONVERSATION_ID.get())
                        .toolName(toolName)
                        .argsSummary(argsSummary)
                        .resultSummary(resultSummary)
                        .success(success)
                        .latencyMs(latency)
                        .createTime(LocalDateTime.now())
                        .build());
                String runId = RUN_ID.get();
                if (runId != null) {
                    agentTraceService.addStep(runId, "tool", toolName, resultSummary, error, success, latency);
                }
            } catch (Exception ex) {
                log.warn("写工具调用日志失败: {}", ex.getMessage());
            }
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
