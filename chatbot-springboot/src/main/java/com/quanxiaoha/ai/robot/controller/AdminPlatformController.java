package com.quanxiaoha.ai.robot.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quanxiaoha.ai.robot.agent.AgentOrchestratorService;
import com.quanxiaoha.ai.robot.agent.AgentTraceService;
import com.quanxiaoha.ai.robot.commerce.CommerceApprovalService;
import com.quanxiaoha.ai.robot.domain.dos.AdminUserDO;
import com.quanxiaoha.ai.robot.domain.dos.AgentRunDO;
import com.quanxiaoha.ai.robot.domain.dos.ChatDO;
import com.quanxiaoha.ai.robot.domain.dos.ChatMessageDO;
import com.quanxiaoha.ai.robot.domain.dos.CommerceActionRequestDO;
import com.quanxiaoha.ai.robot.domain.dos.ConversationStateDO;
import com.quanxiaoha.ai.robot.domain.dos.CsMessageDO;
import com.quanxiaoha.ai.robot.domain.dos.LangEventDO;
import com.quanxiaoha.ai.robot.domain.dos.TicketDO;
import com.quanxiaoha.ai.robot.domain.dos.ToolCallLogDO;
import com.quanxiaoha.ai.robot.domain.mapper.AdminUserMapper;
import com.quanxiaoha.ai.robot.domain.mapper.ChatMapper;
import com.quanxiaoha.ai.robot.domain.mapper.ChatMessageMapper;
import com.quanxiaoha.ai.robot.domain.mapper.ConversationStateMapper;
import com.quanxiaoha.ai.robot.domain.mapper.CsMessageMapper;
import com.quanxiaoha.ai.robot.domain.mapper.ToolCallLogMapper;
import com.quanxiaoha.ai.robot.eval.ContractEvalRunner;
import com.quanxiaoha.ai.robot.helpdesk.HelpdeskService;
import com.quanxiaoha.ai.robot.i18n.LanguageEvidenceService;
import com.quanxiaoha.ai.robot.utils.Response;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class AdminPlatformController {

    @Resource
    private ConversationStateMapper conversationStateMapper;
    @Resource
    private CsMessageMapper csMessageMapper;
    @Resource
    private ChatMapper chatMapper;
    @Resource
    private ChatMessageMapper chatMessageMapper;
    @Resource
    private HelpdeskService helpdeskService;
    @Resource
    private AgentTraceService agentTraceService;
    @Resource
    private CommerceApprovalService approvalService;
    @Resource
    private ToolCallLogMapper toolCallLogMapper;
    @Resource
    private AdminUserMapper adminUserMapper;
    @Resource
    private AgentOrchestratorService orchestratorService;
    @Resource
    private LanguageEvidenceService languageEvidenceService;

    @GetMapping("/admin/conversations")
    public Response<List<Map<String, Object>>> conversations(@RequestParam(defaultValue = "80") int limit) {
        int size = Math.min(Math.max(limit, 1), 200);
        Map<String, Map<String, Object>> rows = new LinkedHashMap<>();

        List<ConversationStateDO> states = conversationStateMapper.selectList(Wrappers.<ConversationStateDO>lambdaQuery()
                .orderByDesc(ConversationStateDO::getUpdateTime)
                .last("limit " + size));
        for (ConversationStateDO state : states) {
            Map<String, Object> row = touch(rows, state.getChatId(), "widget");
            row.put("status", state.getStatus());
            row.put("updateTime", state.getUpdateTime());
        }

        for (AgentRunDO run : agentTraceService.listRuns(size)) {
            Map<String, Object> row = touch(rows, run.getConversationId(), "agent");
            if (!StringUtils.hasText((String) row.get("summary"))) {
                row.put("summary", truncate(run.getUserMessage(), 80));
            }
            if (row.get("updateTime") == null) {
                row.put("updateTime", run.getUpdateTime());
            }
            row.put("sourceLang", run.getSourceLang());
            row.put("specialist", run.getSpecialist());
        }

        for (TicketDO ticket : helpdeskService.list(null)) {
            if (!StringUtils.hasText(ticket.getChatId())) {
                continue;
            }
            Map<String, Object> row = touch(rows, ticket.getChatId(), "inbox");
            row.put("ticketNo", ticket.getTicketNo());
            if (!StringUtils.hasText((String) row.get("summary"))) {
                row.put("summary", truncate(ticket.getSubject(), 80));
            }
            if (row.get("status") == null) {
                row.put("status", ticket.getStatus());
            }
        }

        List<ChatDO> chats = chatMapper.selectList(Wrappers.<ChatDO>lambdaQuery()
                .orderByDesc(ChatDO::getUpdateTime)
                .last("limit " + size));
        for (ChatDO chat : chats) {
            Map<String, Object> row = touch(rows, chat.getUuid(), "test");
            if (!StringUtils.hasText((String) row.get("summary"))) {
                row.put("summary", truncate(chat.getSummary(), 80));
            }
            if (row.get("updateTime") == null) {
                row.put("updateTime", chat.getUpdateTime());
            }
        }

        List<Map<String, Object>> list = new ArrayList<>(rows.values());
        for (Map<String, Object> row : list) {
            String chatId = (String) row.get("chatId");
            CsMessageDO last = csMessageMapper.selectOne(Wrappers.<CsMessageDO>lambdaQuery()
                    .eq(CsMessageDO::getChatId, chatId)
                    .orderByDesc(CsMessageDO::getId)
                    .last("limit 1"));
            if (last != null) {
                row.put("lastRole", last.getRole());
                row.put("lastMessage", truncate(last.getContent(), 80));
                if (row.get("updateTime") == null) {
                    row.put("updateTime", last.getCreateTime());
                }
            }
            Long count = csMessageMapper.selectCount(Wrappers.<CsMessageDO>lambdaQuery()
                    .eq(CsMessageDO::getChatId, chatId));
            row.put("messageCount", count == null ? 0 : count);
        }
        list.sort(Comparator.comparing(
                (Map<String, Object> m) -> (LocalDateTime) m.get("updateTime"),
                Comparator.nullsLast(Comparator.reverseOrder())));
        if (list.size() > size) {
            return Response.success(list.subList(0, size));
        }
        return Response.success(list);
    }

    @GetMapping("/admin/conversations/{chatId}")
    public Response<Map<String, Object>> conversation(@PathVariable String chatId) {
        Map<String, Object> body = new HashMap<>();
        ConversationStateDO state = conversationStateMapper.selectOne(Wrappers.<ConversationStateDO>lambdaQuery()
                .eq(ConversationStateDO::getChatId, chatId).last("limit 1"));
        body.put("status", state == null ? null : state.getStatus());
        body.put("messages", helpdeskService.listMessages(chatId));
        List<ChatMessageDO> testMessages = chatMessageMapper.selectList(Wrappers.<ChatMessageDO>lambdaQuery()
                .eq(ChatMessageDO::getChatUuid, chatId)
                .orderByAsc(ChatMessageDO::getId)
                .last("limit 100"));
        body.put("testMessages", testMessages);
        return Response.success(body);
    }

    @GetMapping("/admin/audit")
    public Response<List<Map<String, Object>>> audit(@RequestParam(defaultValue = "80") int limit) {
        int size = Math.min(Math.max(limit, 1), 200);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (CommerceActionRequestDO action : approvalService.list(null)) {
            if (!StringUtils.hasText(action.getReviewer()) && "PENDING".equals(action.getStatus())) {
                rows.add(auditRow(action.getCreateTime(), "APPROVAL", "agent",
                        action.getActionType() + " " + action.getOrderNo() + " 待审批", true));
                continue;
            }
            rows.add(auditRow(action.getUpdateTime() == null ? action.getCreateTime() : action.getUpdateTime(),
                    "APPROVAL",
                    action.getReviewer() == null ? "agent" : action.getReviewer(),
                    action.getActionType() + " " + action.getOrderNo() + " → " + action.getStatus(),
                    !"REJECTED".equals(action.getStatus())));
        }
        for (ToolCallLogDO log : toolCallLogMapper.selectList(Wrappers.<ToolCallLogDO>lambdaQuery()
                .orderByDesc(ToolCallLogDO::getId).last("limit " + size))) {
            rows.add(auditRow(log.getCreateTime(), "TOOL", log.getToolName(),
                    truncate(log.getResultSummary(), 120), log.getSuccess()));
        }
        for (TicketDO ticket : helpdeskService.list(null)) {
            if (StringUtils.hasText(ticket.getAssignee()) || "RESOLVED".equalsIgnoreCase(ticket.getStatus())) {
                rows.add(auditRow(ticket.getUpdateTime(), "INBOX",
                        ticket.getAssignee() == null ? "system" : ticket.getAssignee(),
                        ticket.getTicketNo() + " " + ticket.getStatus(),
                        true));
            }
        }
        rows.sort(Comparator.comparing(
                (Map<String, Object> m) -> (LocalDateTime) m.get("time"),
                Comparator.nullsLast(Comparator.reverseOrder())));
        if (rows.size() > size) {
            return Response.success(rows.subList(0, size));
        }
        return Response.success(rows);
    }

    @GetMapping("/admin/users")
    public Response<Map<String, Object>> users() {
        List<AdminUserDO> users = adminUserMapper.selectList(Wrappers.<AdminUserDO>lambdaQuery()
                .orderByAsc(AdminUserDO::getId));
        List<Map<String, Object>> rows = users.stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("displayName", u.getDisplayName());
            m.put("role", u.getRole());
            m.put("createTime", u.getCreateTime());
            m.put("demoPassword", demoPassword(u.getUsername()));
            return m;
        }).toList();
        List<Map<String, String>> roles = List.of(
                Map.of("role", "supervisor", "can", "审批退换货、改地址，可回复收件箱"),
                Map.of("role", "agent", "can", "接管和回复收件箱，不能审批")
        );
        Map<String, Object> body = new HashMap<>();
        body.put("users", rows);
        body.put("roles", roles);
        return Response.success(body);
    }

    @GetMapping("/admin/evals/summary")
    public Response<Map<String, Object>> evals() {
        ContractEvalRunner.Report report = ContractEvalRunner.run();
        Map<String, Object> body = new HashMap<>();
        body.put("routePass", report.routePass());
        body.put("routeTotal", report.routeTotal());
        body.put("routeRate", report.routeRate());
        body.put("approvalOk", report.approvalOk());
        body.put("approvalExpect", report.approvalExpect());
        body.put("safetyPass", report.safetyPass());
        body.put("safetyTotal", report.safetyTotal());
        body.put("langPass", report.langPass());
        body.put("langTotal", report.langTotal());
        body.put("writePass", report.writePass());
        body.put("writeTotal", report.writeTotal());
        body.put("faqPass", report.faqPass());
        body.put("faqTotal", report.faqTotal());
        body.put("translatePass", report.translatePass());
        body.put("translateTotal", report.translateTotal());
        body.put("headline", report.lines().isEmpty() ? "" : report.lines().get(0));
        body.put("note", "本页跑的是进程内契约，不调用外部模型。写工具断言的是 PENDING 门禁，不是真实退款。");
        return Response.success(body);
    }

    @GetMapping("/admin/agent-workflow")
    public Response<List<Map<String, Object>>> workflow() {
        return Response.success(orchestratorService.catalog());
    }

    @GetMapping("/admin/multilingual")
    public Response<Map<String, Object>> multilingual(@RequestParam(defaultValue = "50") int limit) {
        List<LangEventDO> events = languageEvidenceService.list(limit);
        List<Map<String, Object>> runs = agentTraceService.listRuns(limit).stream().map(run -> {
            Map<String, Object> m = new HashMap<>();
            m.put("runId", run.getRunId());
            m.put("conversationId", run.getConversationId());
            m.put("sourceLang", run.getSourceLang());
            m.put("specialist", run.getSpecialist());
            m.put("userMessage", truncate(run.getUserMessage(), 80));
            m.put("createTime", run.getCreateTime());
            return m;
        }).toList();
        Map<String, Object> body = new HashMap<>();
        body.put("events", events);
        body.put("runs", runs);
        return Response.success(body);
    }

    private static Map<String, Object> touch(Map<String, Map<String, Object>> rows, String chatId, String source) {
        if (!StringUtils.hasText(chatId)) {
            return new HashMap<>();
        }
        return rows.computeIfAbsent(chatId, id -> {
            Map<String, Object> row = new HashMap<>();
            row.put("chatId", id);
            row.put("source", source);
            row.put("status", null);
            row.put("summary", "");
            return row;
        });
    }

    private static Map<String, Object> auditRow(LocalDateTime time, String type, String actor, String summary, Boolean success) {
        Map<String, Object> m = new HashMap<>();
        m.put("time", time);
        m.put("type", type);
        m.put("actor", actor);
        m.put("summary", summary);
        m.put("success", success);
        return m;
    }

    private static String demoPassword(String username) {
        return switch (username) {
            case "admin" -> "123456";
            case "agent" -> "agent123";
            case "supervisor" -> "super123";
            default -> null;
        };
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        String oneLine = text.replace('\n', ' ').trim();
        return oneLine.length() > max ? oneLine.substring(0, max) + "…" : oneLine;
    }
}
