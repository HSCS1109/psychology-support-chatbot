package org.example.aispringboot.AiService;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 心理咨询相关工具：心理量表信息、心理援助热线
 */
@Service
public class PsychologyTools {

    public static final String TRACE_ID_KEY = "traceId";

    @Autowired
    private ToolExecutionService toolExecutionService;

    @Tool(description = "查询常用心理量表的信息，包括适用场景、题目数量、评分标准和结果解读。当用户询问量表、测评、PHQ-9、SDS、GAD-7、SAS相关内容时调用。")
    public String getScaleInfo(
            @ToolParam(description = "量表名称，如：PHQ-9、SDS、GAD-7、SAS")
            String scaleName,
            ToolContext toolContext) {
        String traceId = getTraceId(toolContext);
        return toolExecutionService.execute(
                "getScaleInfo",
                scaleName,
                traceId,
                this::normalizeScaleName,
                this::doGetScaleInfo
        );
    }

    @Tool(description = "查询全国及各城市心理援助热线电话。当用户需要心理咨询热线、危机干预电话、求助电话时调用。")
    public String getCounselingHotline(
            @ToolParam(description = "城市名称，如：北京、上海、广州、深圳、大连；查询全国热线传全国")
            String city,
            ToolContext toolContext) {
        String traceId = getTraceId(toolContext);
        return toolExecutionService.execute(
                "getCounselingHotline",
                city,
                traceId,
                this::normalizeCity,
                this::doGetHotline
        );
    }

    /**
     * 从ToolContext取traceId，没有则返回空串
     */
    private String getTraceId(ToolContext toolContext) {
        if (toolContext == null) {
            return "";
        }
        Map<String, Object> ctx = toolContext.getContext();
        Object id = ctx.get(TRACE_ID_KEY);
        return id == null ? "" : id.toString();
    }

    /**
     * 量表名标准化：兼容中文别名、大小写、空格
     */
    private String normalizeScaleName(String raw) {
        String name = raw.trim().toUpperCase();
        // 中文别名映射
        if (name.contains("抑郁") || name.contains("PHQ")) {
            return "PHQ-9";
        }
        if (name.contains("焦虑自评") || name.equals("SAS")) {
            return "SAS";
        }
        if (name.contains("GAD") || name.contains("广泛焦虑")) {
            return "GAD-7";
        }
        if (name.contains("抑郁自评") || name.equals("SDS")) {
            return "SDS";
        }
        return name;
    }

    /**
     * 城市名标准化：去空格、去"市"
     */
    private String normalizeCity(String raw) {
        String city = raw.trim();
        if (city.endsWith("市")) {
            city = city.substring(0, city.length() - 1);
        }
        if (city.equals("全国") || city.contains("全国") || city.equals("国家")) {
            return "全国";
        }
        return city;
    }

    private String doGetScaleInfo(String scaleName) {
        return switch (scaleName) {
            case "PHQ-9" -> """
                    【PHQ-9 抑郁症筛查量表】
                    1. 适用场景：抑郁症状初步筛查与严重程度评估
                    2. 题目数量：9道题
                    3. 评分标准：每题0-3分，总分0-27分
                    4. 结果解读：
                       - 0-4分：无明显抑郁
                       - 5-9分：轻度抑郁
                       - 10-14分：中度抑郁
                       - 15-19分：中重度抑郁
                       - 20-27分：重度抑郁
                    5. 建议：中度及以上建议寻求专业心理帮助
                    """;
            case "SDS" -> """
                    【SDS 抑郁自评量表】
                    1. 适用场景：抑郁症状的自我评估
                    2. 题目数量：20道题
                    3. 评分标准：标准分=粗分×1.25，取整数
                    4. 结果解读：
                       - 53分以下：正常
                       - 53-62分：轻度抑郁
                       - 63-72分：中度抑郁
                       - 73分以上：重度抑郁
                    """;
            case "GAD-7" -> """
                    【GAD-7 广泛性焦虑量表】
                    1. 适用场景：焦虑症状筛查与严重程度评估
                    2. 题目数量：7道题
                    3. 评分标准：每题0-3分，总分0-21分
                    4. 结果解读：
                       - 0-4分：无明显焦虑
                       - 5-9分：轻度焦虑
                       - 10-14分：中度焦虑
                       - 15-21分：重度焦虑
                    """;
            case "SAS" -> """
                    【SAS 焦虑自评量表】
                    1. 适用场景：焦虑症状的自我评估
                    2. 题目数量：20道题
                    3. 评分标准：标准分=粗分×1.25，取整数
                    4. 结果解读：
                       - 50分以下：正常
                       - 50-59分：轻度焦虑
                       - 60-69分：中度焦虑
                       - 70分以上：重度焦虑
                    """;
            default -> "抱歉，暂未收录该量表（" + scaleName + "）的信息。目前支持查询：PHQ-9、SDS、GAD-7、SAS。";
        };
    }

    private String doGetHotline(String city) {
        String national = """
                【全国心理援助热线】
                1. 全国心理援助热线：400-161-9995（24小时）
                2. 北京心理危机研究与干预中心：010-82951332
                3. 希望24热线：400-161-9995
                """;
        String cityLine = switch (city) {
            case "北京" -> "4. 北京心理援助热线：010-82951332";
            case "上海" -> "4. 上海心理援助热线：021-12320-5";
            case "广州" -> "4. 广州心理援助热线：020-81899120";
            case "深圳" -> "4. 深圳心理援助热线：0755-25629459";
            case "大连" -> "4. 大连心理援助热线：0411-39580000";
            default -> null;
        };
        if (cityLine == null) {
            return national + "5. 暂未收录" + city + "本地热线，可先拨打全国热线。";
        }
        return national + cityLine;
    }
}
