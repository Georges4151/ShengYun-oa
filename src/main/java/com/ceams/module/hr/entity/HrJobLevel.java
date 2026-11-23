package com.ceams.module.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** HR 职级字典（hr_job_level）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_job_level")
public class HrJobLevel extends BaseEntity {

    /** 职级编码（唯一）。 */
    private String code;

    /** 职级名称。 */
    private String name;

    /** 职级序列。 */
    private String jobFamily;

    /** 职级序号（数值越大职级越高）。 */
    private Integer rankNo;

    /** 排序号。 */
    private Integer sortOrder;

    /** 状态：ENABLED/DISABLED。 */
    private String status;

    /** 备注。 */
    private String remark;
}
