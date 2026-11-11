package org.example.aispringboot.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aispringboot.DTO.command.ConsultationSessionCreateDTO;
import org.example.aispringboot.DTO.response.ConsultationMessageResponseDTO;
import org.example.aispringboot.DTO.response.SessionPageResponseDTO;
import org.example.aispringboot.entity.ConsultationSession;
import org.example.aispringboot.entity.User;
import org.example.aispringboot.mapper.ConsultationSessionMapper;
import org.example.aispringboot.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ConsultationSessionService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ConsultationSessionMapper consultationSessionMapper;

    @Autowired
    private ConsultationMessageService consultationMessageService;

    // 插入表的方法
    public ConsultationSession createSession(Long userId, ConsultationSessionCreateDTO createDTO) {
        // 验证用户是否存在
        User user = userMapper.selectById(userId);
        if (user != null){
            // 创建会话记录
            ConsultationSession session = ConsultationSession.builder()
                    .userId(userId)
                    .sessionTitle(createDTO.getSessionTitle())
                    .startedAt(LocalDateTime.now())
                    .build();
            // 如果未提供标题
            if (StrUtil.isBlank(createDTO.getSessionTitle())){
                session.setSessionTitle(String.format("宁渡AI助手 -" + DateUtil.format(LocalDateTime.now(), "MM-dd HH:mm")));
            }
            // 插入数据库
            consultationSessionMapper.insert(session);
            return session;
        }

        return null;
    }

    // 管理端分页查询会话，并组装用户昵称、消息数、最后一条消息
    public Page<SessionPageResponseDTO> adminPage(Integer current, Integer size) {
        // 1. 分页查询会话，按开始时间倒序
        Page<ConsultationSession> sessionPage = new Page<>(current, size);
        LambdaQueryWrapper<ConsultationSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ConsultationSession::getStartedAt);
        consultationSessionMapper.selectPage(sessionPage, wrapper);

        // 2. 逐个会话组装展示数据
        List<SessionPageResponseDTO> records = new ArrayList<>();
        for (ConsultationSession session : sessionPage.getRecords()) {
            SessionPageResponseDTO dto = new SessionPageResponseDTO();
            dto.setId(session.getId());
            dto.setUserId(session.getUserId());
            dto.setSessionTitle(session.getSessionTitle());
            dto.setStartedAt(session.getStartedAt());

            // 用户昵称（无昵称时getDisplayName会回退到用户名）
            User user = userMapper.selectById(session.getUserId());
            if (user != null) {
                dto.setUserNickname(user.getDisplayName());
            }

            // 消息数量
            dto.setMessageCount(consultationMessageService.getMessageCountBySessionId(session.getId()));

            // 最后一条消息
            ConsultationMessageResponseDTO lastMessage = consultationMessageService.getLastMessageBySessionId(session.getId());
            if (lastMessage != null) {
                dto.setLastMessageContent(lastMessage.getContent());
                dto.setLastMessageTime(lastMessage.getCreatedAt());
            }

            records.add(dto);
        }

        // 3. 组装成分页结果返回
        Page<SessionPageResponseDTO> result = new Page<>(current, size);
        result.setRecords(records);
        result.setTotal(sessionPage.getTotal());
        return result;
    }
}
