package com.ceams.module.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.hr.enums.ContractStatus;
import com.ceams.module.hr.enums.ContractType;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** HR 劳动合同（hr_contract）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_contract")
public class HrContract extends BaseEntity {

    /** 员工 ID（逻辑引用 hr_employee.id）。 */
    private Long employeeId;

    /** 合同编号（唯一）。 */
    private String contractNo;

    /** 合同类型。 */
    private ContractType contractType;

    /** 合同开始日期。 */
    private LocalDate startDate;

    /** 合同结束日期。 */
    private LocalDate endDate;

    /** 签订日期。 */
    private LocalDate signDate;

    /** 合同附件 ID。 */
    private Long fileId;

    /** 合同状态。 */
    private ContractStatus status;

    /** 续签提醒日期。 */
    private LocalDate renewalRemindDate;

    /** 备注。 */
    private String remark;
}
