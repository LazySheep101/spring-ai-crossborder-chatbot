package com.quanxiaoha.ai.robot.agent;

import com.quanxiaoha.ai.robot.commerce.CommerceDtos;
import com.quanxiaoha.ai.robot.config.RagProperties;
import com.quanxiaoha.ai.robot.helpdesk.ConversationStateService;
import com.quanxiaoha.ai.robot.helpdesk.HelpdeskService;
import com.quanxiaoha.ai.robot.i18n.LanguageDetector;
import com.quanxiaoha.ai.robot.i18n.LanguageEvidenceService;
import com.quanxiaoha.ai.robot.model.vo.chat.AIResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class AgentChatService {

    @Resource
    private RagProperties ragProperties;
    @Resource
    private AgentOrchestratorService orchestratorService;
    @Resource
    private ToolCallbackProvider commerceToolCallbackProvider;
    @Resource
    private SafetyGateService safetyGateService;
    @Resource
    private AgentMemoryStore agentMemoryStore;
    @Resource
    private LanguageDetector languageDetector;
    @Resource
    private LanguageEvidenceService languageEvidenceService;
    @Resource
    private AgentTraceService agentTraceService;
    @Resource
    private ConversationStateService conversationStateService;
    @Resource
    private HelpdeskService helpdeskService;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;
    @Value("${customer-service.model}")
    private String model;
    @Value("${customer-service.temperature}")
    private Double temperature;

    public Flux<AIResponse> streamChat(String userMessage, String conversationId) {
        String chatId = StringUtils.hasText(conversationId) ? conversationId : UUID.randomUUID().toString();
        String sourceLang = languageDetector.detect(userMessage);
        String replyLang = switch (sourceLang) {
            case "en", "zh", "mixed" -> sourceLang;
            default -> "zh";
        };
        languageEvidenceService.record(chatId, sourceLang, replyLang, "detect", false, "PROMPT_CONSTRAINT", userMessage);

        SafetyGateService.GateResult gate = safetyGateService.check(userMessage);
        if (gate.blocked()) {
            log.warn("## SafetyGate blocked chatId={}, reason={}", chatId, gate.reason());
            String runId = agentTraceService.startRun(chatId, userMessage, "blocked", sourceLang);
            agentTraceService.addStep(runId, "safety", gate.reason(), gate.userVisibleMessage(), null, false, 0L);
            agentTraceService.finish(runId, "BLOCKED");
            helpdeskService.appendMessage(chatId, "user", userMessage, null);
            helpdeskService.appendMessage(chatId, "assistant", gate.userVisibleMessage(),
                    "{\"blocked\":true,\"reason\":\"" + gate.reason() + "\"}");
            return Flux.just(AIResponse.builder()
                    .type("blocked")
                    .v(gate.userVisibleMessage())
                    .message(gate.reason())
                    .sourceLang(sourceLang)
                    .build());
        }

        AgentOrchestratorService.SpecialistPlan plan = orchestratorService.plan(userMessage);
        log.info("## Agent 分流: specialist={}, tools={}, risk={}, lang={}",
                plan.specialistLabel(), plan.toolAllowlist(), plan.riskLevel(), sourceLang);

        String runId = agentTraceService.startRun(chatId, userMessage, plan.specialistKey(), sourceLang);
        conversationStateService.ensureAiWorking(chatId);

        ToolCallback[] callbacks = filterCallbacks(plan.toolAllowlist());
        ChatModel chatModel = OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .build())
                .build();

        String langHint = switch (sourceLang) {
            case "en" -> "Reply in English.";
            case "zh" -> "请用中文回答。";
            case "mixed" -> "Match the customer's language (Chinese or English).";
            default -> "Reply in the customer's language.";
        };

        String system = ragProperties.getAgent().getSystemPrompt()
                + "\n当前 specialist：" + plan.specialistLabel()
                + "\n风险级别：" + plan.riskLevel()
                + "\n检测到的买家语种：" + sourceLang + "。" + langHint
                + "\n若政策原文语种和买家不一致，可调用 translateSnippet；翻译失败就保留原文。"
                + (plan.requiresIdentityVerification() ? "\n本轮需要身份校验（邮箱/手机）。" : "")
                + (plan.requiresApproval() ? "\n写操作只能创建审批单。" : "")
                + (plan.recommendHumanHandoff() ? "\n建议适时 escalateToHuman。" : "");

        List<Message> history = agentMemoryStore.history(chatId);
        List<AIResponse> prelude = new ArrayList<>();
        prelude.add(AIResponse.builder()
                .type("meta")
                .metaKind("triage")
                .specialist(plan.specialistKey())
                .sourceLang(sourceLang)
                .message(plan.specialistLabel())
                .build());

        ToolAuditService.setConversationId(chatId);
        ToolAuditService.setRunId(runId);
        AgentSideChannel.clear();
        helpdeskService.appendMessage(chatId, "user", userMessage, null);

        StringBuilder assistantBuf = new StringBuilder();

        try {
            List<Message> promptMessages = new ArrayList<>(history);
            promptMessages.add(new UserMessage(userMessage));

            ChatClient.ChatClientRequestSpec spec = ChatClient.create(chatModel)
                    .prompt()
                    .system(system)
                    .messages(promptMessages)
                    .options(OpenAiChatOptions.builder()
                            .model(model)
                            .temperature(temperature))
                    .toolCallbacks(callbacks);

            Flux<AIResponse> textFlux = spec.stream()
                    .content()
                    .map(text -> {
                        assistantBuf.append(text);
                        return AIResponse.builder().type("text").v(text).build();
                    });

            Flux<AIResponse> metaFlux = Flux.defer(() -> {
                List<AgentSideChannel.Event> events = AgentSideChannel.drain();
                List<AIResponse> metas = new ArrayList<>();
                Map<String, Object> metaBag = new HashMap<>();
                for (AgentSideChannel.Event ev : events) {
                    agentTraceService.addStep(runId, ev.kind(),
                            ev.kind(), summarize(ev), null, true, 0L);
                    if ("citation".equals(ev.kind()) && ev.payload() instanceof List<?> list) {
                        @SuppressWarnings("unchecked")
                        List<CommerceDtos.PolicyChunk> chunks = (List<CommerceDtos.PolicyChunk>) list;
                        List<Map<String, Object>> citations = chunks.stream().map(c -> {
                            Map<String, Object> m = new HashMap<>();
                            m.put("id", c.id());
                            m.put("text", c.text());
                            m.put("rerankScore", c.rerankScore());
                            m.put("rrfScore", c.rrfScore());
                            return m;
                        }).toList();
                        metas.add(AIResponse.builder()
                                .type("meta").metaKind("citation").citations(citations).build());
                        metaBag.put("citations", citations);
                        conversationStateService.markAiWorking(chatId);
                    } else if ("action".equals(ev.kind()) && ev.payload() instanceof AgentSideChannel.ActionPayload a) {
                        metas.add(AIResponse.builder()
                                .type("meta").metaKind("action")
                                .actionRequestId(a.requestId())
                                .actionType(a.actionType())
                                .actionStatus(a.status())
                                .message(a.message())
                                .build());
                        metaBag.put("actionRequestId", a.requestId());
                        metaBag.put("actionType", a.actionType());
                        metaBag.put("actionStatus", a.status());
                        conversationStateService.markNeedsApproval(chatId);
                    } else if ("ticket".equals(ev.kind()) && ev.payload() instanceof AgentSideChannel.TicketPayload t) {
                        metas.add(AIResponse.builder()
                                .type("meta").metaKind("ticket")
                                .ticketNo(t.ticketNo())
                                .actionStatus(t.status())
                                .message(t.message())
                                .build());
                        metaBag.put("ticketNo", t.ticketNo());
                        metaBag.put("ticketStatus", t.status());
                        conversationStateService.markHumanAssigned(chatId);
                    }
                }
                String answer = assistantBuf.toString();
                agentMemoryStore.append(chatId, userMessage, answer);
                String metaJson = metaBag.isEmpty() ? null : com.quanxiaoha.ai.robot.utils.JsonUtil.toJsonString(metaBag);
                helpdeskService.appendMessage(chatId, "assistant", answer, metaJson);
                agentTraceService.addStep(runId, "response", "assistant",
                        answer.length() > 200 ? answer.substring(0, 200) : answer, null, true, 0L);
                agentTraceService.finish(runId, "DONE");
                return Flux.fromIterable(metas);
            });

            return Flux.concat(Flux.fromIterable(prelude), textFlux, metaFlux)
                    .doFinally(signal -> {
                        ToolAuditService.clearConversationId();
                        ToolAuditService.clearRunId();
                        AgentSideChannel.clear();
                    });
        } catch (Exception e) {
            ToolAuditService.clearConversationId();
            ToolAuditService.clearRunId();
            AgentSideChannel.clear();
            agentTraceService.addStep(runId, "error", "exception", null, e.getMessage(), false, 0L);
            agentTraceService.finish(runId, "ERROR");
            throw e;
        }
    }

    private ToolCallback[] filterCallbacks(java.util.List<String> allowlist) {
        ToolCallback[] all = commerceToolCallbackProvider.getToolCallbacks();
        if (allowlist == null || allowlist.isEmpty()) {
            return all;
        }
        Set<String> allowed = new HashSet<>(allowlist);
        return Arrays.stream(all)
                .filter(cb -> allowed.contains(cb.getToolDefinition().name()))
                .toArray(ToolCallback[]::new);
    }

    private static String summarize(AgentSideChannel.Event ev) {
        if (ev.payload() == null) {
            return ev.kind();
        }
        String s = String.valueOf(ev.payload());
        return s.length() > 300 ? s.substring(0, 300) : s;
    }
}
