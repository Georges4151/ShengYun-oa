package com.ceams.module.hr.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

/** 月度工资核算初始化 / 重算结果：总数、新建、重算、跳过、锁定与告警清单。 */
@Data
@AllArgsConstructor
public class PayrollCalcResultVO {

    /** 核算月（yyyy-MM）。 */
    private String calcMonth;

    /** 命中范围的员工总数。 */
    private int total;

    /** 本次新建的核算记录数。 */
    private int created;

    /** 已存在 DRAFT 被覆盖重算的记录数。 */
    private int recalculated;

    /** 无生效薪资而跳过的员工数。 */
    private int skipped;

    /** 已确认 / 已锁定而跳过的员工数。 */
    private int locked;

    /** 中文告警：哪些员工因何跳过、扣款截断、实发为负等。 */
    private List<String> warnings;
}
