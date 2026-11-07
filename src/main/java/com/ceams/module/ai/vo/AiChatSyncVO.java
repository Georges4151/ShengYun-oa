package com.ceams.module.ai.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;
import lombok.Data;

/** 非流式对话结果（{@code POST /api/v1/ai/chat/sync}）：一次性返回完整回答。 */
@Data
public class AiChatSyncVO {

    /** 会话 ID（雪花 ID 序列化为字符串，避免前端精度丢失）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long conversationId;

    /** 助手消息 ID（落库失败时为 null）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long messageId;

    /** 完整回答文本。 */
    private String answer;

    /** 模型名，如 {@code qwen2.5:7b}。 */
    private String model;

    /** 服务端处理总耗时（毫秒）。 */
    private long latencyMs;

    /** 本次回答过程中执行的工具调用（无调用时为空列表）。 */
    private List<AiToolCallVO> toolCalls;
}
