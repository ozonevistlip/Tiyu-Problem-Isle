package com.example.oj.judge;

import com.example.oj.service.JudgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class JudgeWorker {
    private final StringRedisTemplate stringRedisTemplate;
    private final JudgeService judgeService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean redisWarningLogged = new AtomicBoolean(false);

    @Value("${oj.judge.queue-name:judge_queue}")
    private String judgeQueueName;

    @Scheduled(fixedDelay = 1000)
    public void poll() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            String value = stringRedisTemplate.opsForList().rightPop(judgeQueueName);
            redisWarningLogged.set(false);
            if (value == null) {
                return;
            }
            judgeService.processSubmission(Long.valueOf(value));
        } catch (RedisConnectionFailureException e) {
            if (redisWarningLogged.compareAndSet(false, true)) {
                log.warn("Judge worker paused because Redis is unavailable");
            }
        } catch (Exception e) {
            log.error("Judge worker failed", e);
        } finally {
            running.set(false);
        }
    }
}
