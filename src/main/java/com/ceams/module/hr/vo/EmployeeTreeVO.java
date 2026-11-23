package com.ceams.module.hr.vo;

import com.ceams.module.hr.enums.EmployeeStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 员工组织树节点（按直属上级关系组织）。 */
@Data
public class EmployeeTreeVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String employeeNo;

    private String name;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long deptId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long positionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long levelId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long leaderId;

    private EmployeeStatus status;

    private List<EmployeeTreeVO> children = new ArrayList<>();
}
