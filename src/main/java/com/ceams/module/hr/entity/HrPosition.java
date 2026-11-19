package com.ceams.module.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** HR 岗位（hr_position）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_position")
public class HrPosition extends BaseEntity {

    /** 岗位编码（唯一）。 */
    private String code;

    /** 岗位名称。 */
    private String name;

    /** 岗位职级。 */
    private String level;

    /** 岗位序列。 */
    private String jobFamily;

    /** 排序号。 */
    private Integer sortOrder;

    /** 状态：ENABLED/DISABLED。 */
    private String status;

    /** 备注。 */
    private String remark;
}
