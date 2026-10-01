package com.example.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("lesson_student_tier")
public class LessonStudentTier {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long lessonId;
    private Long classId;
    private Long studentId;
    private String tier;
}
