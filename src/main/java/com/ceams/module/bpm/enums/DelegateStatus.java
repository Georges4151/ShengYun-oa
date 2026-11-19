package com.ceams.module.bpm.enums;

/** 用户代理状态（bpm_delegate.status）。 */
public enum DelegateStatus {
    /** 生效中（是否处于代理时段由 start_time/end_time 判定）。 */
    ENABLED,
    /** 已停用（用户主动取消）。 */
    DISABLED
}
