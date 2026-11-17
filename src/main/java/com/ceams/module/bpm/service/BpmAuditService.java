package com.ceams.module.bpm.service;

import com.ceams.module.bpm.enums.AuditAction;

/** BPM 审计快照服务：记录流程动作与模板发布的前后 JSON。 */
public interface BpmAuditService {

    /** 目标类型：模板。 */
    String TARGET_TEMPLATE = "BPM_TEMPLATE";

    /** 目标类型：实例。 */
    String TARGET_INSTANCE = "BPM_INSTANCE";

    /** 目标类型：任务。 */
    String TARGET_TASK = "BPM_TASK";

    /**
     * 写入一条审计快照。
     *
     * @param targetType 目标类型
     * @param targetId 目标 ID
     * @param action 动作
     * @param before 变更前对象（可为 null）
     * @param after 变更后对象（可为 null）
     * @param maskedFields 脱敏字段清单（可为 null）
     * @param operatorId 操作人 ID
     * @return traceId（写入审计并从日志可追踪）
     */
    String record(
            String targetType,
            Long targetId,
            AuditAction action,
            Object before,
            Object after,
            String maskedFields,
            Long operatorId);
}
