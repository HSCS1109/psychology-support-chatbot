package org.example.aispringboot.service;

import org.example.aispringboot.DTO.response.AnalyticsOverviewDTO;
import org.example.aispringboot.mapper.AnalyticsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 数据分析Service，组装看板所需的4块数据
 */
@Service
public class AnalyticsService {
    @Autowired
    private AnalyticsMapper analyticsMapper;

    // 组装完整看板数据
    public AnalyticsOverviewDTO overview() {
        AnalyticsOverviewDTO result = new AnalyticsOverviewDTO();

        // 1. 系统概览
        result.setSystemOverview(buildSystemOverview());

        // 2. 情绪趋势
        result.setEmotionTrend(analyticsMapper.emotionTrend());

        // 3. 咨询统计
        AnalyticsOverviewDTO.ConsultationStats stats = new AnalyticsOverviewDTO.ConsultationStats();
        stats.setTotalSessions(analyticsMapper.countAllSessions());
        stats.setAvgDurationMinutes(analyticsMapper.avgDurationMinutes());
        stats.setDailyTrend(analyticsMapper.consultationDailyTrend());
        result.setConsultationStats(stats);

        // 4. 用户活跃度（合并4个按天来源）
        result.setUserActivity(buildUserActivity());

        return result;
    }

    // 组装系统概览
    private AnalyticsOverviewDTO.SystemOverview buildSystemOverview() {
        AnalyticsOverviewDTO.SystemOverview overview = new AnalyticsOverviewDTO.SystemOverview();
        overview.setTotalUsers(analyticsMapper.countAllUsers());
        overview.setActiveUsers(analyticsMapper.countActiveUsers());
        overview.setTotalDiaries(analyticsMapper.countAllDiaries());
        overview.setTodayNewDiaries(analyticsMapper.countTodayDiaries());
        overview.setTotalSessions(analyticsMapper.countAllSessions());
        overview.setTodayNewSessions(analyticsMapper.countTodaySessions());
        overview.setAvgMoodScore(analyticsMapper.avgMoodScore());
        return overview;
    }

    // 组装用户活跃度：把4个按天统计按日期合并，缺项补0
    private List<AnalyticsOverviewDTO.ActivityItem> buildUserActivity() {
        Map<String, Long> active = toDateMap(analyticsMapper.activeUsersByDay());
        Map<String, Long> newUsers = toDateMap(analyticsMapper.newUsersByDay());
        Map<String, Long> diary = toDateMap(analyticsMapper.diaryUsersByDay());
        Map<String, Long> consultation = toDateMap(analyticsMapper.consultationUsersByDay());

        // 所有日期的并集，TreeSet保证按日期升序
        TreeSet<String> allDates = new TreeSet<>();
        allDates.addAll(active.keySet());
        allDates.addAll(newUsers.keySet());
        allDates.addAll(diary.keySet());
        allDates.addAll(consultation.keySet());

        return allDates.stream().map(date -> {
            AnalyticsOverviewDTO.ActivityItem item = new AnalyticsOverviewDTO.ActivityItem();
            item.setDate(date);
            item.setActiveUsers(active.getOrDefault(date, 0L));
            item.setNewUsers(newUsers.getOrDefault(date, 0L));
            item.setDiaryUsers(diary.getOrDefault(date, 0L));
            item.setConsultationUsers(consultation.getOrDefault(date, 0L));
            return item;
        }).collect(Collectors.toList());
    }

    // 把 [{d:日期, c:数量}] 转成 日期->数量 的Map
    private Map<String, Long> toDateMap(List<Map<String, Object>> rows) {
        Map<String, Long> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String date = String.valueOf(row.get("d"));
            // 数据库COUNT可能返回Long或BigInteger，统一按Number处理
            Long count = ((Number) row.get("c")).longValue();
            map.put(date, count);
        }
        return map;
    }
}
