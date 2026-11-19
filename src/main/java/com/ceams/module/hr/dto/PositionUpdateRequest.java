package com.ceams.module.hr.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 修改岗位请求：字段为 null 表示不修改。 */
@Data
public class PositionUpdateRequest {

    @Size(max = 128, message = "岗位名称长度不能超过 128")
    private String name;

    @Size(max = 32, message = "岗位职级长度不能超过 32")
    private String level;

    @Size(max = 64, message = "岗位序列长度不能超过 64")
    private String jobFamily;

    private Integer sortOrder;

    /** 状态：ENABLED/DISABLED。 */
    private String status;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
