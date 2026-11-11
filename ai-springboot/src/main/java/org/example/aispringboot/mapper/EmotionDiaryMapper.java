package org.example.aispringboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.aispringboot.entity.EmotionDiary;

/**
 * 情绪日记Mapper，继承BaseMapper即可获得增删改查能力
 */
@Mapper
public interface EmotionDiaryMapper extends BaseMapper<EmotionDiary> {
}
