package com.ceams.module.hr.dto;

import com.ceams.module.hr.enums.ContractStatus;
import com.ceams.module.hr.enums.ContractType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;

/** 新建劳动合同请求。 */
@Data
public class ContractCreateRequest {

    @NotNull(message = "员工ID不能为空")
    private Long employeeId;

    @NotBlank(message = "合同编号不能为空")
    @Size(max = 64, message = "合同编号长度不能超过 64")
    private String contractNo;

    @NotNull(message = "合同类型不能为空")
    private ContractType contractType;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDate signDate;

    private Long fileId;

    /** 合同原件存储名（通用文件服务返回的 name）。 */
    @Size(max = 255, message = "合同文件存储名长度不能超过 255")
    private String filePath;

    /** 合同原件原始文件名。 */
    @Size(max = 255, message = "合同文件名长度不能超过 255")
    private String fileName;

    /** 合同状态，默认 ACTIVE。 */
    private ContractStatus status;

    private LocalDate renewalRemindDate;

    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
}
