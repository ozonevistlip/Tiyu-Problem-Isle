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
@TableName("site_visit")
public class SiteVisit {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String visitorKey;
    private String path;
    private String ipAddress;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
