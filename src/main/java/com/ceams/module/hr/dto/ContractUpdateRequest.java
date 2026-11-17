package com.ceams.module.hr.dto;

import com.ceams.module.hr.enums.ContractStatus;
import com.ceams.module.hr.enums.ContractType;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;

/** 修改劳动合同请求：字段为 null 表示不修改。 */
@Data
public class ContractUpdateRequest {

    @Size(max = 64, message = "合同编号长度不能超过 64")
    private String contractNo;

    private ContractType contractType;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDate signDate;

    private Long fileId;

    private ContractStatus status;

    private LocalDate renewalRemindDate;

    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
}
