package com.ceams.module.ai.enums;

/** AI 消息角色（代码值落库，不落中文）。 */
public enum AiMessageRole {

    /** 用户提问。 */
    USER,

    /** 模型回答。 */
    ASSISTANT,

    /** 只读工具返回结果。 */
    TOOL;

    /** 按代码值（忽略大小写）解析；无法识别时回退为 {@link #USER}，保证审计不阻断业务。 */
    public static AiMessageRole fromCode(String code) {
        if (code == null || code.isBlank()) {
            return USER;
        }
        String normalized = code.trim();
        for (AiMessageRole role : values()) {
            if (role.name().equalsIgnoreCase(normalized)) {
                return role;
            }
        }
        return USER;
    }
}
