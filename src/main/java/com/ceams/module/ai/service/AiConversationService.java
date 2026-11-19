package com.ceams.module.ai.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.ai.dto.AiFeedbackRequest;
import com.ceams.module.ai.vo.AiConversationVO;
import com.ceams.module.ai.vo.AiMessageVO;

/** AI 会话查询与消息评价服务（仅允许访问本人会话）。 */
public interface AiConversationService {

    /** 当前用户的会话列表（按 updateTime 倒序）。 */
    PageResult<AiConversationVO> conversations(long page, long size);

    /** 指定会话的消息列表（按时间正序；校验会话归属）。 */
    PageResult<AiMessageVO> messages(Long conversationId, long page, long size);

    /** 提交消息评价（校验消息所属会话属于当前用户）。 */
    void feedback(AiFeedbackRequest request);
}
