package com.example.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.dto.LoginRequest;
import com.example.oj.dto.RegisterRequest;
import com.example.oj.entity.User;
import com.example.oj.mapper.UserMapper;
import com.example.oj.entity.UserSession;
import com.example.oj.entity.SiteSetting;
import com.example.oj.mapper.UserSessionMapper;
import com.example.oj.mapper.SiteSettingMapper;
import com.example.oj.service.AuthService;
import com.example.oj.utils.JwtUtils;
import com.example.oj.utils.PasswordUtils;
import com.example.oj.utils.UserContext;
import com.example.oj.vo.LoginResponse;
import com.example.oj.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final UserSessionMapper userSessionMapper;
    private final SiteSettingMapper siteSettingMapper;

    @Override
    public UserVO register(RegisterRequest request) {
        SiteSetting registration = siteSettingMapper.selectById("registration_enabled");
        if (registration != null && !Boolean.parseBoolean(registration.getSettingValue())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "网站当前未开放注册");
        }
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (exists > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名已存在");
        }
        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(PasswordUtils.hash(request.getPassword()))
                .realName(request.getRealName())
                .nickname(request.getNickname())
                .role(request.getRole())
                .status(1)
                .build();
        userMapper.insert(user);
        return toUserVO(user);
    }

    @Override
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
                .last("LIMIT 1"));
        if (user == null || user.getStatus() == null || user.getStatus() != 1
                || !PasswordUtils.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名或密码错误");
        }
        String sessionId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        userSessionMapper.insert(UserSession.builder()
                .id(sessionId)
                .userId(user.getId())
                .status("ACTIVE")
                .ipAddress(limit(ipAddress, 64))
                .userAgent(limit(userAgent, 500))
                .expiresAt(LocalDateTime.ofInstant(jwtUtils.expiresAt(), ZoneId.systemDefault()))
                .lastActiveAt(now)
                .build());
        user.setLastLoginAt(now);
        user.setLastActiveAt(now);
        userMapper.updateById(user);
        return LoginResponse.builder()
                .token(jwtUtils.generateToken(user.getId(), user.getRole(), sessionId))
                .user(toUserVO(user))
                .build();
    }

    @Override
    public void logout() {
        if (UserContext.get().getSessionId() != null) {
            UserSession session = userSessionMapper.selectById(UserContext.get().getSessionId());
            if (session != null) {
                session.setStatus("LOGGED_OUT");
                userSessionMapper.updateById(session);
            }
        }
    }

    @Override
    public UserVO me() {
        User user = userMapper.selectById(UserContext.userId());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return toUserVO(user);
    }

    static UserVO toUserVO(User user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .nickname(user.getNickname())
                .role(user.getRole())
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt())
                .lastActiveAt(user.getLastActiveAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private String limit(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
