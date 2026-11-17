package com.quanxiaoha.ai.robot.i18n;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quanxiaoha.ai.robot.domain.dos.LangEventDO;
import com.quanxiaoha.ai.robot.domain.mapper.LangEventMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class LanguageEvidenceService {

    @Resource
    private LangEventMapper langEventMapper;

    public void record(String conversationId, String sourceLang, String targetLang,
                       String toolName, boolean fallback, String reason, String snippet) {
        try {
            langEventMapper.insert(LangEventDO.builder()
                    .conversationId(conversationId)
                    .sourceLang(sourceLang)
                    .targetLang(targetLang)
                    .toolName(toolName)
                    .fallback(fallback)
                    .reason(reason)
                    .snippet(truncate(snippet))
                    .createTime(LocalDateTime.now())
                    .build());
        } catch (Exception ex) {
            log.warn("语言证据写入失败: {}", ex.getMessage());
        }
    }

    public List<LangEventDO> list(int limit) {
        int size = Math.min(Math.max(limit, 1), 200);
        return langEventMapper.selectList(Wrappers.<LangEventDO>lambdaQuery()
                .orderByDesc(LangEventDO::getId)
                .last("limit " + size));
    }

    private static String truncate(String snippet) {
        if (snippet == null) {
            return null;
        }
        return snippet.length() > 500 ? snippet.substring(0, 500) : snippet;
    }
}
