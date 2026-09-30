package com.example.oj.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminDashboardVO {
    private Long totalUsers;
    private Long teachers;
    private Long students;
    private Long disabledUsers;
    private Long onlineUsers;
    private Long totalVisits;
    private Long todayVisits;
    private long uptimeSeconds;
    private int processors;
    private long maxMemoryMb;
    private long usedMemoryMb;
    private double systemLoadAverage;
}
