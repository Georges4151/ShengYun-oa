package com.ceams.module.bpm.support;

import com.ceams.module.bpm.enums.BpmAssigneeSource;
import lombok.Data;

/** 模板 JSON 中的审批人来源配置。 */
@Data
public class BpmAssigneeSourceConfig {

    /** 来源类型。 */
    private BpmAssigneeSource type;

    /** 来源值：USER=用户ID；ROLE=角色编码；DEPT_LEADER/SUPERIOR 可空。 */
    private String value;
}
