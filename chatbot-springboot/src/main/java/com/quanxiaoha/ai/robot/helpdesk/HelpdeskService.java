package com.quanxiaoha.ai.robot.helpdesk;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quanxiaoha.ai.robot.commerce.CommerceDtos;
import com.quanxiaoha.ai.robot.domain.dos.CsMessageDO;
import com.quanxiaoha.ai.robot.domain.dos.TicketDO;
import com.quanxiaoha.ai.robot.domain.mapper.CsMessageMapper;
import com.quanxiaoha.ai.robot.domain.mapper.TicketMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class HelpdeskService {

    @Resource
    private TicketMapper ticketMapper;
    @Resource
    private CsMessageMapper csMessageMapper;
    @Resource
    private ConversationStateService conversationStateService;

    public CommerceDtos.EscalateResult escalate(String subject, String summary, String customerContact,
                                                String orderNo, String chatId) {
        LocalDateTime now = LocalDateTime.now();
        String ticketNo = "TK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        TicketDO ticket = TicketDO.builder()
                .ticketNo(ticketNo)
                .chatId(chatId)
                .customerContact(customerContact)
                .orderNo(orderNo)
                .subject(StringUtils.hasText(subject) ? subject : "客户请求人工客服")
                .summary(summary)
                .status(ConversationStateService.HUMAN_ASSIGNED)
                .createTime(now)
                .updateTime(now)
                .build();
        ticketMapper.insert(ticket);
        if (StringUtils.hasText(chatId)) {
            conversationStateService.markHumanAssigned(chatId);
        }
        return new CommerceDtos.EscalateResult(true, ticketNo, ticket.getStatus(),
                "已创建人工工单，客服同事会尽快接手。");
    }

    public List<TicketDO> list(String status) {
        return ticketMapper.selectList(Wrappers.<TicketDO>lambdaQuery()
                .eq(StringUtils.hasText(status), TicketDO::getStatus, status)
                .orderByDesc(TicketDO::getId));
    }

    public TicketDO getById(Long id) {
        return ticketMapper.selectById(id);
    }

    public TicketDO takeover(Long id, String assignee, String note) {
        TicketDO ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new IllegalArgumentException("工单不存在");
        }
        ticket.setStatus(ConversationStateService.HUMAN_ASSIGNED);
        ticket.setAssignee(assignee);
        if (StringUtils.hasText(note)) {
            ticket.setInternalNote(note);
        }
        ticket.setUpdateTime(LocalDateTime.now());
        ticketMapper.updateById(ticket);
        if (StringUtils.hasText(ticket.getChatId())) {
            conversationStateService.markHumanAssigned(ticket.getChatId());
        }
        return ticket;
    }

    public TicketDO resolve(Long id, String note) {
        TicketDO ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new IllegalArgumentException("工单不存在");
        }
        ticket.setStatus(ConversationStateService.RESOLVED);
        if (StringUtils.hasText(note)) {
            ticket.setInternalNote(note);
        }
        ticket.setUpdateTime(LocalDateTime.now());
        ticketMapper.updateById(ticket);
        if (StringUtils.hasText(ticket.getChatId())) {
            conversationStateService.markResolved(ticket.getChatId());
        }
        return ticket;
    }

    public void appendMessage(String chatId, String role, String content, String metaJson) {
        if (!StringUtils.hasText(chatId) || !StringUtils.hasText(content)) {
            return;
        }
        csMessageMapper.insert(CsMessageDO.builder()
                .chatId(chatId)
                .role(role)
                .content(content)
                .metaJson(metaJson)
                .createTime(LocalDateTime.now())
                .build());
    }

    public List<CsMessageDO> listMessages(String chatId) {
        if (!StringUtils.hasText(chatId)) {
            return List.of();
        }
        return csMessageMapper.selectList(Wrappers.<CsMessageDO>lambdaQuery()
                .eq(CsMessageDO::getChatId, chatId)
                .orderByAsc(CsMessageDO::getId));
    }

    /**
     * 人工对买家可见回复（写入同一 chat）。
     */
    public CsMessageDO replyToChat(Long ticketId, String content, String agentName) {
        TicketDO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new IllegalArgumentException("工单不存在");
        }
        if (!StringUtils.hasText(ticket.getChatId())) {
            throw new IllegalArgumentException("工单未绑定会话，无法回复");
        }
        String body = StringUtils.hasText(agentName)
                ? ("【人工客服 " + agentName + "】" + content)
                : content;
        CsMessageDO msg = CsMessageDO.builder()
                .chatId(ticket.getChatId())
                .role("human")
                .content(body)
                .metaJson("{\"from\":\"inbox\",\"ticketNo\":\"" + ticket.getTicketNo() + "\"}")
                .createTime(LocalDateTime.now())
                .build();
        csMessageMapper.insert(msg);
        if (!ConversationStateService.HUMAN_ASSIGNED.equals(ticket.getStatus())
                && !ConversationStateService.RESOLVED.equals(ticket.getStatus())) {
            ticket.setStatus(ConversationStateService.HUMAN_ASSIGNED);
            ticket.setUpdateTime(LocalDateTime.now());
            ticketMapper.updateById(ticket);
            conversationStateService.markHumanAssigned(ticket.getChatId());
        }
        return msg;
    }
}
