package org.example.aispringboot.DTO.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 数据看板总览DTO，包含系统概览、情绪趋势、咨询统计、用户活跃度
 */
@Data
public class AnalyticsOverviewDTO {
    // 顶部系统概览卡片
    private SystemOverview systemOverview;

    // 情绪趋势
    private List<TrendItem> emotionTrend;

    // 咨询会话统计
    private ConsultationStats consultationStats;

    // 用户活跃度趋势
    private List<ActivityItem> userActivity;

    // 系统概览
    @Data
    public static class SystemOverview {
        // 总用户数
        private Long totalUsers;
        // 活跃用户数
        private Long activeUsers;
        // 情绪日志总数
        private Long totalDiaries;
        // 今日新增日记
        private Long todayNewDiaries;
        // 咨询会话总数
        private Long totalSessions;
        // 今日新增会话
        private Long todayNewSessions;
        // 平均情绪分
        private BigDecimal avgMoodScore;
    }

    // 情绪趋势项
    @Data
    public static class TrendItem {
        // 日期
        private String date;
        // 平均情绪分
        private BigDecimal avgMoodScore;
        // 记录数量
        private Long recordCount;
    }

    // 咨询统计
    @Data
    public static class ConsultationStats {
        // 总会话数
        private Long totalSessions;
        // 平均时长（分钟）
        private BigDecimal avgDurationMinutes;
        // 每日趋势
        private List<DailyItem> dailyTrend;
    }

    // 咨询每日项
    @Data
    public static class DailyItem {
        // 日期
        private String date;
        // 会话数量
        private Long sessionCount;
        // 参与用户数
        private Long userCount;
    }

    // 用户活跃度项
    @Data
    public static class ActivityItem {
        // 日期
        private String date;
        // 活跃用户
        private Long activeUsers;
        // 新增用户
        private Long newUsers;
        // 日记用户
        private Long diaryUsers;
        // 咨询用户
        private Long consultationUsers;
    }
}
