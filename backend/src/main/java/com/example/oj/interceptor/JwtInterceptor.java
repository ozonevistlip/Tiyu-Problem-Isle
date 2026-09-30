package com.example.oj.interceptor;

import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.utils.JwtUtils;
import com.example.oj.utils.UserContext;
import com.example.oj.utils.LoginUser;
import com.example.oj.entity.User;
import com.example.oj.entity.UserSession;
import com.example.oj.mapper.UserMapper;
import com.example.oj.mapper.UserSessionMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {
    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;
    private final UserSessionMapper userSessionMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        try {
            LoginUser loginUser = jwtUtils.parseToken(header.substring(7));
            if (loginUser.getSessionId() == null) {
                throw new IllegalStateException("missing session");
            }
            UserSession session = userSessionMapper.selectById(loginUser.getSessionId());
            User user = userMapper.selectById(loginUser.getUserId());
            LocalDateTime now = LocalDateTime.now();
            if (session == null || user == null || user.getStatus() == null || user.getStatus() != 1
                    || !"ACTIVE".equals(session.getStatus()) || session.getExpiresAt().isBefore(now)
                    || !user.getRole().equals(loginUser.getRole()) || !user.getId().equals(session.getUserId())) {
                throw new IllegalStateException("inactive account or session");
            }
            if (session.getLastActiveAt() == null || session.getLastActiveAt().isBefore(now.minusMinutes(1))) {
                userSessionMapper.update(null, new LambdaUpdateWrapper<UserSession>()
                        .eq(UserSession::getId, session.getId()).set(UserSession::getLastActiveAt, now));
                userMapper.update(null, new LambdaUpdateWrapper<User>()
                        .eq(User::getId, user.getId()).set(User::getLastActiveAt, now));
            }
            UserContext.set(loginUser);
            return true;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
