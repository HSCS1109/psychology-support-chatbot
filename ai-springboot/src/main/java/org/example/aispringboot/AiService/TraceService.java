package org.example.aispringboot.AiService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轻量链路追踪：每次对话生成traceId，按ID收集完整链路，
 * 结束时以JSONL格式落盘（每行一条），不依赖数据库，可回放归因。
 */
@Service
public class TraceService {

    // 相对工作目录：ai-springboot/logs/trace/trace.jsonl
    private static final Path TRACE_FILE = Paths.get("logs", "trace", "trace.jsonl");

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 进行中的trace：traceId -> 记录
    private final Map<String, Map<String, Object>> activeTraces = new ConcurrentHashMap<>();

    /**
     * 开始一次trace，返回traceId
     */
    public String startTrace(String sessionId, String userInput) {
        String traceId = UUID.randomUUID().toString();
        Map<String, Object> rec = new LinkedHashMap<>();
        rec.put("traceId", traceId);
        rec.put("startTime", new Date().toString());
        rec.put("sessionId", sessionId);
        rec.put("userInput", userInput);
        rec.put("toolCalls", new ArrayList<Map<String, Object>>());
        rec.put("retryCount", 0);
        activeTraces.put(traceId, rec);
        return traceId;
    }

    /**
     * 通用字段埋点
     */
    public void put(String traceId, String key, Object value) {
        Map<String, Object> rec = activeTraces.get(traceId);
        if (rec != null) {
            rec.put(key, value);
        }
    }

    /**
     * 重试次数+1
     */
    public void incrementRetry(String traceId) {
        Map<String, Object> rec = activeTraces.get(traceId);
        if (rec != null) {
            int c = (int) rec.getOrDefault("retryCount", 0);
            rec.put("retryCount", c + 1);
        }
    }

    /**
     * 追加一次工具调用记录
     */
    public void addToolCall(String traceId, String toolName, String param,
                            String status, long durationMs) {
        Map<String, Object> rec = activeTraces.get(traceId);
        if (rec != null) {
            Map<String, Object> tool = new LinkedHashMap<>();
            tool.put("toolName", toolName);
            tool.put("param", param);
            tool.put("status", status);
            tool.put("durationMs", durationMs);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tools = (List<Map<String, Object>>) rec.get("toolCalls");
            tools.add(tool);
        }
    }

    /**
     * 结束trace并落盘
     */
    public void finalizeTrace(String traceId, String finalResponse, String status) {
        Map<String, Object> rec = activeTraces.remove(traceId);
        if (rec == null) {
            return;
        }
        rec.put("endTime", new Date().toString());
        rec.put("finalResponse", finalResponse);
        rec.put("status", status);
        try {
            Files.createDirectories(TRACE_FILE.getParent());
            String line = objectMapper.writeValueAsString(rec);
            Files.writeString(TRACE_FILE, line + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("trace落盘失败: " + e.getMessage());
        }
    }
}
