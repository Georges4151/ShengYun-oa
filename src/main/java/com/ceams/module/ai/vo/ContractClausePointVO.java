package com.ceams.module.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 合同要点条目。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractClausePointVO {

    /** 要点名称。 */
    private String label;

    /** 要点取值。 */
    private String value;
}
