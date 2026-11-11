package org.example.aispringboot.DTO.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识文章分页响应DTO，在文章字段基础上补充分类名、作者名
 */
@Data
public class KnowledgeArticlePageDTO {
    // 文章ID
    private String id;

    // 分类ID
    private Long categoryId;

    // 分类名称（关联查询）
    private String categoryName;

    // 标题
    private String title;

    // 摘要
    private String summary;

    // 正文
    private String content;

    // 封面
    private String coverImage;

    // 标签
    private String tags;

    // 作者ID
    private Long authorId;

    // 作者名称（关联查询）
    private String authorName;

    // 阅读量
    private Integer readCount;

    // 状态
    private Integer status;

    // 发布时间
    private LocalDateTime publishedAt;

    // 创建时间
    private LocalDateTime createdAt;

    // 更新时间
    private LocalDateTime updatedAt;
}
