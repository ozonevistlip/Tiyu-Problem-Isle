package com.example.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.entity.Contest;
import com.example.oj.entity.Submission;
import com.example.oj.entity.SubmissionCase;
import com.example.oj.mapper.ContestMapper;
import com.example.oj.mapper.SubmissionCaseMapper;
import com.example.oj.mapper.SubmissionMapper;
import com.example.oj.service.SubmissionService;
import com.example.oj.utils.UserContext;
import com.example.oj.vo.SubmissionCaseVO;
import com.example.oj.vo.SubmissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {
    private final SubmissionMapper submissionMapper;
    private final SubmissionCaseMapper submissionCaseMapper;
    private final ContestMapper contestMapper;

    @Override
    public SubmissionVO getSubmission(Long submissionId) {
        Submission submission = requireReadableSubmission(submissionId);
        return toSubmissionVO(submission);
    }

    @Override
    public List<SubmissionCaseVO> listCases(Long submissionId) {
        requireReadableSubmission(submissionId);
        return submissionCaseMapper.selectList(new LambdaQueryWrapper<SubmissionCase>()
                        .eq(SubmissionCase::getSubmissionId, submissionId)
                        .orderByAsc(SubmissionCase::getId))
                .stream().map(this::toCaseVO).toList();
    }

    private Submission requireReadableSubmission(Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "提交不存在");
        }
        if ("student".equals(UserContext.role())) {
            if (!UserContext.userId().equals(submission.getUserId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "不能查看其他学生的提交");
            }
            return submission;
        }
        if ("teacher".equals(UserContext.role()) && submission.getContestId() != null) {
            Contest contest = contestMapper.selectById(submission.getContestId());
            if (contest != null && UserContext.userId().equals(contest.getTeacherId())) {
                return submission;
            }
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该提交");
    }

    private SubmissionVO toSubmissionVO(Submission submission) {
        return SubmissionVO.builder()
                .id(submission.getId())
                .userId(submission.getUserId())
                .problemId(submission.getProblemId())
                .contestId(submission.getContestId())
                .language(submission.getLanguage())
                .status(submission.getStatus())
                .score(submission.getScore())
                .timeUsedMs(submission.getTimeUsedMs())
                .memoryUsedKb(submission.getMemoryUsedKb())
                .errorMessage(submission.getErrorMessage())
                .judgedAt(submission.getJudgedAt())
                .createdAt(submission.getCreatedAt())
                .build();
    }

    private SubmissionCaseVO toCaseVO(SubmissionCase item) {
        return SubmissionCaseVO.builder()
                .id(item.getId())
                .testcaseId(item.getTestcaseId())
                .status(item.getStatus())
                .timeUsedMs(item.getTimeUsedMs())
                .memoryUsedKb(item.getMemoryUsedKb())
                .inputPreview(item.getInputPreview())
                .expectedOutputPreview(item.getExpectedOutputPreview())
                .actualOutputPreview(item.getActualOutputPreview())
                .errorMessage(item.getErrorMessage())
                .build();
    }
}
