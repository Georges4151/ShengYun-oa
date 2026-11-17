package com.ceams.module.hr.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

/** 工资条发布结果：总数、发布、跳过与中文告警清单。 */
@Data
@AllArgsConstructor
public class SalarySlipPublishResultVO {

    /** 核算月（yyyy-MM）。 */
    private String calcMonth;

    /** 该月命中的核算记录总数（按 employeeIds 过滤后）。 */
    private int total;

    /** 本次新发布的工资条数。 */
    private int published;

    /** 因未确认、已锁定或工资条已存在而跳过的记录数。 */
    private int skipped;

    /** 中文告警：未确认 / 已锁定记录被跳过等。 */
    private List<String> warnings;
}
