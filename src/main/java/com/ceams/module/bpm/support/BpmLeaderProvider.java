package com.ceams.module.bpm.support;

import com.ceams.module.bpm.enums.BpmBizType;

/** 审批人解析扩展：按业务单据或发起人解析其"直属上级的系统用户 ID"。实现方不得反向依赖 BPM 内部实现。 */
public interface BpmLeaderProvider {

    /** 按业务类型 + 业务单据 ID 解析业务员工的直属上级系统用户 ID；解析不到返回 null。 */
    Long resolveLeaderUserId(BpmBizType bizType, Long bizId);

    /** 按系统用户 ID 解析其员工档案的直属上级系统用户 ID；解析不到返回 null。 */
    Long resolveLeaderUserIdByUser(Long userId);
}
