package com.ceams.module.bpm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.bpm.enums.CcReadStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 抄送记录（bpm_cc_record）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bpm_cc_record")
public class BpmCcRecord extends BaseEntity {

    /** 实例 ID。 */
    private Long instanceId;

    /** 节点 ID。 */
    private Long nodeId;

    /** 抄送接收人 ID。 */
    private Long receiverId;

    /** 阅读状态。 */
    private CcReadStatus readStatus = CcReadStatus.UNREAD;
}
