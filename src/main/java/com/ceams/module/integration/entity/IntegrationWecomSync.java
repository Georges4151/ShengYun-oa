package com.ceams.module.integration.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.integration.enums.WeComSyncStatus;
import com.ceams.module.integration.enums.WeComSyncType;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 企业微信组织同步批次（integration_wecom_sync）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("integration_wecom_sync")
public class IntegrationWecomSync extends BaseEntity {

    /** 同步批次号（唯一，形如 WECOM-yyyyMMddHHmmss-1234）。 */
    private String syncBatchNo;

    /** 触发方式。 */
    private WeComSyncType syncType;

    /** 同步对象总数（部门 + 成员）。 */
    private Integer total = 0;

    /** 成功数。 */
    private Integer successCount = 0;

    /** 失败数。 */
    private Integer failedCount = 0;

    /** 状态。 */
    private WeComSyncStatus status;

    /** 失败原因（截断 1000 字符）。 */
    private String errorMsg;

    /** 开始时间。 */
    private LocalDateTime startedAt;

    /** 结束时间。 */
    private LocalDateTime finishedAt;
}
