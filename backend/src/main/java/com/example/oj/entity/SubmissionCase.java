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
@TableName("submission_case")
public class SubmissionCase {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private Long testcaseId;
    private String status;
    private Integer timeUsedMs;
    private Integer memoryUsedKb;
    private String inputPreview;
    private String expectedOutputPreview;
    private String actualOutputPreview;
    private String errorMessage;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
