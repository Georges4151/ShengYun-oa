package com.ceams.module.bpm.support;

import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.enums.BpmInstanceStatus;

/**
 * 业务流程回调：业务模块通过实现本接口，把审批结果写回业务单据。
 *
 * <p>审批状态的唯一来源是 {@code bpm_instance}；业务表上的状态字段仅作为派生的快照，
 * 只能由本回调写入，业务侧不再自行修改，避免出现"双状态"。
 *
 * <p>回调在审批事务内同步执行，异常会向上抛出并回滚审批动作，保证业务快照与实例状态一致。
 */
public interface BpmBizCallback {

    /** 该回调负责的业务类型。 */
    BpmBizType bizType();

    /**
     * 审批节点完成（会签最后一人同意、或签任一同意）。
     *
     * @param instance 流程实例
     * @param nodeKey 完成的节点编码
     */
    default void onNodeCompleted(BpmInstance instance, String nodeKey) {}

    /**
     * 实例终态收口（通过/驳回/撤回/终止）。
     *
     * @param instance 流程实例
     * @param status 终态
     */
    default void onInstanceFinished(BpmInstance instance, BpmInstanceStatus status) {}
}
