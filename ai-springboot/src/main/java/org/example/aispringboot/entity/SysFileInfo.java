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
 * 文件信息实体，对应 sys_file_info 表
 */
@Data
@TableName("sys_file_info")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysFileInfo {
    // 文件记录ID
    @TableId(type = IdType.AUTO)
    private Long id;

    // 原始文件名
    @TableField("original_name")
    private String originalName;

    // 文件访问路径
    @TableField("file_path")
    private String filePath;

    // 文件大小（字节）
    @TableField("file_size")
    private Long fileSize;

    // 文件类型，如IMG
    @TableField("file_type")
    private String fileType;

    // 业务类型，如ARTICLE
    @TableField("business_type")
    private String businessType;

    // 业务ID（文章UUID）
    @TableField("business_id")
    private String businessId;

    // 业务字段，如cover
    @TableField("business_field")
    private String businessField;

    // 上传用户ID
    @TableField("upload_user_id")
    private Long uploadUserId;

    // 是否临时文件
    @TableField("is_temp")
    private Integer isTemp;

    // 状态
    private Integer status;

    // 创建时间
    @TableField("create_time")
    private LocalDateTime createTime;

    // 过期时间
    @TableField("expire_time")
    private LocalDateTime expireTime;
}
