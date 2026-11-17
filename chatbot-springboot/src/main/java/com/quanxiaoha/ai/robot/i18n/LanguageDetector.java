package com.quanxiaoha.ai.robot.i18n;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 轻量语种检测：zh / en / mixed / unknown（不引入重依赖）。
 */
@Component
public class LanguageDetector {

    public String detect(String text) {
        if (!StringUtils.hasText(text)) {
            return "unknown";
        }
        int cjk = 0;
        int latin = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                cjk++;
            } else if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                latin++;
            }
        }
        if (cjk == 0 && latin == 0) {
            return "unknown";
        }
        if (cjk > 0 && latin > 0) {
            return "mixed";
        }
        return cjk > latin ? "zh" : "en";
    }
}
