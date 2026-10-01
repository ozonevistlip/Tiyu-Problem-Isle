package com.example.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.dto.AdminCreateUserRequest;
import com.example.oj.dto.AdminUpdateUserRequest;
import com.example.oj.dto.AnnouncementRequest;
import com.example.oj.entity.Announcement;
import com.example.oj.entity.ClassGroup;
import com.example.oj.entity.Contest;
import com.example.oj.entity.Problem;
import com.example.oj.entity.SiteSetting;
import com.example.oj.entity.SiteVisit;
import com.example.oj.entity.User;
import com.example.oj.entity.UserSession;
import com.example.oj.mapper.AnnouncementMapper;
import com.example.oj.mapper.ClassGroupMapper;
import com.example.oj.mapper.ContestMapper;
import com.example.oj.mapper.ProblemMapper;
import com.example.oj.mapper.SiteSettingMapper;
import com.example.oj.mapper.SiteVisitMapper;
import com.example.oj.mapper.UserMapper;
import com.example.oj.mapper.UserSessionMapper;
import com.example.oj.service.SuperAdminService;
import com.example.oj.utils.PasswordUtils;
import com.example.oj.vo.AdminDashboardVO;
import com.example.oj.vo.SessionVO;
import com.example.oj.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.management.ManagementFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SuperAdminServiceImpl implements SuperAdminService {
    private final UserMapper userMapper;
    private final UserSessionMapper userSessionMapper;
    private final SiteSettingMapper siteSettingMapper;
    private final SiteVisitMapper siteVisitMapper;
    private final AnnouncementMapper announcementMapper;
    private final ClassGroupMapper classGroupMapper;
    private final ContestMapper contestMapper;
    private final ProblemMapper problemMapper;

    @Override
    public AdminDashboardVO dashboard() {
        Runtime runtime = Runtime.getRuntime();
        LocalDateTime onlineSince = LocalDateTime.now().minusMinutes(15);
        long onlineUsers = userSessionMapper.selectList(new LambdaQueryWrapper<UserSession>()
                .eq(UserSession::getStatus, "ACTIVE")
                .gt(UserSession::getExpiresAt, LocalDateTime.now())
                .ge(UserSession::getLastActiveAt, onlineSince)).stream()
                .map(UserSession::getUserId).distinct().count();
        return AdminDashboardVO.builder()
                .totalUsers(userMapper.selectCount(null))
                .teachers(countUsers("teacher", null))
                .students(countUsers("student", null))
                .disabledUsers(countUsers(null, 0))
                .onlineUsers(onlineUsers)
                .totalVisits(siteVisitMapper.selectCount(null))
                .todayVisits(siteVisitMapper.selectCount(new LambdaQueryWrapper<SiteVisit>()
                        .ge(SiteVisit::getCreatedAt, LocalDate.now().atStartOfDay())))
                .uptimeSeconds(ManagementFactory.getRuntimeMXBean().getUptime() / 1000)
                .processors(runtime.availableProcessors())
                .maxMemoryMb(runtime.maxMemory() / 1024 / 1024)
                .usedMemoryMb((runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024)
                .systemLoadAverage(ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage())
                .build();
    }

    @Override
    public IPage<UserVO> users(long page, long size, String keyword, String role, Integer status) {
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<User>()
                .eq(role != null && !role.isBlank(), User::getRole, role)
                .eq(status != null, User::getStatus, status)
                .and(keyword != null && !keyword.isBlank(), q -> q.like(User::getUsername, keyword)
                        .or().like(User::getRealName, keyword).or().like(User::getNickname, keyword))
                .orderByDesc(User::getCreatedAt);
        Page<User> source = userMapper.selectPage(new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 100)), query);
        Page<UserVO> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setRecords(source.getRecords().stream().map(AuthServiceImpl::toUserVO).toList());
        return result;
    }

    @Override
    public UserVO user(Long id) {
        return AuthServiceImpl.toUserVO(requireUser(id));
    }

    @Override
    public UserVO createUser(AdminCreateUserRequest request) {
        if (!"teacher".equals(request.getRole())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "超级管理员只能创建老师账号");
        }
        ensureUsernameAvailable(request.getUsername(), null);
        User user = User.builder()
                .username(request.getUsername().trim())
                .passwordHash(PasswordUtils.hash(request.getPassword()))
                .role(request.getRole())
                .realName(request.getRealName())
                .nickname(request.getNickname())
                .status(1)
                .build();
        userMapper.insert(user);
        return AuthServiceImpl.toUserVO(user);
    }

    @Override
    public UserVO updateUser(Long id, AdminUpdateUserRequest request) {
        User user = requireManagedUser(id);
        if (request.getRole() != null && !request.getRole().equals(user.getRole())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能通过编辑账号改变角色");
        }
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            ensureUsernameAvailable(request.getUsername(), id);
            user.setUsername(request.getUsername().trim());
        }
        if (request.getRealName() != null) user.setRealName(request.getRealName());
        if (request.getNickname() != null) user.setNickname(request.getNickname());
        if (request.getRole() != null) user.setRole(request.getRole());
        userMapper.updateById(user);
        return AuthServiceImpl.toUserVO(user);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        User user = requireManagedUser(id);
        user.setStatus(status);
        userMapper.updateById(user);
        if (status == 0) forceLogout(id);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String password) {
        User user = requireManagedUser(id);
        user.setPasswordHash(PasswordUtils.hash(password));
        userMapper.updateById(user);
        forceLogout(id);
    }

    @Override
    public void forceLogout(Long id) {
        requireManagedUser(id);
        List<UserSession> sessions = userSessionMapper.selectList(new LambdaQueryWrapper<UserSession>()
                .eq(UserSession::getUserId, id).eq(UserSession::getStatus, "ACTIVE"));
        sessions.forEach(session -> {
            session.setStatus("FORCED_OUT");
            userSessionMapper.updateById(session);
        });
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = requireManagedUser(id);
        if ("teacher".equals(user.getRole())) {
            if (classGroupMapper.selectCount(new LambdaQueryWrapper<ClassGroup>()
                    .eq(ClassGroup::getTeacherId, id)) > 0
                    || userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getCreatedBy, id)) > 0
                    || contestMapper.selectCount(new LambdaQueryWrapper<Contest>()
                    .eq(Contest::getTeacherId, id)) > 0
                    || problemMapper.selectCount(new LambdaQueryWrapper<Problem>()
                    .eq(Problem::getCreatedBy, id)) > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "该老师仍有关联的班级、学生、比赛或题目，请先处理关联数据");
            }
        }
        userSessionMapper.delete(new LambdaQueryWrapper<UserSession>().eq(UserSession::getUserId, id));
        userMapper.deleteById(id);
    }

    @Override
    public List<SessionVO> onlineUsers() {
        List<UserSession> sessions = userSessionMapper.selectList(new LambdaQueryWrapper<UserSession>()
                .eq(UserSession::getStatus, "ACTIVE")
                .gt(UserSession::getExpiresAt, LocalDateTime.now())
                .ge(UserSession::getLastActiveAt, LocalDateTime.now().minusMinutes(15))
                .orderByDesc(UserSession::getLastActiveAt));
        if (sessions.isEmpty()) return Collections.emptyList();
        Map<Long, User> users = userMapper.selectBatchIds(sessions.stream().map(UserSession::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return sessions.stream().map(session -> {
            User user = users.get(session.getUserId());
            return SessionVO.builder().id(session.getId()).userId(session.getUserId())
                    .username(user == null ? "已删除用户" : user.getUsername())
                    .realName(user == null ? null : user.getRealName())
                    .role(user == null ? null : user.getRole())
                    .ipAddress(session.getIpAddress()).userAgent(session.getUserAgent())
                    .loginAt(session.getCreatedAt()).lastActiveAt(session.getLastActiveAt()).build();
        }).toList();
    }

    @Override
    public void terminateSession(String sessionId) {
        UserSession session = userSessionMapper.selectById(sessionId);
        if (session == null) throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        User user = requireUser(session.getUserId());
        if ("SUPER_ADMIN".equals(user.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能在此强制超级管理员下线");
        }
        session.setStatus("FORCED_OUT");
        userSessionMapper.updateById(session);
    }

    @Override
    public Map<String, Object> settings() {
        SiteSetting setting = siteSettingMapper.selectById("registration_enabled");
        return Map.of("registrationEnabled", setting != null && Boolean.parseBoolean(setting.getSettingValue()));
    }

    @Override
    public void updateRegistration(boolean enabled) {
        SiteSetting setting = siteSettingMapper.selectById("registration_enabled");
        if (setting == null) {
            siteSettingMapper.insert(SiteSetting.builder().settingKey("registration_enabled")
                    .settingValue(String.valueOf(enabled)).build());
        } else {
            setting.setSettingValue(String.valueOf(enabled));
            siteSettingMapper.updateById(setting);
        }
    }

    @Override
    public List<Announcement> announcements() {
        return announcementMapper.selectList(new LambdaQueryWrapper<Announcement>().orderByDesc(Announcement::getCreatedAt));
    }

    @Override
    public Announcement saveAnnouncement(Long id, AnnouncementRequest request) {
        Announcement announcement = id == null ? new Announcement() : announcementMapper.selectById(id);
        if (announcement == null) throw new BusinessException(ErrorCode.NOT_FOUND, "公告不存在");
        announcement.setTitle(request.getTitle());
        announcement.setContent(request.getContent());
        int status = request.getStatus() == null ? 0 : request.getStatus();
        announcement.setStatus(status);
        if (status == 1 && announcement.getPublishedAt() == null) announcement.setPublishedAt(LocalDateTime.now());
        if (id == null) {
            announcement.setCreatedBy(com.example.oj.utils.UserContext.userId());
            announcementMapper.insert(announcement);
        } else {
            announcementMapper.updateById(announcement);
        }
        return announcement;
    }

    @Override
    public void deleteAnnouncement(Long id) {
        if (announcementMapper.deleteById(id) == 0) throw new BusinessException(ErrorCode.NOT_FOUND, "公告不存在");
    }

    private long countUsers(String role, Integer status) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(role != null, User::getRole, role).eq(status != null, User::getStatus, status));
    }

    private User requireUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        return user;
    }

    private User requireManagedUser(Long id) {
        User user = requireUser(id);
        if (!"teacher".equals(user.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "该入口只能管理老师账号");
        }
        return user;
    }

    private void ensureUsernameAvailable(String username, Long exceptId) {
        User exists = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username.trim()).last("LIMIT 1"));
        if (exists != null && !exists.getId().equals(exceptId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号已存在");
        }
    }
}
