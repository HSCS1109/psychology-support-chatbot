package org.example.aispringboot.AiService;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * 意图识别服务：规则 + 模型两层
 * 规则层零延迟命中高确定性意图，模型层兜底处理模糊表达
 */
@Service
public class IntentRecognitionService {

    /**
     * 意图识别结果
     *
     * @param intent     意图
     * @param source     命中方式：rule（规则）/ model（模型）/ fallback（异常兜底）
     * @param confidence 置信度 0~1
     */
    public record IntentResult(IntentType intent, String source, double confidence) {
    }

    // 量表相关：量表名 + 评分/分数词
    private static final Pattern SCALE_PATTERN =
            Pattern.compile("PHQ-9|SDS|GAD-7|SAS|量表|评分|测出来|多少分|几分");
    // 热线相关
    private static final Pattern HOTLINE_PATTERN =
            Pattern.compile("热线|援助电话|咨询电话|心理咨询电话");
    // 闲聊/能力询问
    private static final Pattern CHITCHAT_PATTERN =
            Pattern.compile("你好|在吗|你是谁|你能做什么|你会什么|你能帮我做什么|介绍一下你");

    @Autowired
    private OpenAiChatModel openAiChatModel;

    // 分类专用客户端：不带工具、不带记忆，避免分类时误触发工具
    private ChatClient classifyClient;

    @PostConstruct
    public void init() {
        this.classifyClient = ChatClient.create(openAiChatModel);
    }

    /**
     * 识别消息意图
     */
    public IntentResult recognize(String message) {
        IntentResult ruleResult = matchByRule(message);
        if (ruleResult != null) {
            return ruleResult;
        }
        return matchByModel(message);
    }

    /**
     * 第一层：规则匹配
     */
    private IntentResult matchByRule(String message) {
        if (SCALE_PATTERN.matcher(message).find()) {
            return new IntentResult(IntentType.SCALE_QUERY, "rule", 0.95);
        }
        if (HOTLINE_PATTERN.matcher(message).find()) {
            return new IntentResult(IntentType.HOTLINE_QUERY, "rule", 0.95);
        }
        if (CHITCHAT_PATTERN.matcher(message).find()) {
            return new IntentResult(IntentType.CHITCHAT, "rule", 0.9);
        }
        return null;
    }

    /**
     * 第二层：模型分类，结构化返回意图枚举
     */
    private IntentResult matchByModel(String message) {
        try {
            String instruction = "你是一个意图分类器，请判断用户消息属于下列哪个意图，只返回对应的英文枚举名称，不要解释：\n"
                    + "CRISIS：出现明确的自杀或自伤意图、计划、行为，或直接表达不想活、结束生命、提到具体自伤方式（如割腕、跳楼、吃药）\n"
                    + "SCALE_QUERY：心理量表、评分、分数相关查询\n"
                    + "HOTLINE_QUERY：心理援助热线、咨询电话查询\n"
                    + "NORMAL_SUPPORT：情绪倾诉、心理困扰、压力，以及“撑不下去、好难、没意义”等没有明确自伤计划的模糊消极表达\n"
                    + "CHITCHAT：问候、询问助手能力\n"
                    + "OFF_TOPIC：与心理健康完全无关的问题\n"
                    + "注意：单纯表达疲惫、艰难、撑不下去，但没有明确自杀或自伤计划时，归 NORMAL_SUPPORT，不要判 CRISIS。\n"
                    + "用户消息：" + message;

            String raw = classifyClient.prompt()
                    .options(OpenAiChatOptions.builder()
                            .model("deepseek-ai/DeepSeek-V3")
                            .temperature(0.0)
                            .build())
                    .user(instruction)
                    .call()
                    .content();

            return new IntentResult(parseEnum(raw), "model", 0.7);
        } catch (Exception e) {
            // 模型异常时保守归为普通咨询，保证主链路不中断
            return new IntentResult(IntentType.NORMAL_SUPPORT, "fallback", 0.3);
        }
    }

    /**
     * 从模型返回文本中解析意图，兼容携带解释的情况
     */
    private IntentType parseEnum(String raw) {
        if (raw == null) {
            return IntentType.NORMAL_SUPPORT;
        }
        String upper = raw.toUpperCase();
        for (IntentType type : IntentType.values()) {
            if (upper.contains(type.name())) {
                return type;
            }
        }
        return IntentType.NORMAL_SUPPORT;
    }
}
