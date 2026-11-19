package com.ceams.module.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新建岗位请求。 */
@Data
public class PositionCreateRequest {

    @NotBlank(message = "岗位编码不能为空")
    @Size(max = 64, message = "岗位编码长度不能超过 64")
    private String code;

    @NotBlank(message = "岗位名称不能为空")
    @Size(max = 128, message = "岗位名称长度不能超过 128")
    private String name;

    @Size(max = 32, message = "岗位职级长度不能超过 32")
    private String level;

    @Size(max = 64, message = "岗位序列长度不能超过 64")
    private String jobFamily;

    private Integer sortOrder;

    /** 状态：ENABLED/DISABLED，默认 ENABLED。 */
    private String status;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
