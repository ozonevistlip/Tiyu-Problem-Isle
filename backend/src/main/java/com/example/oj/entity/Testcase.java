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
@TableName("testcase")
public class Testcase {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long problemId;
    private String inputData;
    private String outputData;
    private Integer score;
    private Integer sortOrder;
    @TableField("is_sample")
    private Boolean sample;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
