package com.example.oj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度必须在 6 到 64 个字符之间")
    private String password;
}
