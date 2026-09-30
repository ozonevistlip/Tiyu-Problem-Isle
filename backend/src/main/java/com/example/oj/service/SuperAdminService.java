package com.example.oj.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.oj.dto.AdminCreateUserRequest;
import com.example.oj.dto.AdminUpdateUserRequest;
import com.example.oj.dto.AnnouncementRequest;
import com.example.oj.vo.AdminDashboardVO;
import com.example.oj.vo.SessionVO;
import com.example.oj.vo.UserVO;
import com.example.oj.entity.Announcement;

import java.util.List;
import java.util.Map;

public interface SuperAdminService {
    AdminDashboardVO dashboard();
    IPage<UserVO> users(long page, long size, String keyword, String role, Integer status);
    UserVO user(Long id);
    UserVO createUser(AdminCreateUserRequest request);
    UserVO updateUser(Long id, AdminUpdateUserRequest request);
    void updateStatus(Long id, Integer status);
    void resetPassword(Long id, String password);
    void forceLogout(Long id);
    void deleteUser(Long id);
    List<SessionVO> onlineUsers();
    void terminateSession(String sessionId);
    Map<String, Object> settings();
    void updateRegistration(boolean enabled);
    List<Announcement> announcements();
    Announcement saveAnnouncement(Long id, AnnouncementRequest request);
    void deleteAnnouncement(Long id);
}
