package org.example.aispringboot.AiService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 安全护栏服务：在用户消息进入大模型前进行危机干预拦截。
 * 命中自杀/自残等高危信号时，不调用大模型，直接返回固定求助话术，
 * 避免模型生成不当回应，同时节省token、保证合规。
 */
@Service
public class SafetyGuardService {

    private static final Logger log = LoggerFactory.getLogger(SafetyGuardService.class);

    /** 危机关键词正则：覆盖常见自杀/自伤表达 */
    private static final List<Pattern> CRISIS_PATTERNS = List.of(
            Pattern.compile("自杀|自残|自伤|割腕|跳楼|卧轨|上吊|烧炭"),
            Pattern.compile("不想活|不想活了|我想死|我要自杀|想结束生命|了结自己"),
            Pattern.compile("活着没意义|活着没意思|活着太累|没有活下去的理由"),
            Pattern.compile("吃.*安眠药|安眠药.*睡过去|跳楼|跳下去|结束一切")
    );

    /** 危机干预固定话术：包含24小时热线与紧急处理建议 */
    private static final String CRISIS_RESPONSE =
            "我感受到你现在非常痛苦，谢谢你愿意把这些告诉我。你现在的感受是真实的、可以被理解的，但请相信——这种痛苦不会永远持续下去。\n\n" +
            "你不是一个人在面对这些，请现在就做一件事：\n" +
            "• 全国24小时心理援助热线：400-161-9995\n" +
            "• 北京心理危机研究与干预中心：010-82951332\n" +
            "• 如果情况紧急，请立即拨打 120 或前往最近医院急诊\n" +
            "• 联系你身边信任的家人、朋友或辅导员\n\n" +
            "请现在就拨打一个电话。你值得被帮助，也值得被好好对待。";

    /**
     * 判断用户消息是否包含危机信号
     */
    public boolean isCrisis(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String text = message.toLowerCase();
        boolean hit = CRISIS_PATTERNS.stream().anyMatch(p -> p.matcher(text).find());
        if (hit) {
            log.warn("[SafetyGuard] 命中危机关键词，消息已拦截，contentLength={}", message.length());
        }
        return hit;
    }

    /**
     * 返回危机干预固定话术（不经过大模型）
     */
    public String getCrisisResponse() {
        return CRISIS_RESPONSE;
    }
}
