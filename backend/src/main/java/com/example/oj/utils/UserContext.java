package com.example.oj.utils;

import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;

public final class UserContext {
    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser loginUser) {
        HOLDER.set(loginUser);
    }

    public static LoginUser get() {
        LoginUser loginUser = HOLDER.get();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return loginUser;
    }

    public static Long userId() {
        return get().getUserId();
    }

    public static String role() {
        return get().getRole();
    }

    public static void requireTeacher() {
        if (!"teacher".equals(role())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅教师可操作");
        }
    }

    public static void requireStudent() {
        if (!"student".equals(role())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅学生可操作");
        }
    }

    public static void requireSuperAdmin() {
        if (!"SUPER_ADMIN".equals(role())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅超级管理员可操作");
        }
    }

    public static void clear() {
        HOLDER.remove();
    }
}
