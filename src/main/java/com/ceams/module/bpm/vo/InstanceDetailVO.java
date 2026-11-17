package com.ceams.module.bpm.vo;

import java.util.List;
import java.util.Map;
import lombok.Data;

/** 流程实例详情视图。 */
@Data
public class InstanceDetailVO {

    /** 实例概要。 */
    private InstanceVO instance;

    /** 节点与任务。 */
    private List<NodeVO> nodes;

    /** 流程变量。 */
    private Map<String, String> variables;

    /** 抄送记录。 */
    private List<CcRecordVO> ccRecords;
}
