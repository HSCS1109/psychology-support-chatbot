package org.example.aispringboot.DTO.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 知识文章新建/更新DTO
 */
@Data
public class KnowledgeArticleCommandDTO {
    // 文章ID（编辑时使用，新增时可由前端传UUID）
    private String id;

    // 标题
    @NotBlank(message = "文章标题不能为空")
    @Size(max = 200, message = "文章标题最多200个字符")
    private String title;

    // 分类ID（前端传字符串数字，Spring自动转换）
    @NotNull(message = "请选择文章分类")
    private Long categoryId;

    // 摘要
    private String summary;

    // 正文
    @NotBlank(message = "文章内容不能为空")
    private String content;

    // 封面
    private String coverImage;

    // 标签，逗号分隔
    private String tags;
}
