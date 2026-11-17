package com.ceams.module.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.hr.enums.PeriodType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** HR 绩效模板（hr_performance_template）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_performance_template")
public class HrPerformanceTemplate extends BaseEntity {

    /** 模板编码（唯一）。 */
    private String code;

    /** 模板名称。 */
    private String name;

    /** 适用岗位 ID（逻辑引用 hr_position.id，NULL 表示通用）。 */
    private Long positionId;

    /** 考核周期类型。 */
    private PeriodType periodType;

    /** 考评项 JSON：[{name,weight,minScore,maxScore,description}]。 */
    private String itemsJson;

    /** 系数规则 JSON：[{minScore,maxScore,coefficient}]。 */
    private String scoreRulesJson;

    /** 权重合计（冗余校验，写入时须等于 100）。 */
    private Integer totalWeight;

    /** 状态：ENABLED/DISABLED。 */
    private String status;
}
