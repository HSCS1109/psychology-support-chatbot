package org.example.aispringboot.DTO.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 情绪日记提交DTO，接收前端表单字段
 */
@Data
public class EmotionDiaryCommandDTO {
    // 日记日期，前端传 yyyy-MM-dd 字符串
    @NotBlank(message = "日记日期不能为空")
    private String diaryDate;

    // 情绪评分 1-10
    @NotNull(message = "情绪评分不能为空")
    @Min(value = 1, message = "评分最低1分")
    @Max(value = 10, message = "评分最高10分")
    private Integer moodScore;

    // 主要情绪
    private String dominantEmotion;

    // 情绪触发因素
    private String emotionTriggers;

    // 日记正文
    private String diaryContent;

    // 睡眠质量 1-5
    private Integer sleepQuality;

    // 压力水平 1-5
    private Integer stressLevel;
}
