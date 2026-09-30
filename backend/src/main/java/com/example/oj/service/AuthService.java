package com.example.oj.service;

import com.example.oj.dto.LoginRequest;
import com.example.oj.dto.RegisterRequest;
import com.example.oj.vo.LoginResponse;
import com.example.oj.vo.UserVO;

public interface AuthService {
    UserVO register(RegisterRequest request);

    LoginResponse login(LoginRequest request, String ipAddress, String userAgent);

    void logout();

    UserVO me();
}
