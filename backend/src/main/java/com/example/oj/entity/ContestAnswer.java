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
@TableName("contest_answer")
public class ContestAnswer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contestId;
    private Long problemId;
    private Long studentId;
    private String status;
    private Integer bestScore;
    private Integer submitCount;
    private LocalDateTime firstOpenAt;
    private LocalDateTime firstSubmitAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime lastSubmitAt;
    private Boolean hintShown;
    private LocalDateTime hintFirstShownAt;
    private Long finalSubmissionId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
