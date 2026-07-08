package com.example.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.oj.entity.*;
import com.example.oj.judge.DockerJudgeRunner;
import com.example.oj.judge.JudgeResult;
import com.example.oj.mapper.*;
import com.example.oj.service.JudgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JudgeServiceImpl implements JudgeService {
    private final SubmissionMapper submissionMapper;
    private final SubmissionCaseMapper submissionCaseMapper;
    private final ProblemMapper problemMapper;
    private final TestcaseMapper testcaseMapper;
    private final ContestAnswerMapper contestAnswerMapper;
    private final ContestParticipantMapper contestParticipantMapper;
    private final DockerJudgeRunner dockerJudgeRunner;

    @Override
    @Transactional
    public void processSubmission(Long submissionId) {
        if (submissionMapper.markJudgingIfPending(submissionId) == 0) {
            return;
        }
        Submission submission = submissionMapper.selectById(submissionId);
        Problem problem = problemMapper.selectById(submission.getProblemId());
        List<Testcase> testcases = testcaseMapper.selectList(new LambdaQueryWrapper<Testcase>()
                .eq(Testcase::getProblemId, submission.getProblemId())
                .orderByAsc(Testcase::getSortOrder));
        JudgeResult result = dockerJudgeRunner.run(submission, problem, testcases);
        for (JudgeResult.JudgeCaseResult item : result.getCases()) {
            submissionCaseMapper.insert(SubmissionCase.builder()
                    .submissionId(submissionId)
                    .testcaseId(item.getTestcaseId())
                    .status(item.getStatus())
                    .timeUsedMs(item.getTimeUsedMs())
                    .memoryUsedKb(item.getMemoryUsedKb())
                    .inputPreview(item.getInputPreview())
                    .expectedOutputPreview(item.getExpectedOutputPreview())
                    .actualOutputPreview(item.getActualOutputPreview())
                    .errorMessage(item.getErrorMessage())
                    .build());
        }
        submission.setStatus(result.getStatus());
        submission.setScore(result.getScore());
        submission.setTimeUsedMs(result.getTimeUsedMs());
        submission.setMemoryUsedKb(result.getMemoryUsedKb());
        submission.setErrorMessage(result.getErrorMessage());
        submission.setJudgedAt(LocalDateTime.now());
        submissionMapper.updateById(submission);
        updateProblemStats(submission);
        if (submission.getContestId() != null) {
            updateContestStats(submission);
        }
    }

    private void updateProblemStats(Submission submission) {
        problemMapper.update(new LambdaUpdateWrapper<Problem>()
                .eq(Problem::getId, submission.getProblemId())
                .setSql("submit_count = submit_count + 1"));
        if ("ACCEPTED".equals(submission.getStatus())) {
            problemMapper.update(new LambdaUpdateWrapper<Problem>()
                    .eq(Problem::getId, submission.getProblemId())
                    .setSql("accepted_count = accepted_count + 1"));
        }
    }

    private void updateContestStats(Submission submission) {
        ContestAnswer answer = contestAnswerMapper.selectOne(new LambdaQueryWrapper<ContestAnswer>()
                .eq(ContestAnswer::getContestId, submission.getContestId())
                .eq(ContestAnswer::getProblemId, submission.getProblemId())
                .eq(ContestAnswer::getStudentId, submission.getUserId())
                .last("LIMIT 1"));
        if (answer == null) {
            answer = ContestAnswer.builder()
                    .contestId(submission.getContestId())
                    .problemId(submission.getProblemId())
                    .studentId(submission.getUserId())
                    .status("UNTRIED")
                    .bestScore(0)
                    .submitCount(0)
                    .hintShown(false)
                    .build();
            contestAnswerMapper.insert(answer);
        }
        LocalDateTime now = LocalDateTime.now();
        answer.setSubmitCount((answer.getSubmitCount() == null ? 0 : answer.getSubmitCount()) + 1);
        if (answer.getFirstSubmitAt() == null) {
            answer.setFirstSubmitAt(submission.getCreatedAt());
        }
        answer.setLastSubmitAt(now);
        int oldBest = answer.getBestScore() == null ? 0 : answer.getBestScore();
        int newScore = submission.getScore() == null ? 0 : submission.getScore();
        if ("ACCEPTED".equals(submission.getStatus())) {
            answer.setStatus("ACCEPTED");
            if (answer.getAcceptedAt() == null) {
                answer.setAcceptedAt(now);
            }
            answer.setFinalSubmissionId(submission.getId());
        } else if (newScore > 0 && !"ACCEPTED".equals(answer.getStatus())) {
            answer.setStatus("PARTIAL");
        } else if ("UNTRIED".equals(answer.getStatus())) {
            answer.setStatus("TRIED");
        }
        if (newScore >= oldBest) {
            answer.setBestScore(newScore);
            answer.setFinalSubmissionId(submission.getId());
        }
        contestAnswerMapper.updateById(answer);
        refreshParticipant(submission.getContestId(), submission.getUserId());
    }

    private void refreshParticipant(Long contestId, Long studentId) {
        List<ContestAnswer> answers = contestAnswerMapper.selectList(new LambdaQueryWrapper<ContestAnswer>()
                .eq(ContestAnswer::getContestId, contestId)
                .eq(ContestAnswer::getStudentId, studentId));
        int totalScore = answers.stream().mapToInt(a -> a.getBestScore() == null ? 0 : a.getBestScore()).sum();
        int acceptedCount = (int) answers.stream().filter(a -> "ACCEPTED".equals(a.getStatus())).count();
        int submitCount = answers.stream().mapToInt(a -> a.getSubmitCount() == null ? 0 : a.getSubmitCount()).sum();
        contestParticipantMapper.update(new LambdaUpdateWrapper<ContestParticipant>()
                .eq(ContestParticipant::getContestId, contestId)
                .eq(ContestParticipant::getStudentId, studentId)
                .set(ContestParticipant::getTotalScore, totalScore)
                .set(ContestParticipant::getAcceptedCount, acceptedCount)
                .set(ContestParticipant::getSubmitCount, submitCount)
                .set(ContestParticipant::getLastSubmitAt, LocalDateTime.now()));
    }
}
