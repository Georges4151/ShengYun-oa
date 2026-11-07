package com.ceams.module.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 流式对话请求（{@code POST /api/v1/ai/chat}）。 */
@Data
public class AiChatRequest {

    /** 已有会话 ID；null 表示新建会话。 */
    private String conversationId;

    /** 用户问题（1~2000 字）。 */
    @NotBlank(message = "问题不能为空")
    @Size(max = 2000, message = "问题长度不能超过 2000")
    private String message;

    /** 前端上下文，可选。 */
    @Valid private AiChatContext context;
}
