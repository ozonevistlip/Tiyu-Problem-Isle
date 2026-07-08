package com.example.oj.common;

import lombok.Getter;

@Getter
public enum ErrorCode {
    PARAM_ERROR(40000, "参数错误"),
    UNAUTHORIZED(40001, "未登录或登录已过期"),
    FORBIDDEN(40003, "无权限访问"),
    NOT_FOUND(40004, "资源不存在"),
    OPERATION_ERROR(50000, "操作失败"),
    SYSTEM_ERROR(50001, "系统异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
