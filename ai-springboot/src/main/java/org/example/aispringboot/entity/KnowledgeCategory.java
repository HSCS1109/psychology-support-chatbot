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
 * 知识分类实体，对应 knowledge_category 表
 */
@Data
@TableName("knowledge_category")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeCategory {
    // 分类ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 父分类ID，顶级分类为0
    @TableField("parent_id")
    private Long parentId;

    // 分类名称
    @TableField("category_name")
    private String categoryName;

    // 分类编码
    @TableField("category_code")
    private String categoryCode;

    // 分类描述
    private String description;

    // 排序号，数字越小越靠前
    @TableField("sort_order")
    private Integer sortOrder;

    // 状态 1:启用 0:停用
    private Integer status;

    // 创建时间
    @TableField("created_at")
    private LocalDateTime createdAt;

    // 更新时间
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
