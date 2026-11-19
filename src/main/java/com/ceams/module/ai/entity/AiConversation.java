package com.ceams.module.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** AI 会话（ai_conversation）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_conversation")
public class AiConversation extends BaseEntity {

    /** 所属用户ID（逻辑引用 sys_user.id）。 */
    private Long userId;

    /** 会话标题。 */
    private String title;
}
