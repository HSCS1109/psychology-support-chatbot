package org.example.aispringboot.AiService;

/**
 * 用户消息意图分类
 */
public enum IntentType {
    CRISIS("危机干预"),
    SCALE_QUERY("量表查询"),
    HOTLINE_QUERY("热线查询"),
    NORMAL_SUPPORT("普通情绪咨询"),
    CHITCHAT("闲聊/能力询问"),
    OFF_TOPIC("无关问题");

    private final String displayName;

    IntentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
