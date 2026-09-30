package com.example.oj.interceptor;

import com.example.oj.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SuperAdminInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"OPTIONS".equalsIgnoreCase(request.getMethod())) {
            UserContext.requireSuperAdmin();
        }
        return true;
    }
}
