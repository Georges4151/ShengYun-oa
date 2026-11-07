package com.ceams.module.ai.vo;

import com.ceams.module.ai.enums.AiMessageRole;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import lombok.Data;

/** AI 消息视图对象（{@code GET /api/v1/ai/conversations/{id}/messages}）。 */
@Data
public class AiMessageVO {

    /** 消息 ID（雪花 ID 序列化为字符串）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 角色：USER / ASSISTANT / TOOL。 */
    private AiMessageRole role;

    /** 消息内容。 */
    private String content;

    /** 创建时间（沿用全局 ISO8601 序列化）。 */
    private LocalDateTime createTime;
}
