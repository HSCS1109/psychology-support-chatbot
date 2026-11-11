package org.example.aispringboot.AiService;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 摘要压缩记忆：长期滚动摘要 + 近期原始消息
 * 替代纯截断的窗口记忆，避免长对话丢失早期关键背景
 */
@Component
public class SummaryChatMemory implements ChatMemory {

    // 每个会话保留的最近原始消息条数
    private static final int KEEP_RECENT = 10;
    // 原始消息超过该阈值时触发一次压缩
    private static final int COMPRESS_THRESHOLD = 12;

    // 各会话当前保留的原始消息（已被摘要的会移除）
    private final Map<String, List<Message>> store = new ConcurrentHashMap<>();
    // 各会话的滚动摘要
    private final Map<String, String> summaries = new ConcurrentHashMap<>();

    @Autowired
    private OpenAiChatModel openAiChatModel;

    // 摘要专用客户端，不带工具和记忆
    private ChatClient summaryClient;

    @PostConstruct
    public void init() {
        this.summaryClient = ChatClient.create(openAiChatModel);
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Message> list = store.computeIfAbsent(conversationId, k -> new ArrayList<>());
        list.addAll(messages);
        if (list.size() > COMPRESS_THRESHOLD) {
            compress(conversationId, list);
        }
    }

    /**
     * 把早期消息压缩成摘要，只保留最近 KEEP_RECENT 条原始消息
     */
    private synchronized void compress(String conversationId, List<Message> all) {
        int split = all.size() - KEEP_RECENT;
        if (split <= 0) {
            return;
        }
        List<Message> oldMessages = new ArrayList<>(all.subList(0, split));
        String oldSummary = summaries.get(conversationId);
        String newSummary = callSummary(oldSummary, oldMessages);
        summaries.put(conversationId, newSummary);
        // 移除已摘要的原始消息
        all.subList(0, split).clear();
        System.out.println("记忆压缩：摘要了" + oldMessages.size() + "条早期消息，当前摘要" + newSummary.length() + "字");
    }

    /**
     * 调用大模型生成滚动摘要
     */
    private String callSummary(String oldSummary, List<Message> oldMessages) {
        StringBuilder convo = new StringBuilder();
        for (Message m : oldMessages) {
            String role = m.getMessageType() == MessageType.USER ? "用户" : "助手";
            convo.append(role).append("：").append(m.getText()).append("\n");
        }

        String prompt = "请把心理咨询对话压缩成简洁的事实摘要，只保留长期有用的核心信息：用户的主要困扰、持续时间、情绪状态、重要背景和已给出的关键建议。\n"
                + "要求：\n"
                + "1. 摘要总长度控制在300字以内，必须精炼，不要逐句累加。\n"
                + "2. 临时的日常细节（如今天感觉还好、昨天睡了几小时、简短寒暄）可以丢弃，不要保留。\n"
                + "3. 不要添加对话中没有的内容。\n";
        if (oldSummary != null && !oldSummary.isBlank()) {
            prompt += "已有摘要：\n" + oldSummary
                    + "\n请结合已有摘要和下面的新对话，输出更新后的完整摘要，同样控制在300字以内。\n";
        }
        prompt += "对话内容：\n" + convo;

        try {
            return summaryClient.prompt()
                    .options(OpenAiChatOptions.builder()
                            .model("deepseek-ai/DeepSeek-V3")
                            .temperature(0.0)
                            .build())
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            // 摘要失败时保留旧摘要，保证主链路不中断
            return oldSummary != null ? oldSummary : "（早期对话摘要生成失败）";
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        List<Message> result = new ArrayList<>();
        String summary = summaries.get(conversationId);
        if (summary != null && !summary.isBlank()) {
            result.add(new SystemMessage("以下是早期对话的摘要，请结合它理解用户的长期背景：\n" + summary));
        }
        List<Message> recent = store.get(conversationId);
        if (recent != null) {
            result.addAll(recent);
        }
        return result;
    }

    @Override
    public void clear(String conversationId) {
        store.remove(conversationId);
        summaries.remove(conversationId);
    }
}
