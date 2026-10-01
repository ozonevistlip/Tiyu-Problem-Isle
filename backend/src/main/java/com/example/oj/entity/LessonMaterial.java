package com.example.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("lesson_material")
public class LessonMaterial {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long lessonId;
    private String tier;
    private String kind;
    private String title;
    private String content;
    private String filePath;
    private String fileName;
    private String mimeType;
}
