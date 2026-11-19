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

    /** 合同原件存储名（通用文件服务返回的 name）。 */
    @Size(max = 255, message = "合同文件存储名长度不能超过 255")
    private String filePath;

    /** 合同原件原始文件名。 */
    @Size(max = 255, message = "合同文件名长度不能超过 255")
    private String fileName;

    private ContractStatus status;

    private LocalDate renewalRemindDate;

    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
}
