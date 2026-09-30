package com.example.oj.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_session")
public class UserSession {
    @TableId(type = IdType.INPUT)
    private String id;
    private Long userId;
    private String status;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime expiresAt;
    private LocalDateTime lastActiveAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
