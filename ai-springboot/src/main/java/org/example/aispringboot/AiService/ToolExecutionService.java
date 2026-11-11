package org.example.aispringboot.AiService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * 工具调用统一执行层：
 * 1. 标准化入参（去空格、大小写、别名映射）
 * 2. 统一包装每次调用，记录工具名、入参、状态、耗时
 * 3. 工具异常时返回安全兜底，不中断主流程
 * 4. 累计工具调用统计，供评测与监控
 * 5. 把每次调用推给trace，支持链路回放
 */
@Service
public class ToolExecutionService {

    @Autowired
    private TraceService traceService;

    // 工具调用统计
    private int totalCalls = 0;
    private int successCalls = 0;
    private int fallbackCalls = 0;
    private int emptyParamCalls = 0;

    /**
     * 统一执行工具
     *
     * @param toolName  工具名
     * @param rawParam  模型传入的原始参数
     * @param traceId   当前链路ID（由ToolContext传入，可能为空）
     * @param normalizer 入参标准化逻辑
     * @param logic      真正的工具逻辑
     * @return 工具结果（异常时返回安全兜底）
     */
    public String execute(String toolName, String rawParam, String traceId,
                          UnaryOperator<String> normalizer,
                          Function<String, String> logic) {
        totalCalls++;
        long start = System.currentTimeMillis();
        String param = rawParam;
        String status;
        try {
            // 入参标准化
            if (param == null || param.trim().isEmpty()) {
                emptyParamCalls++;
                param = "";
            } else {
                param = normalizer.apply(param.trim());
            }
            // 执行工具
            String result = logic.apply(param);
            if (result == null || result.trim().isEmpty()) {
                fallbackCalls++;
                result = "抱歉，暂时没有查询到相关信息，我先基于通用经验为你说明。";
                status = "EMPTY_RESULT";
            } else {
                successCalls++;
                status = "SUCCESS";
            }
            long cost = System.currentTimeMillis() - start;
            traceService.addToolCall(traceId, toolName, param, status, cost);
            return result;
        } catch (Exception e) {
            fallbackCalls++;
            long cost = System.currentTimeMillis() - start;
            traceService.addToolCall(traceId, toolName, param, "ERROR", cost);
            System.out.println("工具 " + toolName + " 执行异常: " + e.getMessage());
            return "抱歉，工具查询暂时出了点问题，我先基于通用经验为你解答。";
        }
    }

    public int getTotalCalls() {
        return totalCalls;
    }

    public int getSuccessCalls() {
        return successCalls;
    }

    public int getFallbackCalls() {
        return fallbackCalls;
    }

    public int getEmptyParamCalls() {
        return emptyParamCalls;
    }
}
