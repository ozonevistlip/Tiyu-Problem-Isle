package com.example.oj.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ContestVO {
    private Long id;
    private Long teacherId;
    private Long classId;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Boolean showRank;
    private Boolean allowSubmitAfterEnd;
}
