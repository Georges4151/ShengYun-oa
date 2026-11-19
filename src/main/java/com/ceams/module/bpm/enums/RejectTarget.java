package com.ceams.module.bpm.enums;

/** 驳回目标。 */
public enum RejectTarget {
    /** 驳回发起人（实例回到首节点，生成发起人待办）。 */
    INITIATOR,
    /** 驳回上一级（实例回到上一个已完成节点）。 */
    PREVIOUS
}
