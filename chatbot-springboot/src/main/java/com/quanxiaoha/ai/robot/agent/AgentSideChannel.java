package com.quanxiaoha.ai.robot.agent;

import com.quanxiaoha.ai.robot.commerce.CommerceDtos;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 单次 Agent 请求内收集 citation / 审批 / 工单等旁路事件，供 SSE 在文本流后推送。
 */
public final class AgentSideChannel {

    private static final ThreadLocal<List<Event>> HOLDER = ThreadLocal.withInitial(CopyOnWriteArrayList::new);

    private AgentSideChannel() {
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static void addCitation(CommerceDtos.PolicyAnswer answer) {
        if (answer == null || answer.citations() == null || answer.citations().isEmpty()) {
            return;
        }
        HOLDER.get().add(Event.citation(answer.citations()));
    }

    public static void addAction(CommerceDtos.ActionResult result) {
        if (result == null || result.requestId() == null) {
            return;
        }
        HOLDER.get().add(Event.action(result.actionType(), result.requestId(), result.status(), result.message()));
    }

    public static void addTicket(CommerceDtos.EscalateResult result) {
        if (result == null || !result.ok()) {
            return;
        }
        HOLDER.get().add(Event.ticket(result.ticketNo(), result.status(), result.message()));
    }

    public static List<Event> drain() {
        List<Event> copy = new ArrayList<>(HOLDER.get());
        HOLDER.remove();
        return copy;
    }

    public record Event(String kind, Object payload) {
        public static Event citation(List<CommerceDtos.PolicyChunk> citations) {
            return new Event("citation", citations);
        }

        public static Event action(String actionType, Long requestId, String status, String message) {
            return new Event("action", new ActionPayload(actionType, requestId, status, message));
        }

        public static Event ticket(String ticketNo, String status, String message) {
            return new Event("ticket", new TicketPayload(ticketNo, status, message));
        }
    }

    public record ActionPayload(String actionType, Long requestId, String status, String message) {
    }

    public record TicketPayload(String ticketNo, String status, String message) {
    }
}
