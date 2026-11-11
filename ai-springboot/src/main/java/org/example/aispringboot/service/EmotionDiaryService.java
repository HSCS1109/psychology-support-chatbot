package org.example.aispringboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aispringboot.DTO.command.EmotionDiaryCommandDTO;
import org.example.aispringboot.entity.EmotionDiary;
import org.example.aispringboot.mapper.EmotionDiaryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * 情绪日记Service
 */
@Service
public class EmotionDiaryService {
    @Autowired
    private EmotionDiaryMapper emotionDiaryMapper;

    // 保存一条情绪日记
    public EmotionDiary saveDiary(Long userId, EmotionDiaryCommandDTO dto) {
        EmotionDiary diary = EmotionDiary.builder()
                .userId(userId)
                // 字符串日期转LocalDate
                .diaryDate(LocalDate.parse(dto.getDiaryDate()))
                .moodScore(dto.getMoodScore())
                .dominantEmotion(dto.getDominantEmotion())
                .emotionTriggers(dto.getEmotionTriggers())
                .diaryContent(dto.getDiaryContent())
                .sleepQuality(dto.getSleepQuality())
                .stressLevel(dto.getStressLevel())
                .build();
        emotionDiaryMapper.insert(diary);
        return diary;
    }

    // 查询某用户的全部日记，按日期倒序
    public List<EmotionDiary> listByUser(Long userId) {
        LambdaQueryWrapper<EmotionDiary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmotionDiary::getUserId, userId)
                .orderByDesc(EmotionDiary::getDiaryDate);
        return emotionDiaryMapper.selectList(wrapper);
    }

    // 查询某用户最近一条日记
    public EmotionDiary getLatestByUser(Long userId) {
        LambdaQueryWrapper<EmotionDiary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmotionDiary::getUserId, userId)
                .orderByDesc(EmotionDiary::getDiaryDate)
                .last("limit 1");
        return emotionDiaryMapper.selectOne(wrapper);
    }

    // 管理端分页查询，支持按用户ID、评分范围筛选
    public Page<EmotionDiary> adminPage(Integer current, Integer size, Long userId, String moodScoreRange) {
        Page<EmotionDiary> page = new Page<>(current, size);
        LambdaQueryWrapper<EmotionDiary> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(EmotionDiary::getUserId, userId);
        }
        // 评分范围形如 "1-3"
        if (StringUtils.hasText(moodScoreRange)) {
            String[] parts = moodScoreRange.split("-");
            wrapper.between(EmotionDiary::getMoodScore, Integer.valueOf(parts[0]), Integer.valueOf(parts[1]));
        }
        wrapper.orderByDesc(EmotionDiary::getCreatedAt);
        return emotionDiaryMapper.selectPage(page, wrapper);
    }

    // 管理端按ID删除
    public boolean deleteById(Long id) {
        return emotionDiaryMapper.deleteById(id) > 0;
    }
}
