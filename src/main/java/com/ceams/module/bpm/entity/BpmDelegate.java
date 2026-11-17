package com.ceams.module.bpm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.bpm.enums.DelegateStatus;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 用户代理关系（bpm_delegate）：委托人在时段内将待办转交给代理人处理。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bpm_delegate")
public class BpmDelegate extends BaseEntity {

    /** 委托人 ID。 */
    private Long userId;

    /** 代理人 ID。 */
    private Long delegateId;

    /** 适用范围：NULL=全部业务，否则为业务类型编码（EXPENSE/CHANGE_REQUEST）。 */
    private String bizType;

    /** 代理开始时间。 */
    private LocalDateTime startTime;

    /** 代理结束时间。 */
    private LocalDateTime endTime;

    /** 状态。 */
    private DelegateStatus status = DelegateStatus.ENABLED;

    /** 代理原因。 */
    private String reason;
}
