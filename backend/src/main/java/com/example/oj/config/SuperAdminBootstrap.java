package com.example.oj.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.oj.entity.SiteSetting;
import com.example.oj.entity.User;
import com.example.oj.mapper.SiteSettingMapper;
import com.example.oj.mapper.UserMapper;
import com.example.oj.utils.PasswordUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminBootstrap implements ApplicationRunner {
    private final UserMapper userMapper;
    private final SiteSettingMapper siteSettingMapper;

    @Value("${oj.super-admin.enabled:true}")
    private boolean enabled;
    @Value("${oj.super-admin.username:superadmin}")
    private String username;
    @Value("${oj.super-admin.password:CppKid@Admin123}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        initializeSettings();
        if (!enabled || userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getRole, "SUPER_ADMIN")) > 0) {
            return;
        }
        if (password == null || password.length() < 8) {
            log.error("Cannot bootstrap SUPER_ADMIN: SUPER_ADMIN_PASSWORD must contain at least 8 characters");
            return;
        }
        User sameName = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username).last("LIMIT 1"));
        if (sameName != null) {
            log.error("Cannot bootstrap SUPER_ADMIN: username '{}' is already used", username);
            return;
        }
        User admin = User.builder()
                .username(username)
                .passwordHash(PasswordUtils.hash(password))
                .realName("超级管理员")
                .nickname("系统管理员")
                .role("SUPER_ADMIN")
                .status(1)
                .build();
        userMapper.insert(admin);
        log.warn("Bootstrapped SUPER_ADMIN account '{}'. Change the initial password/configuration before production use.", username);
    }

    private void initializeSettings() {
        if (siteSettingMapper.selectById("registration_enabled") == null) {
            siteSettingMapper.insert(SiteSetting.builder()
                    .settingKey("registration_enabled")
                    .settingValue("true")
                    .build());
        }
    }
}
