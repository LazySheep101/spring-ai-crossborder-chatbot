package com.quanxiaoha.ai.robot.commerce;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/**
 * 简版 HMAC JWT：payload 只有用户名和过期时间，角色以数据库为准。
 */
@Component
public class AdminJwtService {

    @Value("${admin.jwt.secret:your_admin_jwt_secret}")
    private String secret;

    @Value("${admin.jwt.ttl-hours:168}")
    private long ttlHours;

    public String issue(String username) {
        long exp = Instant.now().plusSeconds(Math.max(ttlHours, 1) * 3600).getEpochSecond();
        String payload = username + "\n" + exp;
        String body = b64(payload.getBytes(StandardCharsets.UTF_8));
        return body + "." + b64(sign(body));
    }

    public String requireUsername(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("未登录");
        }
        String raw = token.startsWith("Bearer ") ? token.substring(7).trim() : token.trim();
        int dot = raw.indexOf('.');
        if (dot <= 0 || dot == raw.length() - 1) {
            throw new IllegalArgumentException("登录已失效，请重新登录");
        }
        String body = raw.substring(0, dot);
        String sig = raw.substring(dot + 1);
        byte[] expected = sign(body);
        byte[] actual;
        try {
            actual = Base64.getUrlDecoder().decode(sig);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("登录已失效，请重新登录");
        }
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new IllegalArgumentException("登录已失效，请重新登录");
        }
        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(body), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("登录已失效，请重新登录");
        }
        int nl = payload.indexOf('\n');
        if (nl <= 0) {
            throw new IllegalArgumentException("登录已失效，请重新登录");
        }
        long exp;
        try {
            exp = Long.parseLong(payload.substring(nl + 1).trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("登录已失效，请重新登录");
        }
        if (Instant.now().getEpochSecond() > exp) {
            throw new IllegalArgumentException("登录已失效，请重新登录");
        }
        return payload.substring(0, nl);
    }

    private byte[] sign(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("JWT 签名失败", ex);
        }
    }

    private static String b64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
