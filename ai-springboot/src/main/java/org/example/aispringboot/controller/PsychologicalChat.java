package org.example.aispringboot.controller;

//hutool的json工具，用来把对象转json字符串
import cn.hutool.json.JSONUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.validation.Valid;
import org.example.aispringboot.AiService.PsychologicalSupportService;
import org.example.aispringboot.AiService.IntentRecognitionService;
import org.example.aispringboot.AiService.StructOutPut;
import org.example.aispringboot.DTO.command.ConsultationSessionCreateDTO;
import org.example.aispringboot.DTO.command.ConsultationStreamDTO;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.common.ResultCode;
import org.example.aispringboot.entity.ConsultationMessage;
import org.example.aispringboot.service.ConsultationSessionService;
import org.example.aispringboot.service.ConsultationMessageService;
import org.example.aispringboot.utils.JwtTokenUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent; // SSE流式有关
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;  // 响应式数据流

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aispringboot.DTO.response.SessionPageResponseDTO;
//框架知道他是一个控制器，而且方法返回的对象会自动转成JSON写进响应体
@RestController
@RequestMapping("/api/psychological-chat")  // 定地址前缀，类中的所有方法地址都以它开头
public class PsychologicalChat {
    @Autowired
    private PsychologicalSupportService psychologicalSupportService;
    @Autowired
    private ChatClient chatClient;
    @Autowired
    private ConsultationSessionService consultationSessionService;
    @Autowired
    private ConsultationMessageService consultationMessageService;
    @Autowired
    private IntentRecognitionService intentRecognitionService;

    @PostMapping("/session/start")
    public Result<StructOutPut.StreamChatSession> startSession(@Valid @RequestBody ConsultationSessionCreateDTO createDTO) {
        // 获取当前用户
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();

        // 调用service层创建会话
        StructOutPut.StreamChatSession session =  psychologicalSupportService.startSession(userId, createDTO);
        return Result.ok(session);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@Valid @RequestBody ConsultationStreamDTO streamDTO) {
        // 获取当前用户
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();

        if (userId == null) {
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    // 将对象转换为JSON字符串
                    .data(JSONUtil.toJsonStr(Result.error(ResultCode.UNAUTHORIZED.getCode(),ResultCode.UNAUTHORIZED.getMsg(), "用户未登录")))
                    .build());
        }
        // 开始流式对话
        return psychologicalSupportService.streamPsychologicalChat(streamDTO.getSessionId(),streamDTO.getUserMessage())
                .map(Fragment -> {
                    return ServerSentEvent.<String>builder()
                            .event("message")
                            .data(JSONUtil.toJsonStr(Result.ok(Map.of("content",Fragment,"type","normal"))))
                            .build();
                })
                .concatWith(Flux.just(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("{}")
                        .build()
                ))
                .delayElements(Duration.ofMillis(50));  // 添加一个延时确保流式输出的体验
    }

    // 意图识别接口：返回意图、命中方式、置信度，供评测与排查使用
    @PostMapping("/intent")
    public Result<Map<String, Object>> recognizeIntent(@RequestBody Map<String, String> body) {
        String message = body.get("message");
        IntentRecognitionService.IntentResult result = intentRecognitionService.recognize(message);
        return Result.ok(Map.of(
                "intent", result.intent().name(),
                "source", result.source(),
                "confidence", result.confidence()
        ));
    }

    // 管理端分页查询所有咨询会话
    @GetMapping("/sessions")
    public Result<Page<SessionPageResponseDTO>> sessions(
            @RequestParam(value = "currentPage", defaultValue = "1") Integer currentPage,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        Page<SessionPageResponseDTO> page = consultationSessionService.adminPage(currentPage, size);
        return Result.ok(page);
    }

    // 管理端查询某会话的完整对话消息
    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<ConsultationMessage>> sessionMessages(@PathVariable("sessionId") Long sessionId) {
        List<ConsultationMessage> messages = consultationMessageService.listBySession(sessionId);
        return Result.ok(messages);
    }

}
