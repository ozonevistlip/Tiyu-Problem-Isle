package com.example.oj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

@Data
public class AnnouncementRequest {
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 150, message = "公告标题最多 150 个字符")
    private String title;
    @NotBlank(message = "公告内容不能为空")
    private String content;
    @Min(0)
    @Max(1)
    private Integer status;
}
