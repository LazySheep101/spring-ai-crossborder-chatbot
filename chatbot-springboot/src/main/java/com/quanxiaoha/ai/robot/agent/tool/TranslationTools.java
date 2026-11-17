package com.quanxiaoha.ai.robot.agent.tool;

import com.quanxiaoha.ai.robot.agent.ToolAuditService;
import com.quanxiaoha.ai.robot.i18n.LanguageDetector;
import com.quanxiaoha.ai.robot.i18n.LanguageEvidenceService;
import com.quanxiaoha.ai.robot.i18n.SnippetTranslator;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TranslationTools {

    @Resource
    private LanguageDetector languageDetector;
    @Resource
    private LanguageEvidenceService languageEvidenceService;
    @Resource
    private ToolAuditService toolAuditService;

    @Tool(description = """
            Translate a short customer-service snippet between Chinese and English. \
            If no translation API is configured, returns the original text with fallback=true. \
            Do not use this tool to invent order or policy facts.
            """)
    public SnippetTranslator.Result translateSnippet(
            @ToolParam(description = "Text to translate") String text,
            @ToolParam(description = "Target language: zh or en") String targetLang) {
        return toolAuditService.record("translateSnippet",
                Map.of("targetLang", targetLang == null ? "" : targetLang),
                () -> {
                    SnippetTranslator.Result result = SnippetTranslator.translate(text, targetLang, languageDetector);
                    languageEvidenceService.record(
                            null,
                            result.sourceLang(),
                            result.targetLang(),
                            "translateSnippet",
                            result.fallback(),
                            result.reason(),
                            text);
                    return result;
                });
    }
}
