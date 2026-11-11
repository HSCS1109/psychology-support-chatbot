package org.example.aispringboot.DTO.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改文章状态DTO（发布/下线）
 */
@Data
public class ArticleStatusCommandDTO {
    // 目标状态 0:草稿 1:已发布 2:已下线
    @NotNull(message = "状态不能为空")
    private Integer status;
}
