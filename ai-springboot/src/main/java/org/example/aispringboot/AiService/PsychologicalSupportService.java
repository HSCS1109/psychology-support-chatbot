package org.example.aispringboot.AiService;

import org.example.aispringboot.DTO.command.ConsultationSessionCreateDTO;
import org.example.aispringboot.DTO.response.ConsultationMessageResponseDTO;
import org.example.aispringboot.entity.ConsultationSession;
import org.example.aispringboot.service.ConsultationMessageService;
import org.example.aispringboot.service.ConsultationSessionService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PsychologicalSupportService {
    @Autowired
    private ChatMemory chatMemory;
    @Autowired
    @Qualifier("open-ai")
    private ChatClient chatClient;

    @Autowired
    private ConsultationSessionService consultationSessionService;

    @Autowired
    private ConsultationMessageService consultationMessageService;

    @Autowired
    private SafetyGuardService safetyGuardService;

    @Autowired
    private IntentRecognitionService intentRecognitionService;

    @Autowired
    private ToolExecutionService toolExecutionService;

    @Autowired
    private TraceService traceService;

    @Autowired
    private ScaleAssessmentService scaleAssessmentService;

    // 无效调用计数：意图是工具类，但最终回复没拿到工具结果
    private static final java.util.concurrent.atomic.AtomicLong INVALID_TOOL_CALLS =
            new java.util.concurrent.atomic.AtomicLong();
    // 电话号码特征：400-xxx-xxxx 或 区号-号码
    private static final java.util.regex.Pattern PHONE_PATTERN =
            java.util.regex.Pattern.compile("400-?\\d{3}-?\\d{4}|\\d{3,4}-\\d{7,8}");

    public StructOutPut.StreamChatSession startSession(Long userId, ConsultationSessionCreateDTO createDTO) {
        // 创建数据库会话记录
        ConsultationSession consultationSession = consultationSessionService.createSession(userId, createDTO);

        // 将初始用户消息保存到message表中
        consultationMessageService.saveUserMessage(consultationSession.getId(), createDTO.getInitialMessage(), null);

        // 创建会话信息
        String session = "sesson_" + consultationSession.getId();
        return new StructOutPut.StreamChatSession(
                session,
                userId,
                createDTO.getInitialMessage(),
                System.currentTimeMillis(),
                System.currentTimeMillis() + 86400000L,  // 代表24小时
                1,
                "ACTIVE"
        );
    }

    public Flux<String> streamPsychologicalChat(String sessionId, String userMessage) {
        // 安全护栏：危机关键词前置拦截，不调用大模型，直接返回固定求助话术
        if (safetyGuardService.isCrisis(userMessage)) {
            String traceId = traceService.startTrace(sessionId, userMessage);
            traceService.put(traceId, "safetyGuard", "CRISIS_BLOCKED");
            Long dbSessionId = extractSessionId(sessionId);
            String response = safetyGuardService.getCrisisResponse();
            if (dbSessionId != null) {
                consultationMessageService.saveUserMessage(dbSessionId, userMessage, null);
                consultationMessageService.saveAiMessage(dbSessionId, response, "safety-guard");
            }
            traceService.finalizeTrace(traceId, response, "CRISIS_BLOCKED");
            return Flux.just(response);
        }
        // 创建响应流
        return Flux.create(sink -> {
            // sink.next(数据1) 发布数据
            // sink.complete() 完成之后的流数据
            // sink.error(exception) 发布异常数据

            // 获取的数据库字段中的sessionId
            Long dbSessionId = extractSessionId(sessionId);
            if(dbSessionId == null){
                sink.error(new RuntimeException("会话ID格式错误"));
                return;
            }

            // 开启链路追踪
            final String traceId = traceService.startTrace(sessionId, userMessage);

            // 是否为初始消息
            boolean isInitialMessage = false;
            // 判断当前的sessionId对应的消息是不是初始数据，避免重复保存
            Integer messageCount = consultationMessageService.getMessageCountBySessionId(dbSessionId);
            if (messageCount == 1){
                // 初始消息
                ConsultationMessageResponseDTO lastMessage = consultationMessageService.getLastMessageBySessionId(dbSessionId);
                if(lastMessage != null && lastMessage.getSenderType() == 1 && userMessage.equals(lastMessage.getContent())){
                    isInitialMessage = true;
                }
            }
            if(!isInitialMessage){
                // 保存用户消息到数据库
                consultationMessageService.saveUserMessage(dbSessionId, userMessage, null);
            }

            // ===== 逐题量表测评：确定性流程，不调用大模型 =====
            if (scaleAssessmentService.isInProgress(sessionId)) {
                ScaleAssessmentService.AssessmentReply reply =
                        scaleAssessmentService.isQuitMessage(userMessage)
                                ? scaleAssessmentService.quit(sessionId)
                                : scaleAssessmentService.handleAnswer(sessionId, userMessage);
                sink.next(reply.text());
                consultationMessageService.saveAiMessage(dbSessionId, reply.text(), "scale-assessment");
                traceService.put(traceId, "assessment", reply.finished() ? "FINISHED" : "IN_PROGRESS");
                if (reply.totalScore() != null) {
                    traceService.put(traceId, "assessmentTotalScore", reply.totalScore());
                    traceService.put(traceId, "assessmentLevel", reply.level());
                }
                traceService.finalizeTrace(traceId, reply.text(),
                        reply.finished() ? "ASSESSMENT_DONE" : "ASSESSMENT");
                sink.complete();
                return;
            }
            // 命中启动词：开始测评并返回第一题
            if (scaleAssessmentService.tryStart(userMessage)) {
                ScaleAssessmentService.AssessmentReply reply = scaleAssessmentService.start(sessionId);
                sink.next(reply.text());
                consultationMessageService.saveAiMessage(dbSessionId, reply.text(), "scale-assessment");
                traceService.put(traceId, "assessment", "STARTED");
                traceService.finalizeTrace(traceId, reply.text(), "ASSESSMENT_START");
                sink.complete();
                return;
            }
            // ===== 测评逻辑结束 =====

            // 进行流式对话
            // 意图识别：规则+模型两层，再按意图路由
            IntentRecognitionService.IntentResult intentResult = intentRecognitionService.recognize(userMessage);
            System.out.println("意图识别：" + intentResult.intent().getDisplayName()
                    + "（来源:" + intentResult.source() + ", 置信度:" + intentResult.confidence() + "）");
            // 意图埋点
            traceService.put(traceId, "intent", intentResult.intent().name());
            traceService.put(traceId, "intentSource", intentResult.source());
            traceService.put(traceId, "intentConfidence", intentResult.confidence());

            // 生成对话记忆管理
            String conversationId = "conversation_" + sessionId;
            // 构建系统提示词：基础提示词 + 按意图追加引导
            String systemPrompt = PromptManage.PSYCHOLOGICAL_SUPPORT_SYSTEM_PROMPT
                    + PromptManage.getIntentGuidance(intentResult.intent());
            List<Message> userMessages = new ArrayList<>();
            userMessages.add(new UserMessage(userMessage));
            chatMemory.add(conversationId, userMessages);

            // 最大尝试次数：首次 + 1次自动重试
            final int maxAttempts = 2;
            // 当前尝试次数（从1开始）
            final int[] attempt = {1};
            // 用于存储本次尝试的完整响应
            final StringBuilder fullaiResponse = new StringBuilder();
            // 持有最后一个流式分片的token用量（最后一片通常是累计值）
            final org.springframework.ai.chat.metadata.Usage[] lastUsage = new org.springframework.ai.chat.metadata.Usage[1];

            // 单次尝试的执行逻辑，支持无效时自动重试
            Runnable runAttempt = new Runnable() {
                @Override
                public void run() {
                    // 用.system()设置系统提示词，避免传入自定义Prompt对象覆盖默认工具配置
                    chatClient.prompt()
                            .system(systemPrompt)
                            .user(userMessage)
                            .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID,conversationId))
                            // 通过ToolContext把traceId传给工具，串联工具调用记录
                            .toolContext(Map.of(PsychologyTools.TRACE_ID_KEY, traceId))
                            .stream()
                            .chatResponse()
                            .doOnNext(chatResponse -> {
                                // 取文本片段并转发
                                if (chatResponse.getResult() != null
                                        && chatResponse.getResult().getOutput() != null) {
                                    String fragment = chatResponse.getResult().getOutput().getText();
                                    if (fragment != null) {
                                        fullaiResponse.append(fragment);
                                        sink.next(fragment);
                                    }
                                }
                                // 记录token用量
                                if (chatResponse.getMetadata() != null
                                        && chatResponse.getMetadata().getUsage() != null) {
                                    lastUsage[0] = chatResponse.getMetadata().getUsage();
                                }
                            })
                            .doOnComplete(() -> {
                                String completeResponse = fullaiResponse.toString();
                                // 打印本次尝试真实token消耗
                                if (lastUsage[0] != null) {
                                    System.out.println("Token统计（第" + attempt[0] + "次尝试）：prompt="
                                            + lastUsage[0].getPromptTokens()
                                            + " completion=" + lastUsage[0].getCompletionTokens()
                                            + " total=" + lastUsage[0].getTotalTokens());
                                    // token埋点
                                    traceService.put(traceId, "promptTokens", lastUsage[0].getPromptTokens());
                                    traceService.put(traceId, "completionTokens", lastUsage[0].getCompletionTokens());
                                    traceService.put(traceId, "totalTokens", lastUsage[0].getTotalTokens());
                                }
                                // 无效调用检测：工具类意图但回复没有工具应返回的关键信息
                                boolean ineffective = isToolIneffective(intentResult.intent(), completeResponse);
                                if (ineffective) {
                                    long invalid = INVALID_TOOL_CALLS.incrementAndGet();
                                    System.out.println("无效调用：意图为" + intentResult.intent()
                                            + "，第" + attempt[0] + "次尝试未含工具结果，累计无效调用" + invalid + "次");
                                }
                                // 自动重试：无效且仍有尝试次数
                                if (ineffective && attempt[0] < maxAttempts) {
                                    attempt[0]++;
                                    // 重置缓冲，最终只保存有效答案
                                    fullaiResponse.setLength(0);
                                    System.out.println("工具结果无效，自动发起第" + attempt[0] + "次尝试...");
                                    traceService.incrementRetry(traceId);
                                    this.run();
                                    return;
                                }
                                String finalResponse = completeResponse;
                                // 重试仍失败：安全兜底，保证用户不拿空回复
                                if (ineffective) {
                                    finalResponse = "暂时没有查询到相关信息，为了你的安全，可直接拨打全国24小时心理援助热线400-161-9995。";
                                    sink.next(finalResponse);
                                }
                                // 将最终有效答案保存到数据库
                                consultationMessageService.saveAiMessage(dbSessionId, finalResponse, "openai");
                                // 添加最终AI回复到记忆体chatMemory
                                List<Message> aiMessages = new ArrayList<>();
                                aiMessages.add(new AssistantMessage(finalResponse));
                                chatMemory.add(conversationId, aiMessages);

                                // 结束trace并落盘
                                traceService.put(traceId, "responseLength", finalResponse.length());
                                traceService.finalizeTrace(traceId, finalResponse,
                                        ineffective ? "FALLBACK" : "SUCCESS");
                                sink.complete();
                            })
                            .doOnError(error -> {
                                // 临时调试：打印大模型API返回的具体错误体，定位400根因
                                if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException wcre) {
                                    System.out.println("===== 大模型API错误 =====");
                                    System.out.println("状态码: " + wcre.getStatusCode());
                                    System.out.println("响应体: " + wcre.getResponseBodyAsString());
                                    System.out.println("==========================");
                                } else {
                                    System.out.println("===== 其他错误: " + error.getClass().getName() + " =====");
                                    error.printStackTrace();
                                }
                                traceService.finalizeTrace(traceId, error.getMessage(), "ERROR");
                                sink.error(error);
                            })
                            .subscribe(); // 订阅并启动流
                }
            };
            runAttempt.run();
        });
    }
    /**
     * 判断工具类意图是否未拿到有效结果
     */
    private static boolean isToolIneffective(IntentType intent, String reply) {
        if (reply == null || reply.isBlank()) {
            return true;
        }
        if (intent == IntentType.HOTLINE_QUERY) {
            // 热线类：回复里应出现电话号码
            return !PHONE_PATTERN.matcher(reply).find();
        }
        if (intent == IntentType.SCALE_QUERY) {
            // 量表类：回复里应出现评分区间（如5-9）或"数字+分"，排除量表名里的数字
            return !java.util.regex.Pattern.compile("\\d+[-~]\\d+|\\d+分").matcher(reply).find();
        }
        return false;
    }

    // 获取参数中的sessionId
    public Long extractSessionId(String sessionId) {
        if (sessionId.startsWith("sesson_") && sessionId != null) {
            String idStr = sessionId.substring("sesson_".length());
            return Long.parseLong(idStr);
        }
        return null;
    }
}
