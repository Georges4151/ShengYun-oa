package com.ceams.module.integration.vo;

import lombok.Data;

/** 企业微信组织同步结果。 */
@Data
public class WeComSyncResultVO {

    /** 同步批次号。 */
    private String batchNo;

    /** 状态：SUCCESS/PARTIAL/FAILED。 */
    private String status;

    /** 同步对象总数。 */
    private Integer total;

    /** 成功数。 */
    private Integer successCount;

    /** 失败数。 */
    private Integer failedCount;

    /** 失败原因。 */
    private String errorMsg;
}
