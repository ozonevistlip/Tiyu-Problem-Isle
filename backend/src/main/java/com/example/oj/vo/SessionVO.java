package com.example.oj.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SessionVO {
    private String id;
    private Long userId;
    private String username;
    private String realName;
    private String role;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime loginAt;
    private LocalDateTime lastActiveAt;
}
