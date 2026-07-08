package com.example.oj.judge;

import com.example.oj.entity.Problem;
import com.example.oj.entity.Submission;
import com.example.oj.entity.Testcase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class DockerJudgeRunner {
    private final OutputComparator outputComparator;

    @Value("${oj.judge.work-dir:./judge-work}")
    private String workDir;
    @Value("${oj.judge.docker-image:gcc:13}")
    private String dockerImage;
    @Value("${oj.judge.time-limit-ms:2000}")
    private int defaultTimeLimitMs;
    @Value("${oj.judge.memory-limit-mb:128}")
    private int defaultMemoryLimitMb;

    public JudgeResult run(Submission submission, Problem problem, List<Testcase> testcases) {
        if (testcases == null || testcases.isEmpty()) {
            return JudgeResult.builder()
                    .status("SYSTEM_ERROR")
                    .score(0)
                    .timeUsedMs(0)
                    .memoryUsedKb(0)
                    .errorMessage("题目未配置测试点，无法判题，请联系老师补充测试点")
                    .cases(List.of())
                    .build();
        }

        Path dir = Path.of(workDir, "sub-" + submission.getId() + "-" + UUID.randomUUID());
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("Main.cpp"), submission.getCode(), StandardCharsets.UTF_8);
            ProcessResult compile = runProcess(Duration.ofSeconds(20), List.of(
                    "docker", "run", "--rm", "--network", "none",
                    "--memory=" + memoryMb(problem) + "m", "--cpus=1",
                    "-v", dir.toAbsolutePath() + ":/work", "-w", "/work", dockerImage,
                    "g++", "Main.cpp", "-std=c++17", "-O2", "-DONLINE_JUDGE", "-o", "Main"
            ));
            if (compile.timedOut() || compile.exitCode() != 0) {
                String compileMessage = truncate(compile.stderr() + compile.stdout(), 2000);
                if (isDockerUnavailable(compileMessage)) {
                    return JudgeResult.builder()
                            .status("SYSTEM_ERROR")
                            .score(0)
                            .timeUsedMs(0)
                            .memoryUsedKb(0)
                            .errorMessage("判题环境 Docker 未启动或不可用，请联系老师或管理员")
                            .cases(List.of())
                            .build();
                }
                return JudgeResult.builder()
                        .status("COMPILE_ERROR")
                        .score(0)
                        .timeUsedMs(0)
                        .memoryUsedKb(0)
                        .errorMessage(compileMessage)
                        .cases(List.of())
                        .build();
            }

            List<JudgeResult.JudgeCaseResult> caseResults = new ArrayList<>();
            int totalScore = 0;
            int maxTime = 0;
            String finalStatus = "ACCEPTED";
            for (Testcase testcase : testcases.stream().sorted(Comparator.comparing(Testcase::getSortOrder)).toList()) {
                String inputName = "input-" + testcase.getId() + ".txt";
                Files.writeString(dir.resolve(inputName), testcase.getInputData(), StandardCharsets.UTF_8);
                long start = System.nanoTime();
                ProcessResult run = runProcess(Duration.ofMillis(timeLimitMs(problem) + 1000L), List.of(
                        "docker", "run", "--rm", "--network", "none",
                        "--memory=" + memoryMb(problem) + "m", "--cpus=1",
                        "-v", dir.toAbsolutePath() + ":/work", "-w", "/work", dockerImage,
                        "sh", "-lc", "timeout " + Math.max(1, timeLimitMs(problem) / 1000) + "s ./Main < " + inputName
                ));
                int usedMs = (int) TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                maxTime = Math.max(maxTime, usedMs);
                String status;
                String error = null;
                if (run.timedOut() || run.exitCode() == 124) {
                    status = "TIME_LIMIT";
                    error = "time limit exceeded";
                } else if (run.exitCode() != 0) {
                    status = "RUNTIME_ERROR";
                    error = truncate(run.stderr(), 1000);
                } else if (!outputComparator.matches(testcase.getOutputData(), run.stdout(), problem.getCompareMode())) {
                    status = "WRONG_ANSWER";
                } else {
                    status = "ACCEPTED";
                    totalScore += testcase.getScore() == null ? 0 : testcase.getScore();
                }
                if (!"ACCEPTED".equals(status) && "ACCEPTED".equals(finalStatus)) {
                    finalStatus = status;
                }
                caseResults.add(JudgeResult.JudgeCaseResult.builder()
                        .testcaseId(testcase.getId())
                        .status(status)
                        .score("ACCEPTED".equals(status) ? testcase.getScore() : 0)
                        .timeUsedMs(usedMs)
                        .memoryUsedKb(0)
                        .inputPreview(truncate(testcase.getInputData(), 500))
                        .expectedOutputPreview(truncate(testcase.getOutputData(), 500))
                        .actualOutputPreview(truncate(run.stdout(), 500))
                        .errorMessage(error)
                        .build());
            }
            return JudgeResult.builder()
                    .status(finalStatus)
                    .score(totalScore)
                    .timeUsedMs(maxTime)
                    .memoryUsedKb(0)
                    .cases(caseResults)
                    .build();
        } catch (Exception e) {
            return JudgeResult.builder()
                    .status("SYSTEM_ERROR")
                    .score(0)
                    .timeUsedMs(0)
                    .memoryUsedKb(0)
                    .errorMessage(e.getMessage())
                    .cases(List.of())
                    .build();
        } finally {
            deleteQuietly(dir);
        }
    }

    private ProcessResult runProcess(Duration timeout, List<String> command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(false).start();
        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            return new ProcessResult(-1, "", "process timeout", true);
        }
        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        return new ProcessResult(process.exitValue(), stdout, stderr, false);
    }

    private int timeLimitMs(Problem problem) {
        return problem.getTimeLimitMs() == null ? defaultTimeLimitMs : problem.getTimeLimitMs();
    }

    private int memoryMb(Problem problem) {
        return problem.getMemoryLimitMb() == null ? defaultMemoryLimitMb : problem.getMemoryLimitMb();
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private boolean isDockerUnavailable(String message) {
        if (message == null) {
            return false;
        }
        String value = message.toLowerCase();
        return value.contains("docker daemon is not running")
                || value.contains("cannot connect to the docker daemon")
                || value.contains("error during connect")
                || value.contains("dockerdesktoplinuxengine")
                || value.contains("docker_engine")
                || value.contains("containers/create");
    }

    private void deleteQuietly(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    private record ProcessResult(int exitCode, String stdout, String stderr, boolean timedOut) {
    }
}
