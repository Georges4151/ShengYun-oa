package com.ceams.module.hr.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 修改职级请求：字段为 null 表示不修改。 */
@Data
public class JobLevelUpdateRequest {

    @Size(max = 64, message = "职级编码长度不能超过 64")
    private String code;

    @Size(max = 128, message = "职级名称长度不能超过 128")
    private String name;

    @Size(max = 64, message = "职级序列长度不能超过 64")
    private String jobFamily;

    private Integer rankNo;

    private Integer sortOrder;

    /** 状态：ENABLED/DISABLED。 */
    private String status;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
