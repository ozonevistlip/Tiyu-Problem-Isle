package com.example.oj.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.oj.common.Result;
import com.example.oj.dto.VisitRequest;
import com.example.oj.entity.Announcement;
import com.example.oj.entity.SiteSetting;
import com.example.oj.entity.SiteVisit;
import com.example.oj.mapper.AnnouncementMapper;
import com.example.oj.mapper.SiteSettingMapper;
import com.example.oj.mapper.SiteVisitMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public")
public class PublicController {
    private final SiteSettingMapper siteSettingMapper;
    private final SiteVisitMapper siteVisitMapper;
    private final AnnouncementMapper announcementMapper;

    @GetMapping("/config")
    public Result<Map<String, Object>> config() {
        SiteSetting setting = siteSettingMapper.selectById("registration_enabled");
        return Result.success(Map.of("registrationEnabled",
                setting == null || Boolean.parseBoolean(setting.getSettingValue())));
    }

    @GetMapping("/announcements")
    public Result<List<Announcement>> announcements() {
        return Result.success(announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getStatus, 1)
                .orderByDesc(Announcement::getPublishedAt)));
    }

    @PostMapping("/visits")
    public Result<Void> visit(@Valid @RequestBody VisitRequest request, HttpServletRequest servletRequest) {
        long recent = siteVisitMapper.selectCount(new LambdaQueryWrapper<SiteVisit>()
                .eq(SiteVisit::getVisitorKey, request.getVisitorKey())
                .eq(SiteVisit::getPath, request.getPath())
                .ge(SiteVisit::getCreatedAt, LocalDateTime.now().minusMinutes(30)));
        if (recent == 0) {
            siteVisitMapper.insert(SiteVisit.builder()
                    .visitorKey(request.getVisitorKey())
                    .path(request.getPath())
                    .ipAddress(clientIp(servletRequest))
                    .build());
        }
        return Result.success(null);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
        return ip.length() <= 64 ? ip : ip.substring(0, 64);
    }
}
