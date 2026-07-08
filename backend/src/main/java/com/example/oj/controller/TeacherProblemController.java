package com.example.oj.controller;

import com.example.oj.common.Result;
import com.example.oj.dto.CreateHintRequest;
import com.example.oj.dto.CreateProblemRequest;
import com.example.oj.dto.CreateTestcaseRequest;
import com.example.oj.service.TeacherService;
import com.example.oj.vo.HintVO;
import com.example.oj.vo.ProblemVO;
import com.example.oj.vo.TestcaseVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teacher")
public class TeacherProblemController {
    private final TeacherService teacherService;

    @PostMapping("/problems")
    public Result<ProblemVO> createProblem(@Valid @RequestBody CreateProblemRequest request) {
        return Result.success(teacherService.createProblem(request));
    }

    @GetMapping("/problems")
    public Result<List<ProblemVO>> listProblems() {
        return Result.success(teacherService.listProblems());
    }

    @GetMapping("/problems/{problemId}")
    public Result<ProblemVO> getProblem(@PathVariable Long problemId) {
        return Result.success(teacherService.getProblem(problemId));
    }

    @PutMapping("/problems/{problemId}")
    public Result<ProblemVO> updateProblem(@PathVariable Long problemId, @Valid @RequestBody CreateProblemRequest request) {
        return Result.success(teacherService.updateProblem(problemId, request));
    }

    @DeleteMapping("/problems/{problemId}")
    public Result<Void> deleteProblem(@PathVariable Long problemId) {
        teacherService.deleteProblem(problemId);
        return Result.success(null);
    }

    @PostMapping("/problems/{problemId}/testcases")
    public Result<TestcaseVO> createTestcase(@PathVariable Long problemId, @Valid @RequestBody CreateTestcaseRequest request) {
        return Result.success(teacherService.createTestcase(problemId, request));
    }

    @GetMapping("/problems/{problemId}/testcases")
    public Result<List<TestcaseVO>> listTestcases(@PathVariable Long problemId) {
        return Result.success(teacherService.listTestcases(problemId));
    }

    @PutMapping("/testcases/{testcaseId}")
    public Result<TestcaseVO> updateTestcase(@PathVariable Long testcaseId, @Valid @RequestBody CreateTestcaseRequest request) {
        return Result.success(teacherService.updateTestcase(testcaseId, request));
    }

    @DeleteMapping("/testcases/{testcaseId}")
    public Result<Void> deleteTestcase(@PathVariable Long testcaseId) {
        teacherService.deleteTestcase(testcaseId);
        return Result.success(null);
    }

    @PostMapping("/problems/{problemId}/hints")
    public Result<HintVO> createHint(@PathVariable Long problemId, @Valid @RequestBody CreateHintRequest request) {
        return Result.success(teacherService.createHint(problemId, request));
    }

    @GetMapping("/problems/{problemId}/hints")
    public Result<List<HintVO>> listHints(@PathVariable Long problemId) {
        return Result.success(teacherService.listHints(problemId));
    }

    @PutMapping("/hints/{hintId}")
    public Result<HintVO> updateHint(@PathVariable Long hintId, @Valid @RequestBody CreateHintRequest request) {
        return Result.success(teacherService.updateHint(hintId, request));
    }

    @DeleteMapping("/hints/{hintId}")
    public Result<Void> deleteHint(@PathVariable Long hintId) {
        teacherService.deleteHint(hintId);
        return Result.success(null);
    }
}
