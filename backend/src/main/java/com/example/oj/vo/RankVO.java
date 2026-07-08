package com.example.oj.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RankVO {
    private Long studentId;
    private String studentName;
    private Integer totalScore;
    private Integer acceptedCount;
    private Integer submitCount;
    private LocalDateTime lastSubmitAt;
}
