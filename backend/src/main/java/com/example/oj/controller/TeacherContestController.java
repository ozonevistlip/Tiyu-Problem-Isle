package com.example.oj.controller;

import com.example.oj.common.Result;
import com.example.oj.dto.AddContestProblemRequest;
import com.example.oj.dto.CreateContestRequest;
import com.example.oj.service.TeacherService;
import com.example.oj.vo.ContestProblemVO;
import com.example.oj.vo.ContestVO;
import com.example.oj.vo.RankVO;
import com.example.oj.vo.SubmissionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teacher/contests")
public class TeacherContestController {
    private final TeacherService teacherService;

    @PostMapping
    public Result<ContestVO> create(@Valid @RequestBody CreateContestRequest request) {
        return Result.success(teacherService.createContest(request));
    }

    @GetMapping
    public Result<List<ContestVO>> list() {
        return Result.success(teacherService.listContests());
    }

    @GetMapping("/{contestId}")
    public Result<ContestVO> get(@PathVariable Long contestId) {
        return Result.success(teacherService.getContest(contestId));
    }

    @PutMapping("/{contestId}")
    public Result<ContestVO> update(@PathVariable Long contestId, @Valid @RequestBody CreateContestRequest request) {
        return Result.success(teacherService.updateContest(contestId, request));
    }

    @PostMapping("/{contestId}/problems")
    public Result<ContestProblemVO> addProblem(@PathVariable Long contestId, @Valid @RequestBody AddContestProblemRequest request) {
        return Result.success(teacherService.addContestProblem(contestId, request));
    }

    @DeleteMapping("/{contestId}/problems/{problemId}")
    public Result<Void> deleteProblem(@PathVariable Long contestId, @PathVariable Long problemId) {
        teacherService.deleteContestProblem(contestId, problemId);
        return Result.success(null);
    }

    @PostMapping("/{contestId}/publish")
    public Result<Void> publish(@PathVariable Long contestId) {
        teacherService.publishContest(contestId);
        return Result.success(null);
    }

    @GetMapping("/{contestId}/rank")
    public Result<List<RankVO>> rank(@PathVariable Long contestId) {
        return Result.success(teacherService.rank(contestId));
    }

    @GetMapping("/{contestId}/submissions")
    public Result<List<SubmissionVO>> submissions(@PathVariable Long contestId) {
        return Result.success(teacherService.submissions(contestId));
    }
}
