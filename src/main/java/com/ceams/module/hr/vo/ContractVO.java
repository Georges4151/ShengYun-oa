package com.ceams.module.hr.vo;

import com.ceams.module.hr.enums.ContractStatus;
import com.ceams.module.hr.enums.ContractType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 劳动合同视图对象。 */
@Data
public class ContractVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long employeeId;

    private String contractNo;

    private ContractType contractType;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDate signDate;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long fileId;

    private String filePath;

    private String fileName;

    private ContractStatus status;

    private LocalDate renewalRemindDate;

    private String remark;

    private LocalDateTime createTime;
}
