package com.ceams.module.integration.enums;

/** 企业微信组织同步状态（integration_wecom_sync.status）。 */
public enum WeComSyncStatus {
    /** 同步中。 */
    RUNNING,
    /** 全部成功。 */
    SUCCESS,
    /** 部分成功。 */
    PARTIAL,
    /** 失败。 */
    FAILED
}
