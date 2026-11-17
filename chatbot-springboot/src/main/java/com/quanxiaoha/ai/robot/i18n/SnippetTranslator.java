package com.quanxiaoha.ai.robot.i18n;

import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 不接外部翻译 API。命中少量客服术语表则给出译文，否则回退原文并标明原因。
 */
public final class SnippetTranslator {

    private static final Map<String, String> ZH_TO_EN = new LinkedHashMap<>();
    private static final Map<String, String> EN_TO_ZH = new LinkedHashMap<>();

    static {
        put("七天无理由退货", "7-day no-reason return");
        put("关税由买家承担", "Import duty is paid by the buyer");
        put("签收后 7 天", "within 7 days after delivery");
        put("最终售出不支持无理由退货", "Final-sale items cannot be returned");
        put("物流时效", "shipping time");
    }

    private SnippetTranslator() {
    }

    public record Result(String text, String sourceLang, String targetLang, boolean fallback, String reason) {
    }

    public static Result translate(String text, String targetLang, LanguageDetector detector) {
        String source = detector == null ? "unknown" : detector.detect(text);
        String target = normalize(targetLang);
        if (!StringUtils.hasText(text)) {
            return new Result("", source, target, true, "EMPTY");
        }
        if (source.equals(target)) {
            return new Result(text, source, target, false, "SAME_LANGUAGE");
        }
        String hit = lookup(text, source, target);
        if (hit != null) {
            return new Result(hit, source, target, false, "GLOSSARY");
        }
        return new Result(text, source, target, true, "NO_TRANSLATION_API");
    }

    private static void put(String zh, String en) {
        ZH_TO_EN.put(zh, en);
        EN_TO_ZH.put(en.toLowerCase(Locale.ROOT), zh);
    }

    private static String lookup(String text, String source, String target) {
        if ("en".equals(target)) {
            for (Map.Entry<String, String> e : ZH_TO_EN.entrySet()) {
                if (text.contains(e.getKey())) {
                    return e.getValue();
                }
            }
        }
        if ("zh".equals(target)) {
            String lower = text.toLowerCase(Locale.ROOT);
            for (Map.Entry<String, String> e : EN_TO_ZH.entrySet()) {
                if (lower.contains(e.getKey())) {
                    return e.getValue();
                }
            }
        }
        if ("unknown".equals(source)) {
            return null;
        }
        return null;
    }

    private static String normalize(String targetLang) {
        if (!StringUtils.hasText(targetLang)) {
            return "zh";
        }
        String t = targetLang.trim().toLowerCase(Locale.ROOT);
        if (t.startsWith("en")) {
            return "en";
        }
        if (t.startsWith("zh") || t.contains("中")) {
            return "zh";
        }
        return t;
    }
}
