package com.quanxiaoha.ai.robot.config;

import tools.jackson.databind.ObjectMapper;
import com.quanxiaoha.ai.robot.commerce.AdminAuthService;
import com.quanxiaoha.ai.robot.utils.Response;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    @Resource
    private AdminAuthService adminAuthService;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (path != null && path.endsWith("/admin/auth/login")) {
            return true;
        }
        try {
            adminAuthService.requireUser(request.getHeader("Authorization"));
            return true;
        } catch (IllegalArgumentException ex) {
            response.setStatus(401);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(Response.fail(ex.getMessage())));
            return false;
        }
    }
}
