package com.ceams.module.bpm.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 业务单据详情字段（label/value 两列展示）。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BizDetailItemVO {

    /** 字段名称。 */
    private String label;

    /** 字段值（空值统一为 "-"）。 */
    private String value;
}
