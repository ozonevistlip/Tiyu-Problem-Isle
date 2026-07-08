package com.example.oj.controller;

import com.example.oj.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {
    @GetMapping("/api/health")
    public Result<Map<String, Object>> health() {
        return Result.success(Map.of("ok", true, "service", "cppkid-oj-backend"));
    }
}
