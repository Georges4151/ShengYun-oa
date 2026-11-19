package com.ceams.module.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** AI 用户反馈（ai_feedback）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_feedback")
public class AiFeedback extends BaseEntity {

    /** 被评价消息ID（逻辑引用 ai_message.id）。 */
    private Long messageId;

    /** 评价用户ID（逻辑引用 sys_user.id）。 */
    private Long userId;

    /** 评分：1~5。 */
    private Integer score;

    /** 备注。 */
    private String remark;
}
