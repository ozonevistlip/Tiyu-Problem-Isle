package com.example.oj.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUpdateUserRequest {
    @Size(min = 3, max = 30, message = "账号长度必须在 3 到 30 个字符之间")
    private String username;
    private String realName;
    private String nickname;
    @Pattern(regexp = "teacher|student", message = "角色只能是老师或学生")
    private String role;
}
