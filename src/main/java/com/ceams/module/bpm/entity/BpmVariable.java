package com.ceams.module.bpm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 流程变量（bpm_variable）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bpm_variable")
public class BpmVariable extends BaseEntity {

    /** 实例 ID。 */
    private Long instanceId;

    /** 变量名。 */
    private String varKey;

    /** 变量值（统一字符串存储）。 */
    private String varValue;
}
