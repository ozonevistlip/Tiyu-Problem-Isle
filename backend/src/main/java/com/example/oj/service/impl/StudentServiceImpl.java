package com.example.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.dto.SubmitCodeRequest;
import com.example.oj.entity.*;
import com.example.oj.mapper.*;
import com.example.oj.service.StudentService;
import com.example.oj.utils.UserContext;
import com.example.oj.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {
    private final ClassMemberMapper classMemberMapper;
    private final ContestMapper contestMapper;
    private final ContestProblemMapper contestProblemMapper;
    private final ContestAnswerMapper contestAnswerMapper;
    private final ProblemMapper problemMapper;
    private final ProblemHintMapper problemHintMapper;
    private final SubmissionMapper submissionMapper;
    private final ContestParticipantMapper contestParticipantMapper;
    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${oj.judge.queue-name:judge_queue}")
    private String judgeQueueName;

    @Override
    public List<ContestVO> listContests() {
        UserContext.requireStudent();
        List<Long> classIds = classMemberMapper.selectList(new LambdaQueryWrapper<ClassMember>()
                        .eq(ClassMember::getStudentId, UserContext.userId()))
                .stream().map(ClassMember::getClassId).toList();
        if (classIds.isEmpty()) {
            return List.of();
        }
        return contestMapper.selectList(new LambdaQueryWrapper<Contest>()
                        .in(Contest::getClassId, classIds)
                        .eq(Contest::getStatus, "PUBLISHED")
                        .orderByDesc(Contest::getStartTime))
                .stream().map(this::toContestVO).toList();
    }

    @Override
    public ContestVO getContest(Long contestId) {
        return toContestVO(requireVisibleContest(contestId));
    }

    @Override
    public List<ContestProblemVO> listProblems(Long contestId) {
        Contest contest = requireVisibleContest(contestId);
        List<ContestProblem> contestProblems = contestProblemMapper.selectList(new LambdaQueryWrapper<ContestProblem>()
                .eq(ContestProblem::getContestId, contestId)
                .orderByAsc(ContestProblem::getDisplayOrder));
        if (contestProblems.isEmpty()) {
            return List.of();
        }
        Map<Long, Problem> problemMap = problemMapper.selectBatchIds(
                contestProblems.stream().map(ContestProblem::getProblemId).toList()
        ).stream().collect(Collectors.toMap(Problem::getId, Function.identity()));
        return contestProblems.stream()
                .map(cp -> toContestProblemVO(contest, cp, problemMap.get(cp.getProblemId()), false))
                .toList();
    }

    @Override
    public ContestProblemVO getProblem(Long contestId, Long problemId) {
        Contest contest = requireVisibleContest(contestId);
        ContestProblem contestProblem = requireContestProblem(contestId, problemId);
        Problem problem = requireProblem(problemId);
        ContestAnswer answer = findOrCreateAnswer(contestId, problemId, UserContext.userId());
        LocalDateTime now = LocalDateTime.now();
        if (answer.getFirstOpenAt() == null) {
            answer.setFirstOpenAt(now);
        }
        if ("UNTRIED".equals(answer.getStatus())) {
            answer.setStatus("TRIED");
        }
        contestAnswerMapper.updateById(answer);
        return toContestProblemVO(contest, contestProblem, problem, true);
    }

    @Override
    @Transactional
    public HintView getContestProblemHint(Long contestId, Long problemId, Long studentId) {
        UserContext.requireStudent();
        if (!UserContext.userId().equals(studentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能查看其他学生的提示状态");
        }
        Contest contest = requireVisibleContest(contestId);
        ContestProblem contestProblem = requireContestProblem(contestId, problemId);
        ContestAnswer answer = findOrCreateAnswer(contestId, problemId, studentId);
        HintDecision decision = decideHint(contest, contestProblem, answer);
        if (!decision.canShow()) {
            return HintView.builder()
                    .canShowHint(false)
                    .unlockRemainSeconds(decision.remainSeconds())
                    .hintShown(Boolean.TRUE.equals(answer.getHintShown()))
                    .build();
        }
        ProblemHint hint = problemHintMapper.selectList(new LambdaQueryWrapper<ProblemHint>()
                        .eq(ProblemHint::getProblemId, problemId)
                        .orderByAsc(ProblemHint::getHintLevel))
                .stream().findFirst().orElse(null);
        if (!Boolean.TRUE.equals(answer.getHintShown())) {
            answer.setHintShown(true);
            answer.setHintFirstShownAt(LocalDateTime.now());
            contestAnswerMapper.updateById(answer);
        }
        return HintView.builder()
                .canShowHint(true)
                .unlockRemainSeconds(0L)
                .hintTitle(hint == null ? null : hint.getHintTitle())
                .hintContent(hint == null ? null : hint.getHintContent())
                .hintLevel(hint == null ? null : hint.getHintLevel())
                .hintShown(true)
                .build();
    }

    @Override
    @Transactional
    public SubmissionVO submit(Long contestId, Long problemId, SubmitCodeRequest request) {
        UserContext.requireStudent();
        Contest contest = requireVisibleContest(contestId);
        requireContestProblem(contestId, problemId);
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(contest.getStartTime())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "比赛尚未开始");
        }
        if (now.isAfter(contest.getEndTime()) && !Boolean.TRUE.equals(contest.getAllowSubmitAfterEnd())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "比赛已结束，不能提交");
        }
        if (!"cpp17".equalsIgnoreCase(request.getLanguage())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前仅支持 cpp17");
        }
        Submission submission = Submission.builder()
                .userId(UserContext.userId())
                .problemId(problemId)
                .contestId(contestId)
                .language("cpp17")
                .code(request.getCode())
                .status("PENDING")
                .score(0)
                .timeUsedMs(0)
                .memoryUsedKb(0)
                .build();
        submissionMapper.insert(submission);
        findOrCreateAnswer(contestId, problemId, UserContext.userId());
        contestParticipantMapper.update(new LambdaUpdateWrapper<ContestParticipant>()
                .eq(ContestParticipant::getContestId, contestId)
                .eq(ContestParticipant::getStudentId, UserContext.userId())
                .setSql("started_at = COALESCE(started_at, NOW())"));
        try {
            stringRedisTemplate.opsForList().leftPush(judgeQueueName, submission.getId().toString());
        } catch (RedisConnectionFailureException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "判题队列服务未启动，请联系老师或管理员");
        }
        return toSubmissionVO(submission);
    }

    @Override
    public List<SubmissionVO> submissions(Long contestId) {
        requireVisibleContest(contestId);
        return submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getContestId, contestId)
                        .eq(Submission::getUserId, UserContext.userId())
                        .orderByDesc(Submission::getId))
                .stream().map(this::toSubmissionVO).toList();
    }

    @Override
    public List<RankVO> rank(Long contestId) {
        Contest contest = requireVisibleContest(contestId);
        if (!Boolean.TRUE.equals(contest.getShowRank())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "排名暂未开放");
        }
        List<ContestParticipant> participants = contestParticipantMapper.selectList(new LambdaQueryWrapper<ContestParticipant>()
                .eq(ContestParticipant::getContestId, contestId));
        Map<Long, User> users = participants.isEmpty() ? Map.of() : userMapper.selectBatchIds(
                participants.stream().map(ContestParticipant::getStudentId).toList()
        ).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return participants.stream()
                .sorted(Comparator.comparing(ContestParticipant::getTotalScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ContestParticipant::getLastSubmitAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(p -> RankVO.builder()
                        .studentId(p.getStudentId())
                        .studentName(users.containsKey(p.getStudentId()) ? users.get(p.getStudentId()).getRealName() : null)
                        .totalScore(p.getTotalScore())
                        .acceptedCount(p.getAcceptedCount())
                        .submitCount(p.getSubmitCount())
                        .lastSubmitAt(p.getLastSubmitAt())
                        .build())
                .toList();
    }

    private Contest requireVisibleContest(Long contestId) {
        UserContext.requireStudent();
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "比赛不存在");
        }
        if (!"PUBLISHED".equals(contest.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "比赛不可见");
        }
        Long member = classMemberMapper.selectCount(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, contest.getClassId())
                .eq(ClassMember::getStudentId, UserContext.userId()));
        if (member == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "学生不属于该比赛班级");
        }
        return contest;
    }

    private ContestProblem requireContestProblem(Long contestId, Long problemId) {
        ContestProblem contestProblem = contestProblemMapper.selectOne(new LambdaQueryWrapper<ContestProblem>()
                .eq(ContestProblem::getContestId, contestId)
                .eq(ContestProblem::getProblemId, problemId)
                .last("LIMIT 1"));
        if (contestProblem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "题目不属于该比赛");
        }
        return contestProblem;
    }

    private Problem requireProblem(Long problemId) {
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "题目不存在");
        }
        return problem;
    }

    private ContestAnswer findOrCreateAnswer(Long contestId, Long problemId, Long studentId) {
        ContestAnswer answer = contestAnswerMapper.selectOne(new LambdaQueryWrapper<ContestAnswer>()
                .eq(ContestAnswer::getContestId, contestId)
                .eq(ContestAnswer::getProblemId, problemId)
                .eq(ContestAnswer::getStudentId, studentId)
                .last("LIMIT 1"));
        if (answer != null) {
            return answer;
        }
        answer = ContestAnswer.builder()
                .contestId(contestId)
                .problemId(problemId)
                .studentId(studentId)
                .status("UNTRIED")
                .bestScore(0)
                .submitCount(0)
                .hintShown(false)
                .build();
        contestAnswerMapper.insert(answer);
        return answer;
    }

    private HintDecision decideHint(Contest contest, ContestProblem contestProblem, ContestAnswer answer) {
        LocalDateTime now = LocalDateTime.now();
        boolean accepted = "ACCEPTED".equals(answer.getStatus());
        if (accepted) {
            return new HintDecision(Boolean.TRUE.equals(contestProblem.getShowHintAfterAc()), 0L);
        }
        if (!now.isBefore(contest.getEndTime())) {
            return new HintDecision(Boolean.TRUE.equals(contestProblem.getShowHintAfterContest()), 0L);
        }
        LocalDateTime unlockAt = contest.getStartTime().plusMinutes(contestProblem.getHintUnlockMinutes());
        boolean unlockedByTime = !now.isBefore(unlockAt);
        long remainSeconds = unlockedByTime ? 0L : Math.max(0L, Duration.between(now, unlockAt).getSeconds());
        return new HintDecision(unlockedByTime, remainSeconds);
    }

    private ContestProblemVO toContestProblemVO(Contest contest, ContestProblem cp, Problem problem, boolean includeDetail) {
        ContestAnswer answer = contestAnswerMapper.selectOne(new LambdaQueryWrapper<ContestAnswer>()
                .eq(ContestAnswer::getContestId, contest.getId())
                .eq(ContestAnswer::getProblemId, cp.getProblemId())
                .eq(ContestAnswer::getStudentId, UserContext.userId())
                .last("LIMIT 1"));
        if (answer == null) {
            answer = ContestAnswer.builder().status("UNTRIED").bestScore(0).submitCount(0).hintShown(false).build();
        }
        HintDecision decision = decideHint(contest, cp, answer);
        return ContestProblemVO.builder()
                .problemId(problem.getId())
                .title(problem.getTitle())
                .description(includeDetail ? problem.getDescription() : null)
                .inputFormat(includeDetail ? problem.getInputFormat() : null)
                .outputFormat(includeDetail ? problem.getOutputFormat() : null)
                .sampleInput(includeDetail ? problem.getSampleInput() : null)
                .sampleOutput(includeDetail ? problem.getSampleOutput() : null)
                .difficulty(problem.getDifficulty())
                .displayOrder(cp.getDisplayOrder())
                .score(cp.getScore())
                .hintUnlockMinutes(cp.getHintUnlockMinutes())
                .status(answer.getStatus())
                .bestScore(answer.getBestScore())
                .submitCount(answer.getSubmitCount())
                .canShowHint(decision.canShow())
                .hintUnlockRemainSeconds(decision.remainSeconds())
                .build();
    }

    private ContestVO toContestVO(Contest contest) {
        return ContestVO.builder()
                .id(contest.getId())
                .teacherId(contest.getTeacherId())
                .classId(contest.getClassId())
                .title(contest.getTitle())
                .description(contest.getDescription())
                .startTime(contest.getStartTime())
                .endTime(contest.getEndTime())
                .status(contest.getStatus())
                .showRank(contest.getShowRank())
                .allowSubmitAfterEnd(contest.getAllowSubmitAfterEnd())
                .build();
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

    private record HintDecision(boolean canShow, long remainSeconds) {
    }
}
