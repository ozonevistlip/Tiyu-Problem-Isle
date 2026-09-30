package com.example.oj.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.oj.common.Result;
import com.example.oj.dto.AdminCreateUserRequest;
import com.example.oj.dto.AdminUpdateUserRequest;
import com.example.oj.dto.AnnouncementRequest;
import com.example.oj.dto.ResetPasswordRequest;
import com.example.oj.dto.UserStatusRequest;
import com.example.oj.entity.Announcement;
import com.example.oj.service.SuperAdminService;
import com.example.oj.vo.AdminDashboardVO;
import com.example.oj.vo.SessionVO;
import com.example.oj.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/super-admin")
public class SuperAdminController {
    private final SuperAdminService service;

    @GetMapping("/dashboard")
    public Result<AdminDashboardVO> dashboard() {
        return Result.success(service.dashboard());
    }

    @GetMapping("/users")
    public Result<IPage<UserVO>> users(@RequestParam(defaultValue = "1") long page,
                                       @RequestParam(defaultValue = "20") long size,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) String role,
                                       @RequestParam(required = false) Integer status) {
        return Result.success(service.users(page, size, keyword, role, status));
    }

    @GetMapping("/users/{id}")
    public Result<UserVO> user(@PathVariable Long id) {
        return Result.success(service.user(id));
    }

    @PostMapping("/users")
    public Result<UserVO> createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        return Result.success(service.createUser(request));
    }

    @PutMapping("/users/{id}")
    public Result<UserVO> updateUser(@PathVariable Long id, @Valid @RequestBody AdminUpdateUserRequest request) {
        return Result.success(service.updateUser(id, request));
    }

    @PutMapping("/users/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        service.updateStatus(id, request.getStatus());
        return Result.success(null);
    }

    @PutMapping("/users/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        service.resetPassword(id, request.getPassword());
        return Result.success(null);
    }

    @PostMapping("/users/{id}/force-logout")
    public Result<Void> forceLogout(@PathVariable Long id) {
        service.forceLogout(id);
        return Result.success(null);
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        service.deleteUser(id);
        return Result.success(null);
    }

    @GetMapping("/online-users")
    public Result<List<SessionVO>> onlineUsers() {
        return Result.success(service.onlineUsers());
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> terminateSession(@PathVariable String sessionId) {
        service.terminateSession(sessionId);
        return Result.success(null);
    }

    @GetMapping("/settings")
    public Result<Map<String, Object>> settings() {
        return Result.success(service.settings());
    }

    @PutMapping("/settings/registration")
    public Result<Void> updateRegistration(@RequestBody Map<String, Boolean> body) {
        service.updateRegistration(Boolean.TRUE.equals(body.get("enabled")));
        return Result.success(null);
    }

    @GetMapping("/announcements")
    public Result<List<Announcement>> announcements() {
        return Result.success(service.announcements());
    }

    @PostMapping("/announcements")
    public Result<Announcement> createAnnouncement(@Valid @RequestBody AnnouncementRequest request) {
        return Result.success(service.saveAnnouncement(null, request));
    }

    @PutMapping("/announcements/{id}")
    public Result<Announcement> updateAnnouncement(@PathVariable Long id,
                                                    @Valid @RequestBody AnnouncementRequest request) {
        return Result.success(service.saveAnnouncement(id, request));
    }

    @DeleteMapping("/announcements/{id}")
    public Result<Void> deleteAnnouncement(@PathVariable Long id) {
        service.deleteAnnouncement(id);
        return Result.success(null);
    }
}
