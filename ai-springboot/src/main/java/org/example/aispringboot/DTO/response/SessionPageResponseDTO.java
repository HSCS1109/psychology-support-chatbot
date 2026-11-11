package org.example.aispringboot.DTO.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端咨询会话分页响应DTO
 */
@Data
public class SessionPageResponseDTO {
    // 会话ID
    private Long id;

    // 用户ID
    private Long userId;

    // 用户昵称（没有昵称则显示用户名）
    private String userNickname;

    // 会话标题
    private String sessionTitle;

    // 最后一条消息内容
    private String lastMessageContent;

    // 消息数量
    private Integer messageCount;

    // 最后一条消息时间
    private LocalDateTime lastMessageTime;

    // 会话开始时间
    private LocalDateTime startedAt;
}
