package com.ceams.module.integration.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import lombok.Data;

/** 企业微信组织同步批次展示对象。 */
@Data
public class WeComSyncLogVO {

    /** 批次主键（序列化为字符串，避免前端精度丢失）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 同步批次号。 */
    private String batchNo;

    /** 触发方式：MANUAL/SCHEDULED。 */
    private String syncType;

    /** 同步对象总数。 */
    private Integer total;

    /** 成功数。 */
    private Integer successCount;

    /** 失败数。 */
    private Integer failedCount;

    /** 状态：RUNNING/SUCCESS/PARTIAL/FAILED。 */
    private String status;

    /** 失败原因。 */
    private String errorMsg;

    /** 开始时间。 */
    private LocalDateTime startedAt;

    /** 结束时间。 */
    private LocalDateTime finishedAt;
}
