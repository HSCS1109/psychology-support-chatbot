package org.example.aispringboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.aispringboot.entity.KnowledgeArticle;

/**
 * 知识文章Mapper
 */
@Mapper
public interface KnowledgeArticleMapper extends BaseMapper<KnowledgeArticle> {
}
