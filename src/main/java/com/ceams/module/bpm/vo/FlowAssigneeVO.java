package com.ceams.module.bpm.vo;

import com.ceams.module.bpm.enums.BpmAssigneeSource;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/** 流程节点审批人（已解析或预计）。 */
@Data
public class FlowAssigneeVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 审批人姓名；解析不到时为空。 */
    private String name;

    private BpmAssigneeSource sourceType;
}
