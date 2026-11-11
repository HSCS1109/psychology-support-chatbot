package org.example.aispringboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.aispringboot.DTO.response.ConsultationMessageResponseDTO;
import org.example.aispringboot.entity.ConsultationMessage;
import org.example.aispringboot.mapper.ConsultationMessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConsultationMessageService {
    @Autowired
    private ConsultationMessageMapper consultationMessageMapper;

    public ConsultationMessage saveUserMessage(Long sessionId, String content, String emotion_tag){
        // 构建用户消息实体
        ConsultationMessage userMessage = ConsultationMessage.builder()
                .sessionId(sessionId)
                .senderType(1)
                .messageType(1)
                .content(content)
                .emotionTag(emotion_tag)
                .createdAt(LocalDateTime.now())
                .build();
        // 插入数据库
        consultationMessageMapper.insert(userMessage);
        return userMessage;
    }

    public ConsultationMessage saveAiMessage(Long sessionId,String content,String aiModel){
        ConsultationMessage message = ConsultationMessage.builder()
                .sessionId(sessionId)
                .senderType(2)
                .messageType(1)
                .content(content)
                .aiModel(aiModel)
                .createdAt(LocalDateTime.now())
                .build();
        // 插入数据库
        consultationMessageMapper.insert(message);
        return message;
    }

    public Integer getMessageCountBySessionId(Long sessionId){
        LambdaQueryWrapper<ConsultationMessage> queryWrapper = new LambdaQueryWrapper<>();
        // 查询条件构建
        queryWrapper.eq(ConsultationMessage::getSessionId, sessionId);

        // 统计数量
        Long count = consultationMessageMapper.selectCount(queryWrapper);

        // 返回统计结果
        return count.intValue();
    }

    // 按会话ID查询全部消息，按创建时间正序（用于管理端对话详情）
    public List<ConsultationMessage> listBySession(Long sessionId) {
        LambdaQueryWrapper<ConsultationMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConsultationMessage::getSessionId, sessionId)
                .orderByAsc(ConsultationMessage::getCreatedAt);
        return consultationMessageMapper.selectList(queryWrapper);
    }

    // 获取会话的最后一条消息
    public ConsultationMessageResponseDTO getLastMessageBySessionId(Long sessionId){
        // 构建查询条件
        LambdaQueryWrapper<ConsultationMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConsultationMessage::getSessionId, sessionId)
                .orderByDesc(ConsultationMessage::getCreatedAt)
                .last("limit 1");

        // 查询最后一条消息
        ConsultationMessage lastMessage = consultationMessageMapper.selectOne(queryWrapper);
        return lastMessage != null ? convertToResponseDTO(lastMessage) : null;
    }

    private ConsultationMessageResponseDTO convertToResponseDTO(ConsultationMessage message) {
        if (message == null) {
            return null;
        }

        // 手动逐字段赋值，确保转换的准确性和可控性
        ConsultationMessageResponseDTO responseDTO = new ConsultationMessageResponseDTO();
        responseDTO.setId(message.getId());
        responseDTO.setSessionId(message.getSessionId());
        responseDTO.setSenderType(message.getSenderType());
        responseDTO.setMessageType(message.getMessageType());
        responseDTO.setContent(message.getContent());
        responseDTO.setEmotionTag(message.getEmotionTag());
        responseDTO.setAiModel(message.getAiModel());
        responseDTO.setCreatedAt(message.getCreatedAt());

        // 设置描述字段（通过实体方法获取）
        responseDTO.setSenderTypeDesc(message.getSenderTypeDesc());
        responseDTO.setMessageTypeDesc(message.getMessageTypeDesc());

        // 计算消息长度
        responseDTO.calculateContentLength();

        return responseDTO;
    }


}
