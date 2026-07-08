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
@TableName("contest_problem")
public class ContestProblem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contestId;
    private Long problemId;
    private Integer displayOrder;
    private Integer score;
    private Integer hintUnlockMinutes;
    private Boolean showHintAfterAc;
    private Boolean showHintAfterContest;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
