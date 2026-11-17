package com.ceams.module.hr.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 绩效考评单批量生成结果：总人数 / 新建数 / 跳过数（已存在考评单）。 */
@Data
@AllArgsConstructor
public class PerformanceBatchResultVO {

    /** 命中范围的员工总数。 */
    private int total;

    /** 本次新建的考评单数量。 */
    private int created;

    /** 已存在考评单而跳过的数量。 */
    private int skipped;
}
