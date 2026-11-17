package com.quanxiaoha.ai.robot.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quanxiaoha.ai.robot.agent.AgentTraceService;
import com.quanxiaoha.ai.robot.aspect.ApiOperationLog;
import com.quanxiaoha.ai.robot.commerce.AdminAuthService;
import com.quanxiaoha.ai.robot.commerce.CommerceApprovalService;
import com.quanxiaoha.ai.robot.commerce.CommerceService;
import com.quanxiaoha.ai.robot.config.RagProperties;
import com.quanxiaoha.ai.robot.domain.dos.AdminUserDO;
import com.quanxiaoha.ai.robot.domain.dos.CommerceActionRequestDO;
import com.quanxiaoha.ai.robot.domain.dos.ConversationStateDO;
import com.quanxiaoha.ai.robot.domain.dos.CustomerDO;
import com.quanxiaoha.ai.robot.domain.dos.OrderDO;
import com.quanxiaoha.ai.robot.domain.dos.OrderItemDO;
import com.quanxiaoha.ai.robot.domain.dos.ShipmentDO;
import com.quanxiaoha.ai.robot.domain.dos.TicketDO;
import com.quanxiaoha.ai.robot.domain.dos.ToolCallLogDO;
import com.quanxiaoha.ai.robot.domain.mapper.ToolCallLogMapper;
import com.quanxiaoha.ai.robot.helpdesk.ConversationStateService;
import com.quanxiaoha.ai.robot.helpdesk.HelpdeskService;
import com.quanxiaoha.ai.robot.service.HybridRetrievalService;
import com.quanxiaoha.ai.robot.utils.Response;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.ai.document.Document;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminCommerceController {

    @Resource
    private CommerceService commerceService;
    @Resource
    private CommerceApprovalService approvalService;
    @Resource
    private ToolCallLogMapper toolCallLogMapper;
    @Resource
    private HelpdeskService helpdeskService;
    @Resource
    private AdminAuthService adminAuthService;
    @Resource
    private HybridRetrievalService hybridRetrievalService;
    @Resource
    private AgentTraceService agentTraceService;
    @Resource
    private ConversationStateService conversationStateService;
    @Resource
    private RagProperties ragProperties;

    @PostMapping("/auth/login")
    @ApiOperationLog(description = "商家后台登录")
    public Response<AdminAuthService.LoginResult> login(@RequestBody LoginReq req) {
        return Response.success(adminAuthService.login(req.getUsername(), req.getPassword()));
    }

    @GetMapping("/dashboard/summary")
    public Response<Map<String, Object>> dashboardSummary() {
        List<TicketDO> openTickets = helpdeskService.list(null).stream()
                .filter(t -> t.getStatus() != null && !"RESOLVED".equalsIgnoreCase(t.getStatus())
                        && !"CLOSED".equalsIgnoreCase(t.getStatus()))
                .toList();
        long pendingActions = approvalService.list("PENDING").size();
        long openTicketCount = openTickets.size();
        long humanAssigned = openTickets.stream()
                .filter(t -> "HUMAN_ASSIGNED".equalsIgnoreCase(t.getStatus())
                        || "ASSIGNED".equalsIgnoreCase(t.getStatus()))
                .count();
        List<ToolCallLogDO> recentLogs = toolCallLogMapper.selectList(Wrappers.<ToolCallLogDO>lambdaQuery()
                .orderByDesc(ToolCallLogDO::getId)
                .last("limit 100"));
        long toolFail = recentLogs.stream()
                .filter(l -> Boolean.FALSE.equals(l.getSuccess()))
                .count();
        long recentRuns = agentTraceService.listRuns(50).size();
        long orderCount = commerceService.listOrders().size();
        long productCount = commerceService.listProducts().size();

        Map<String, Object> m = new HashMap<>();
        m.put("pendingActions", pendingActions);
        m.put("openTickets", openTicketCount);
        m.put("humanAssignedTickets", humanAssigned);
        m.put("toolFailuresRecent", toolFail);
        m.put("recentAgentRuns", recentRuns);
        m.put("orderCount", orderCount);
        m.put("productCount", productCount);
        List<Map<String, String>> shortcuts = new java.util.ArrayList<>();
        shortcuts.add(Map.of("label", "统一收件箱", "path", "/admin/inbox"));
        shortcuts.add(Map.of("label", "动作审批", "path", "/admin/actions"));
        shortcuts.add(Map.of("label", "会话记录", "path", "/admin/conversations"));
        shortcuts.add(Map.of("label", "轨迹回放", "path", "/admin/traces"));
        shortcuts.add(Map.of("label", "评测", "path", "/admin/evals"));
        shortcuts.add(Map.of("label", "买家 Widget", "path", "/widget"));
        m.put("shortcuts", shortcuts);
        return Response.success(m);
    }

    @GetMapping("/orders")
    public Response<List<Map<String, Object>>> orders() {
        List<OrderDO> orders = commerceService.listOrders();
        List<Map<String, Object>> rows = orders.stream().map(o -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", o.getId());
            m.put("orderNo", o.getOrderNo());
            m.put("status", o.getStatus());
            m.put("totalAmount", o.getTotalAmount());
            m.put("currency", o.getCurrency());
            m.put("shippingAddress", o.getShippingAddress());
            CustomerDO c = commerceService.getCustomer(o.getCustomerId());
            m.put("customerEmail", c == null ? null : c.getEmail());
            m.put("customerName", c == null ? null : c.getName());
            return m;
        }).toList();
        return Response.success(rows);
    }

    @GetMapping("/orders/{orderNo}")
    public Response<Map<String, Object>> orderDetail(@PathVariable String orderNo) {
        OrderDO order = commerceService.getOrderByNo(orderNo);
        if (order == null) {
            return Response.fail("订单不存在");
        }
        List<OrderItemDO> items = commerceService.listOrderItems(order.getId());
        ShipmentDO shipment = commerceService.getShipmentByOrderId(order.getId());
        CustomerDO customer = commerceService.getCustomer(order.getCustomerId());
        Map<String, Object> m = new HashMap<>();
        m.put("order", order);
        m.put("items", items);
        m.put("shipment", shipment);
        m.put("customer", customer);
        return Response.success(m);
    }

    @GetMapping("/actions")
    public Response<List<CommerceActionRequestDO>> actions(@RequestParam(required = false) String status) {
        return Response.success(approvalService.list(status));
    }

    @PostMapping("/actions/{id}/review")
    @ApiOperationLog(description = "审批动作")
    public Response<CommerceActionRequestDO> review(@PathVariable Long id,
                                                    @RequestHeader(value = "Authorization", required = false) String token,
                                                    @RequestBody ReviewReq req) {
        AdminUserDO user = adminAuthService.requireSupervisor(token);
        return Response.success(approvalService.review(id, Boolean.TRUE.equals(req.getApprove()),
                user.getUsername(), req.getNote()));
    }

    @GetMapping("/tool-logs")
    public Response<List<ToolCallLogDO>> toolLogs(@RequestParam(defaultValue = "50") int limit) {
        int size = Math.min(Math.max(limit, 1), 200);
        return Response.success(toolCallLogMapper.selectList(Wrappers.<ToolCallLogDO>lambdaQuery()
                .orderByDesc(ToolCallLogDO::getId)
                .last("limit " + size)));
    }

    @GetMapping("/tickets")
    public Response<?> tickets(@RequestParam(required = false) String status) {
        List<TicketDO> list = helpdeskService.list(status);
        List<Map<String, Object>> rows = list.stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("ticketNo", t.getTicketNo());
            m.put("chatId", t.getChatId());
            m.put("customerContact", t.getCustomerContact());
            m.put("orderNo", t.getOrderNo());
            m.put("subject", t.getSubject());
            m.put("summary", t.getSummary());
            m.put("status", t.getStatus());
            m.put("assignee", t.getAssignee());
            m.put("internalNote", t.getInternalNote());
            m.put("createTime", t.getCreateTime());
            if (StringUtils.hasText(t.getChatId())) {
                ConversationStateDO st = conversationStateService.get(t.getChatId());
                m.put("conversationStatus", st == null ? null : st.getStatus());
            }
            return m;
        }).toList();
        return Response.success(rows);
    }

    @GetMapping("/tickets/{id}")
    public Response<?> ticketDetail(@PathVariable Long id) {
        TicketDO ticket = helpdeskService.getById(id);
        if (ticket == null) {
            return Response.fail("工单不存在");
        }
        Map<String, Object> m = new HashMap<>();
        m.put("ticket", ticket);
        m.put("messages", helpdeskService.listMessages(ticket.getChatId()));
        ConversationStateDO st = conversationStateService.get(ticket.getChatId());
        m.put("conversationStatus", st == null ? null : st.getStatus());
        return Response.success(m);
    }

    @GetMapping("/inbox/{chatId}/messages")
    public Response<?> inboxMessages(@PathVariable String chatId) {
        return Response.success(helpdeskService.listMessages(chatId));
    }

    @PostMapping("/tickets/{id}/reply")
    @ApiOperationLog(description = "Inbox 人工回复买家")
    public Response<?> reply(@PathVariable Long id,
                             @RequestHeader(value = "Authorization", required = false) String token,
                             @RequestBody ReplyReq req) {
        AdminUserDO user = adminAuthService.requireUser(token);
        return Response.success(helpdeskService.replyToChat(id, req.getContent(), user.getDisplayName()));
    }

    @PostMapping("/tickets/{id}/takeover")
    public Response<?> takeover(@PathVariable Long id,
                                @RequestHeader(value = "Authorization", required = false) String token,
                                @RequestBody TakeoverReq req) {
        AdminUserDO user = adminAuthService.requireUser(token);
        return Response.success(helpdeskService.takeover(id,
                req.getAssignee() != null ? req.getAssignee() : user.getUsername(),
                req.getNote()));
    }

    @PostMapping("/tickets/{id}/resolve")
    public Response<?> resolve(@PathVariable Long id,
                               @RequestHeader(value = "Authorization", required = false) String token,
                               @RequestBody TakeoverReq req) {
        adminAuthService.requireUser(token);
        return Response.success(helpdeskService.resolve(id, req.getNote()));
    }

    @GetMapping("/traces")
    public Response<?> traces(@RequestParam(defaultValue = "50") int limit) {
        return Response.success(agentTraceService.listRuns(limit));
    }

    @GetMapping("/traces/{runId}")
    public Response<?> traceDetail(@PathVariable String runId) {
        Map<String, Object> m = new HashMap<>();
        m.put("steps", agentTraceService.listSteps(runId));
        return Response.success(m);
    }

    @PostMapping("/rag/debug")
    @ApiOperationLog(description = "RAG Debug / Workbench 检索")
    public Response<Map<String, Object>> ragDebug(@RequestBody RagDebugReq req) {
        boolean prevHybrid = ragProperties.getHybrid().isEnabled();
        boolean prevRerank = ragProperties.getRerank().isEnabled();
        String mode = req.getMode() == null ? "current" : req.getMode();
        try {
            switch (mode) {
                case "vector" -> {
                    ragProperties.getHybrid().setEnabled(false);
                    ragProperties.getRerank().setEnabled(false);
                }
                case "hybrid" -> {
                    ragProperties.getHybrid().setEnabled(true);
                    ragProperties.getRerank().setEnabled(false);
                }
                case "rerank" -> {
                    ragProperties.getHybrid().setEnabled(true);
                    ragProperties.getRerank().setEnabled(true);
                }
                default -> {
                    // keep current
                }
            }
            List<Document> docs = hybridRetrievalService.retrieve(req.getQuestion());
            List<Map<String, Object>> rows = docs.stream().map(d -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", d.getId());
                m.put("text", d.getText());
                m.put("rerankScore", d.getMetadata().get("rerankScore"));
                m.put("rrfScore", d.getMetadata().get("rrfScore"));
                return m;
            }).toList();
            boolean enough = !rows.isEmpty() && rows.stream().anyMatch(r -> {
                Object score = r.get("rerankScore");
                if (score instanceof Number n) {
                    return n.doubleValue() >= 0.2;
                }
                return true;
            });
            Map<String, Object> result = new HashMap<>();
            result.put("mode", mode);
            result.put("enoughToAnswer", enough);
            result.put("hits", rows.size());
            result.put("documents", rows);
            return Response.success(result);
        } finally {
            ragProperties.getHybrid().setEnabled(prevHybrid);
            ragProperties.getRerank().setEnabled(prevRerank);
        }
    }

    @GetMapping("/products")
    public Response<?> products() {
        return Response.success(commerceService.listProducts());
    }

    @GetMapping("/customers")
    public Response<?> customers() {
        List<CustomerDO> customers = commerceService.listCustomers();
        List<OrderDO> allOrders = commerceService.listOrders();
        List<Map<String, Object>> rows = customers.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("email", c.getEmail());
            m.put("phone", c.getPhone());
            m.put("createTime", c.getCreateTime() == null ? null : c.getCreateTime().toString());
            List<OrderDO> orders = allOrders.stream()
                    .filter(o -> c.getId() != null && c.getId().equals(o.getCustomerId()))
                    .toList();
            m.put("orderCount", orders.size());
            // HashMap 允许 null；Map.of 不允许，避免 latestOrder 为空时直接 500
            m.put("latestOrderNo", orders.isEmpty() ? null : orders.get(0).getOrderNo());
            return m;
        }).toList();
        return Response.success(rows);
    }

    @Data
    public static class LoginReq {
        private String username;
        private String password;
    }

    @Data
    public static class ReviewReq {
        private Boolean approve;
        private String note;
    }

    @Data
    public static class TakeoverReq {
        private String assignee;
        private String note;
    }

    @Data
    public static class ReplyReq {
        private String content;
    }

    @Data
    public static class RagDebugReq {
        private String question;
        /** current | vector | hybrid | rerank */
        private String mode;
    }
}
