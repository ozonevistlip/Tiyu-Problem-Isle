package com.example.oj.controller;

import com.example.oj.common.Result;
import com.example.oj.dto.LoginRequest;
import com.example.oj.dto.RegisterRequest;
import com.example.oj.service.AuthService;
import com.example.oj.vo.LoginResponse;
import com.example.oj.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return Result.success(authService.login(request, clientIp(servletRequest), servletRequest.getHeader("User-Agent")));
    }

    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(authService.me());
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success(null);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
