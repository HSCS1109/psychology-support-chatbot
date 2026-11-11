package org.example.aispringboot.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aispringboot.DTO.command.KnowledgeArticleCommandDTO;
import org.example.aispringboot.DTO.response.KnowledgeArticlePageDTO;
import org.example.aispringboot.entity.KnowledgeArticle;
import org.example.aispringboot.entity.KnowledgeCategory;
import org.example.aispringboot.entity.User;
import org.example.aispringboot.mapper.KnowledgeArticleMapper;
import org.example.aispringboot.mapper.KnowledgeCategoryMapper;
import org.example.aispringboot.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识库Service，处理分类与文章
 */
@Service
public class KnowledgeService {
    @Autowired
    private KnowledgeArticleMapper articleMapper;
    @Autowired
    private KnowledgeCategoryMapper categoryMapper;
    @Autowired
    private UserMapper userMapper;

    // 查询启用的分类列表（前端按扁平数组使用），按排序号升序
    public List<KnowledgeCategory> categoryTree() {
        LambdaQueryWrapper<KnowledgeCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeCategory::getStatus, 1)
                .orderByAsc(KnowledgeCategory::getSortOrder);
        return categoryMapper.selectList(wrapper);
    }

    // 文章分页。frontend=true时只查已发布（前台），否则按管理端条件筛选
    public Page<KnowledgeArticlePageDTO> articlePage(Integer current, Integer size,
                                                     String title, Long categoryId,
                                                     Integer status, boolean frontend) {
        Page<KnowledgeArticle> articlePage = new Page<>(current, size);
        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        if (frontend) {
            // 前台只展示已发布文章
            wrapper.eq(KnowledgeArticle::getStatus, 1);
        } else if (status != null) {
            wrapper.eq(KnowledgeArticle::getStatus, status);
        }
        if (StringUtils.hasText(title)) {
            wrapper.like(KnowledgeArticle::getTitle, title);
        }
        if (categoryId != null) {
            wrapper.eq(KnowledgeArticle::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(KnowledgeArticle::getUpdatedAt);
        articleMapper.selectPage(articlePage, wrapper);

        // 组装分页DTO，补充分类名与作者名
        List<KnowledgeArticlePageDTO> records = new ArrayList<>();
        for (KnowledgeArticle article : articlePage.getRecords()) {
            KnowledgeArticlePageDTO dto = new KnowledgeArticlePageDTO();
            // 同名字段用hutool复制
            BeanUtil.copyProperties(article, dto);

            KnowledgeCategory category = categoryMapper.selectById(article.getCategoryId());
            if (category != null) {
                dto.setCategoryName(category.getCategoryName());
            }
            if (article.getAuthorId() != null) {
                User author = userMapper.selectById(article.getAuthorId());
                if (author != null) {
                    dto.setAuthorName(author.getDisplayName());
                }
            }
            records.add(dto);
        }

        Page<KnowledgeArticlePageDTO> result = new Page<>(current, size);
        result.setRecords(records);
        result.setTotal(articlePage.getTotal());
        return result;
    }

    // 查询文章详情
    public KnowledgeArticle getArticleDetail(String id) {
        return articleMapper.selectById(id);
    }

    // 新建文章，authorId为当前登录用户，默认草稿状态
    public KnowledgeArticle createArticle(Long authorId, KnowledgeArticleCommandDTO dto) {
        KnowledgeArticle article = new KnowledgeArticle();
        BeanUtil.copyProperties(dto, article);
        article.setAuthorId(authorId);
        if (article.getStatus() == null) {
            article.setStatus(0);
        }
        if (article.getReadCount() == null) {
            article.setReadCount(0);
        }
        articleMapper.insert(article);
        return article;
    }

    // 更新文章
    public KnowledgeArticle updateArticle(String id, KnowledgeArticleCommandDTO dto) {
        KnowledgeArticle article = articleMapper.selectById(id);
        if (article == null) {
            return null;
        }
        // 保留不允许被表单覆盖的字段
        Long authorId = article.getAuthorId();
        Integer readCount = article.getReadCount();
        BeanUtil.copyProperties(dto, article);
        article.setId(id);
        article.setAuthorId(authorId);
        article.setReadCount(readCount);
        articleMapper.updateById(article);
        return article;
    }

    // 修改文章状态，发布时记录发布时间
    public boolean changeStatus(String id, Integer status) {
        KnowledgeArticle article = articleMapper.selectById(id);
        if (article == null) {
            return false;
        }
        article.setStatus(status);
        if (status == 1 && article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }
        articleMapper.updateById(article);
        return true;
    }

    // 删除文章
    public boolean deleteArticle(String id) {
        return articleMapper.deleteById(id) > 0;
    }
}
