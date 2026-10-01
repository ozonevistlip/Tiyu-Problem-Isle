package com.example.oj.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ClassVO {
    private Long id;
    private Long teacherId;
    private Long classTypeId;
    private String classTypeName;
    private String className;
    private String description;
    private Integer status;
    private LocalDateTime createdAt;
}
