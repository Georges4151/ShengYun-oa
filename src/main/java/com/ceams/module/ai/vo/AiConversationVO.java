package com.ceams.module.ai.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import lombok.Data;

/** AI 会话视图对象（{@code GET /api/v1/ai/conversations}）。 */
@Data
public class AiConversationVO {

    /** 会话 ID（雪花 ID 序列化为字符串）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 会话标题。 */
    private String title;

    /** 创建时间（沿用全局 ISO8601 序列化）。 */
    private LocalDateTime createTime;

    /** 更新时间（会话列表按此倒序）。 */
    private LocalDateTime updateTime;
}
