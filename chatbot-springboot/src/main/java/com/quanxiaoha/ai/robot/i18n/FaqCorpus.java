package com.quanxiaoha.ai.robot.i18n;

import com.quanxiaoha.ai.robot.commerce.CommerceDtos;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 内置中英政策 FAQ。向量库未命中时，政策工具仍能引用这些片段。
 */
@Component
public class FaqCorpus {

    private static final Pattern WORD = Pattern.compile("[a-zA-Z]{3,}");
    private static final Set<String> STOP = Set.of(
            "the", "and", "for", "you", "your", "what", "how", "can", "with", "this", "that", "are", "is");

    private final List<Section> sections = new ArrayList<>();

    public FaqCorpus() {
        load("knowledge/faq-zh.md", "zh");
        load("knowledge/faq-en.md", "en");
    }

    public List<CommerceDtos.PolicyChunk> search(String question, int limit) {
        if (!StringUtils.hasText(question) || sections.isEmpty()) {
            return List.of();
        }
        List<String> tokens = tokens(question);
        if (tokens.isEmpty()) {
            return List.of();
        }
        record Scored(Section section, int score) {
        }
        List<Scored> ranked = new ArrayList<>();
        for (Section section : sections) {
            int score = 0;
            String hay = section.text.toLowerCase(Locale.ROOT);
            for (String token : tokens) {
                if (hay.contains(token)) {
                    score++;
                }
            }
            if (score > 0) {
                ranked.add(new Scored(section, score));
            }
        }
        ranked.sort(Comparator.comparingInt(Scored::score).reversed());
        int n = Math.min(Math.max(limit, 1), ranked.size());
        List<CommerceDtos.PolicyChunk> hits = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Scored s = ranked.get(i);
            double score = s.score / (double) tokens.size();
            hits.add(new CommerceDtos.PolicyChunk(
                    "faq:" + s.section.lang + ":" + s.section.title,
                    s.section.text,
                    score,
                    null));
        }
        return hits;
    }

    private void load(String path, String lang) {
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String title = lang;
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("# ")) {
                    flush(lang, title, body);
                    title = line.substring(2).trim();
                    body.setLength(0);
                } else {
                    body.append(line).append('\n');
                }
            }
            flush(lang, title, body);
        } catch (Exception ignored) {
            // 语料缺失时政策工具退回向量检索结果
        }
    }

    private void flush(String lang, String title, StringBuilder body) {
        String text = body.toString().trim();
        if (!StringUtils.hasText(text)) {
            return;
        }
        sections.add(new Section(lang, title, title + "\n" + text));
    }

    private static List<String> tokens(String question) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = WORD.matcher(question);
        while (matcher.find()) {
            String word = matcher.group().toLowerCase(Locale.ROOT);
            if (!STOP.contains(word)) {
                tokens.add(word);
            }
        }
        StringBuilder cjk = new StringBuilder();
        for (int i = 0; i < question.length(); i++) {
            char c = question.charAt(i);
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                cjk.append(c);
            }
        }
        String han = cjk.toString();
        for (int i = 0; i + 1 < han.length(); i++) {
            tokens.add(han.substring(i, i + 2));
        }
        return tokens;
    }

    private record Section(String lang, String title, String text) {
    }
}
