package com.example.oj.controller;

import com.example.oj.common.Result;
import com.example.oj.service.SubmissionService;
import com.example.oj.vo.SubmissionCaseVO;
import com.example.oj.vo.SubmissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/submissions")
public class SubmissionController {
    private final SubmissionService submissionService;

    @GetMapping("/{submissionId}")
    public Result<SubmissionVO> get(@PathVariable Long submissionId) {
        return Result.success(submissionService.getSubmission(submissionId));
    }

    @GetMapping("/{submissionId}/cases")
    public Result<List<SubmissionCaseVO>> cases(@PathVariable Long submissionId) {
        return Result.success(submissionService.listCases(submissionId));
    }
}
