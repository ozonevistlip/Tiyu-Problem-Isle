package com.example.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("lesson")
public class Lesson {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long classTypeId;
    private String title;
    private Integer lessonOrder;
    private Integer status;
}
