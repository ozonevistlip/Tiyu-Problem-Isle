package com.example.oj.controller;

import com.example.oj.common.Result;
import com.example.oj.dto.CustomTestRequest;
import com.example.oj.dto.SubmitCodeRequest;
import com.example.oj.service.StudentService;
import com.example.oj.utils.UserContext;
import com.example.oj.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/student/contests")
public class StudentContestController {
    private final StudentService studentService;

    @GetMapping
    public Result<List<ContestVO>> list() {
        return Result.success(studentService.listContests());
    }

    @GetMapping("/{contestId}")
    public Result<ContestVO> get(@PathVariable Long contestId) {
        return Result.success(studentService.getContest(contestId));
    }

    @GetMapping("/{contestId}/problems")
    public Result<List<ContestProblemVO>> problems(@PathVariable Long contestId) {
        return Result.success(studentService.listProblems(contestId));
    }

    @GetMapping("/{contestId}/problems/{problemId}")
    public Result<ContestProblemVO> problem(@PathVariable Long contestId, @PathVariable Long problemId) {
        return Result.success(studentService.getProblem(contestId, problemId));
    }

    @GetMapping("/{contestId}/problems/{problemId}/hint")
    public Result<HintView> hint(@PathVariable Long contestId, @PathVariable Long problemId) {
        return Result.success(studentService.getContestProblemHint(contestId, problemId, UserContext.userId()));
    }

    @PostMapping("/{contestId}/problems/{problemId}/submit")
    public Result<SubmissionVO> submit(@PathVariable Long contestId,
                                       @PathVariable Long problemId,
                                       @Valid @RequestBody SubmitCodeRequest request) {
        return Result.success(studentService.submit(contestId, problemId, request));
    }

    @PostMapping("/{contestId}/problems/{problemId}/run")
    public Result<CustomTestVO> runCustomTest(@PathVariable Long contestId,
                                              @PathVariable Long problemId,
                                              @Valid @RequestBody CustomTestRequest request) {
        return Result.success(studentService.runCustomTest(contestId, problemId, request));
    }

    @GetMapping("/{contestId}/submissions")
    public Result<List<SubmissionVO>> submissions(@PathVariable Long contestId) {
        return Result.success(studentService.submissions(contestId));
    }

    @GetMapping("/{contestId}/rank")
    public Result<List<RankVO>> rank(@PathVariable Long contestId) {
        return Result.success(studentService.rank(contestId));
    }
}
