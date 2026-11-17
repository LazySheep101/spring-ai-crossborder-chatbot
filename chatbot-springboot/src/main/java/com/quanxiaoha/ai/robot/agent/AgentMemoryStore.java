package com.quanxiaoha.ai.robot.agent;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客服 Agent 多轮记忆（内存；按 conversationId 隔离）。
 */
@Component
public class AgentMemoryStore {

    private static final int MAX_MESSAGES = 12;
    private final Map<String, List<Message>> store = new ConcurrentHashMap<>();

    public List<Message> history(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return List.of();
        }
        return List.copyOf(store.getOrDefault(conversationId, List.of()));
    }

    public void append(String conversationId, String user, String assistant) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        store.compute(conversationId, (k, old) -> {
            List<Message> list = old == null ? new ArrayList<>() : new ArrayList<>(old);
            list.add(new UserMessage(user));
            list.add(new AssistantMessage(assistant == null ? "" : assistant));
            while (list.size() > MAX_MESSAGES) {
                list.remove(0);
            }
            return list;
        });
    }
}
