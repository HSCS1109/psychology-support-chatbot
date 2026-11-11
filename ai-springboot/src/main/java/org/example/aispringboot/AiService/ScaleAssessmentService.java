package org.example.aispringboot.AiService;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 量表逐题交互测评服务（多轮状态机）：
 * 与 getScaleInfo 的静态介绍不同，这里维护每个会话的测评状态，
 * 一次只发一题，校验用户输入（0-3），自动累计分数并在结束时给出结果。
 * 状态保存在内存（本地单用户场景），重启清空，不新建数据库表。
 */
@Service
public class ScaleAssessmentService {

    /** PHQ-9 九道标准题目（回忆最近两周） */
    private static final List<String> PHQ9_QUESTIONS = List.of(
            "做事时提不起劲或没有兴趣",
            "感到心情低落、沮丧或绝望",
            "入睡困难、睡不安稳或睡眠过多",
            "感觉疲倦或没有活力",
            "食欲不振或吃太多",
            "觉得自己很糟，或觉得自己很失败，或让自己、家人失望",
            "对事物专注有困难，例如阅读、看电视时",
            "动作或说话速度缓慢到别人可以察觉，或相反地烦躁、坐立不安",
            "有不如死掉或用某种方式伤害自己的念头"
    );

    /** 启动测评的动作词 */
    private static final Pattern START_PATTERN = Pattern.compile(
            "我要测|开始测|测一下|做.*测评|做.*测试|做一下|做个|评估一下|走一遍|做题|答题|自测"
    );
    /** 指向 PHQ-9 / 抑郁 */
    private static final Pattern PHQ9_PATTERN = Pattern.compile(
            "PHQ-9|PHQ9|抑郁"
    );
    /** 中途退出 */
    private static final Pattern QUIT_PATTERN = Pattern.compile(
            "退出测评|退出|不做了|取消|结束测评"
    );

    /**
     * 一次测评交互的返回结果
     *
     * @param text       回复文本（题目/重问/最终报告）
     * @param finished   本轮是否已结束（完成或退出）
     * @param totalScore 总分（仅完成时有值）
     * @param level      结果等级（仅完成时有值）
     */
    public record AssessmentReply(String text, boolean finished,
                                  Integer totalScore, String level) {
    }

    /** 单会话测评状态 */
    private static class AssessmentState {
        int currentIndex = 0;
        final List<Integer> scores = new ArrayList<>();
        final long startTime = System.currentTimeMillis();
    }

    // 内存态：会话ID -> 测评状态
    private final Map<String, AssessmentState> active = new ConcurrentHashMap<>();

    public boolean isInProgress(String sessionId) {
        return active.containsKey(sessionId);
    }

    public boolean isQuitMessage(String message) {
        return message != null && QUIT_PATTERN.matcher(message.trim()).find();
    }

    /**
     * 判断消息是否要启动 PHQ-9 测评：
     * 同时含动作词且指向 PHQ-9/抑郁，避免和"量表介绍"查询混淆
     */
    public boolean tryStart(String message) {
        if (message == null) {
            return false;
        }
        String m = message.trim();
        return START_PATTERN.matcher(m).find() && PHQ9_PATTERN.matcher(m).find();
    }

    /**
     * 启动测评，返回第一题
     */
    public AssessmentReply start(String sessionId) {
        AssessmentState state = new AssessmentState();
        active.put(sessionId, state);
        return new AssessmentReply(buildQuestion(state), false, null, null);
    }

    /**
     * 处理用户回答：非法输入重问，合法则计分并出下一题或最终报告
     */
    public AssessmentReply handleAnswer(String sessionId, String message) {
        AssessmentState state = active.get(sessionId);
        if (state == null) {
            return null;
        }
        String m = message.trim();
        if (!m.matches("[0-3]")) {
            // 不推进，重问当前题
            return new AssessmentReply(
                    "请只输入 0~3 之间的数字来回答哦。\n" + buildQuestion(state),
                    false, null, null);
        }
        state.scores.add(Integer.parseInt(m));
        state.currentIndex++;

        if (state.currentIndex >= PHQ9_QUESTIONS.size()) {
            return buildResult(sessionId, state);
        }
        return new AssessmentReply(buildQuestion(state), false, null, null);
    }

    /**
     * 中途退出并清理
     */
    public AssessmentReply quit(String sessionId) {
        active.remove(sessionId);
        return new AssessmentReply("已退出本次测评，如果之后想测随时告诉我。", true, null, null);
    }

    private String buildQuestion(AssessmentState state) {
        int no = state.currentIndex + 1;
        return "第" + no + "题（共9题）：" + PHQ9_QUESTIONS.get(state.currentIndex) + "\n"
                + "0分：完全没有　1分：几天　2分：一半以上天数　3分：几乎每天";
    }

    private AssessmentReply buildResult(String sessionId, AssessmentState state) {
        int total = state.scores.stream().mapToInt(Integer::intValue).sum();
        int selfHarmScore = state.scores.get(8); // 第9题

        String level;
        if (total <= 4) {
            level = "无明显抑郁（0-4分）";
        } else if (total <= 9) {
            level = "轻度抑郁（5-9分）";
        } else if (total <= 14) {
            level = "中度抑郁（10-14分）";
        } else if (total <= 19) {
            level = "中重度抑郁（15-19分）";
        } else {
            level = "重度抑郁（20-27分）";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("测评完成！你的 PHQ-9 总分为：").append(total).append(" 分\n");
        sb.append("结果参考：").append(level).append("\n");
        sb.append("各题得分：").append(state.scores).append("\n");
        if (total >= 10) {
            sb.append("建议：结果达到中度及以上，建议联系学校心理咨询中心或专业医生做进一步评估。\n");
        }
        // 安全联动：第9题有自伤念头（非0分）时插入危机提示
        if (selfHarmScore > 0) {
            sb.append("\n安全提示：你在“是否有伤害自己的念头”这一题选择了非0分，请重视这个信号。\n");
            sb.append("如果现在很难受，请立即拨打全国24小时心理援助热线：400-161-9995，");
            sb.append("或联系身边信任的人陪伴你。\n");
        }
        sb.append("\n温馨提示：本测评仅为自评筛查工具，不能替代专业诊断。");

        active.remove(sessionId);
        return new AssessmentReply(sb.toString(), true, total, level);
    }
}
