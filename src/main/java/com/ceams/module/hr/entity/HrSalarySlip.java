package com.ceams.module.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.hr.enums.SalarySlipReadStatus;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * HR 工资条（hr_salary_slip）。
 *
 * <p>明细全部落 {@code content_json}：金额字段先经 AES-256-GCM 加密再序列化，库内不出现明文金额；
 * 员工工号、姓名、核算月与业务度量值（考勤天数、迟到次数、绩效系数）可明文存储。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_salary_slip")
public class HrSalarySlip extends BaseEntity {

    /** 员工 ID（逻辑引用 hr_employee.id）。 */
    private Long employeeId;

    /** 来源核算记录 ID（逻辑引用 hr_salary_calc.id）。 */
    private Long calcId;

    /** 核算月（yyyy-MM）。 */
    private String calcMonth;

    /** 工资条明细 JSON（金额为密文）。 */
    private String contentJson;

    /** 发布时间。 */
    private LocalDateTime publishedAt;

    /** 阅读状态：PENDING/READ。 */
    private SalarySlipReadStatus readStatus;

    /** 阅读时间。 */
    private LocalDateTime readAt;
}
