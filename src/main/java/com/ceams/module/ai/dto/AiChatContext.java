package com.ceams.module.ai.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 对话上下文（前端当前页面，供后续提示词裁剪使用）。 */
@Data
public class AiChatContext {

    /** 前端路由 name，可为空。 */
    @Size(max = 100, message = "页面标识长度不能超过 100")
    private String page;
}
