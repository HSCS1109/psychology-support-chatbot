package org.example.aispringboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识文章实体，对应 knowledge_article 表，主键为UUID字符串
 */
@Data
@TableName("knowledge_article")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeArticle {
    // 文章ID（UUID），前端可传入，未传时由MyBatis-Plus自动生成
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    // 所属分类ID
    @TableField("category_id")
    private Long categoryId;

    // 文章标题
    private String title;

    // 文章摘要
    private String summary;

    // 文章正文（富文本）
    private String content;

    // 封面图片路径
    @TableField("cover_image")
    private String coverImage;

    // 标签，多个用逗号分隔
    private String tags;

    // 作者ID
    @TableField("author_id")
    private Long authorId;

    // 阅读量
    @TableField("read_count")
    private Integer readCount;

    // 状态 0:草稿 1:已发布 2:已下线
    private Integer status;

    // 发布时间
    @TableField("published_at")
    private LocalDateTime publishedAt;

    // 创建时间
    @TableField("created_at")
    private LocalDateTime createdAt;

    // 更新时间
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
