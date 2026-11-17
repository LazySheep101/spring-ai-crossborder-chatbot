package com.quanxiaoha.ai.robot.helpdesk;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quanxiaoha.ai.robot.domain.dos.ConversationStateDO;
import com.quanxiaoha.ai.robot.domain.mapper.ConversationStateMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class ConversationStateService {

    public static final String AI_WORKING = "AI_WORKING";
    public static final String NEEDS_APPROVAL = "NEEDS_APPROVAL";
    public static final String HUMAN_ASSIGNED = "HUMAN_ASSIGNED";
    public static final String RESOLVED = "RESOLVED";

    @Resource
    private ConversationStateMapper conversationStateMapper;

    public void ensureAiWorking(String chatId) {
        if (!StringUtils.hasText(chatId)) {
            return;
        }
        ConversationStateDO existing = get(chatId);
        if (existing == null) {
            LocalDateTime now = LocalDateTime.now();
            conversationStateMapper.insert(ConversationStateDO.builder()
                    .chatId(chatId)
                    .status(AI_WORKING)
                    .createTime(now)
                    .updateTime(now)
                    .build());
        }
    }

    public void markAiWorking(String chatId) {
        upsert(chatId, AI_WORKING);
    }

    public void markNeedsApproval(String chatId) {
        upsert(chatId, NEEDS_APPROVAL);
    }

    public void markHumanAssigned(String chatId) {
        upsert(chatId, HUMAN_ASSIGNED);
    }

    public void markResolved(String chatId) {
        upsert(chatId, RESOLVED);
    }

    public ConversationStateDO get(String chatId) {
        if (!StringUtils.hasText(chatId)) {
            return null;
        }
        return conversationStateMapper.selectOne(Wrappers.<ConversationStateDO>lambdaQuery()
                .eq(ConversationStateDO::getChatId, chatId).last("limit 1"));
    }

    private void upsert(String chatId, String status) {
        if (!StringUtils.hasText(chatId)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        ConversationStateDO existing = get(chatId);
        if (existing == null) {
            conversationStateMapper.insert(ConversationStateDO.builder()
                    .chatId(chatId)
                    .status(status)
                    .createTime(now)
                    .updateTime(now)
                    .build());
        } else {
            // 已结单不再回退
            if (RESOLVED.equals(existing.getStatus())) {
                return;
            }
            // HUMAN 优先于 AI；NEEDS_APPROVAL 可覆盖 AI
            if (HUMAN_ASSIGNED.equals(existing.getStatus()) && AI_WORKING.equals(status)) {
                return;
            }
            existing.setStatus(status);
            existing.setUpdateTime(now);
            conversationStateMapper.updateById(existing);
        }
    }
}
