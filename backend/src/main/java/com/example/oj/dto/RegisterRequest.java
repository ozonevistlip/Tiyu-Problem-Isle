package com.example.oj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "账号不能为空")
    @Size(min = 3, max = 30, message = "账号长度必须在 3 到 30 个字符之间")
    private String username;
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度必须在 6 到 64 个字符之间")
    private String password;
    @NotBlank(message = "身份不能为空")
    @Pattern(regexp = "teacher|student", message = "身份必须是老师或学生")
    private String role;
    private String realName;
    private String nickname;
}
