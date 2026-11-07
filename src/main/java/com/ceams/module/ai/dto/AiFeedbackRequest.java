package com.ceams.module.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 消息评价请求（{@code POST /api/v1/ai/feedback}）。 */
@Data
public class AiFeedbackRequest {

    /** 被评价消息 ID（字符串形式的雪花 ID）。 */
    @NotBlank(message = "messageId 不能为空")
    private String messageId;

    /** 评分：1 点赞 / -1 点踩（业务层还会二次校验该取值范围）。 */
    @NotNull(message = "评分不能为空")
    private Integer score;

    /** 备注，可选。 */
    @Size(max = 200, message = "备注长度不能超过 200")
    private String remark;
}
