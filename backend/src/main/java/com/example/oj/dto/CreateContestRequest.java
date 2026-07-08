package com.example.oj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateContestRequest {
    @NotNull
    private Long classId;
    @NotBlank
    private String title;
    private String description;
    @NotNull
    private LocalDateTime startTime;
    @NotNull
    private LocalDateTime endTime;
    private Boolean showRank;
    private Boolean allowSubmitAfterEnd;
}
