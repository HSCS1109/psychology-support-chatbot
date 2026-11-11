package org.example.aispringboot.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.aispringboot.DTO.response.AnalyticsOverviewDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 数据分析聚合Mapper，全部使用服务端聚合SQL
 */
@Mapper
public interface AnalyticsMapper {

    // 总用户数
    @Select("SELECT COUNT(*) FROM user")
    Long countAllUsers();

    // 活跃用户数（状态正常）
    @Select("SELECT COUNT(*) FROM user WHERE status = 1")
    Long countActiveUsers();

    // 情绪日志总数
    @Select("SELECT COUNT(*) FROM emotion_diary")
    Long countAllDiaries();

    // 今日新增日记
    @Select("SELECT COUNT(*) FROM emotion_diary WHERE diary_date = CURDATE()")
    Long countTodayDiaries();

    // 咨询会话总数
    @Select("SELECT COUNT(*) FROM consultation_session")
    Long countAllSessions();

    // 今日新增会话
    @Select("SELECT COUNT(*) FROM consultation_session WHERE DATE(started_at) = CURDATE()")
    Long countTodaySessions();

    // 平均情绪分
    @Select("SELECT ROUND(AVG(mood_score), 2) FROM emotion_diary")
    BigDecimal avgMoodScore();

    // 情绪趋势：按日记日期聚合平均情绪分与记录数
    @Select("SELECT DATE_FORMAT(diary_date, '%Y-%m-%d') AS date, " +
            "ROUND(AVG(mood_score), 2) AS avgMoodScore, COUNT(*) AS recordCount " +
            "FROM emotion_diary GROUP BY DATE_FORMAT(diary_date, '%Y-%m-%d') ORDER BY date")
    List<AnalyticsOverviewDTO.TrendItem> emotionTrend();

    // 咨询每日趋势：每天会话数与参与用户数
    @Select("SELECT DATE_FORMAT(started_at, '%Y-%m-%d') AS date, " +
            "COUNT(*) AS sessionCount, COUNT(DISTINCT user_id) AS userCount " +
            "FROM consultation_session GROUP BY DATE_FORMAT(started_at, '%Y-%m-%d') ORDER BY date")
    List<AnalyticsOverviewDTO.DailyItem> consultationDailyTrend();

    // 平均会话时长（分钟）：会话最后一条消息时间减去开始时间
    @Select("SELECT ROUND(AVG(TIMESTAMPDIFF(MINUTE, s.started_at, m.last_time)), 2) " +
            "FROM consultation_session s " +
            "INNER JOIN (SELECT session_id, MAX(created_at) AS last_time " +
            "FROM consultation_message GROUP BY session_id) m ON s.id = m.session_id")
    BigDecimal avgDurationMinutes();

    // 每日新增用户
    @Select("SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS d, COUNT(*) AS c " +
            "FROM user GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d')")
    List<Map<String, Object>> newUsersByDay();

    // 每日写日记的去重用户
    @Select("SELECT DATE_FORMAT(diary_date, '%Y-%m-%d') AS d, COUNT(DISTINCT user_id) AS c " +
            "FROM emotion_diary GROUP BY DATE_FORMAT(diary_date, '%Y-%m-%d')")
    List<Map<String, Object>> diaryUsersByDay();

    // 每日咨询的去重用户
    @Select("SELECT DATE_FORMAT(started_at, '%Y-%m-%d') AS d, COUNT(DISTINCT user_id) AS c " +
            "FROM consultation_session GROUP BY DATE_FORMAT(started_at, '%Y-%m-%d')")
    List<Map<String, Object>> consultationUsersByDay();

    // 每日活跃用户（当天有日记或咨询的去重用户）
    @Select("SELECT DATE_FORMAT(activity_date, '%Y-%m-%d') AS d, COUNT(DISTINCT user_id) AS c FROM (" +
            "SELECT user_id, diary_date AS activity_date FROM emotion_diary " +
            "UNION " +
            "SELECT user_id, DATE(started_at) FROM consultation_session) t " +
            "GROUP BY DATE_FORMAT(activity_date, '%Y-%m-%d')")
    List<Map<String, Object>> activeUsersByDay();
}
