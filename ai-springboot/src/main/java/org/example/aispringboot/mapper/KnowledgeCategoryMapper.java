package org.example.aispringboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.aispringboot.entity.KnowledgeCategory;

/**
 * 知识分类Mapper
 */
@Mapper
public interface KnowledgeCategoryMapper extends BaseMapper<KnowledgeCategory> {
}
