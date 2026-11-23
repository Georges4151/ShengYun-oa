package com.ceams.module.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新建职级请求。 */
@Data
public class JobLevelCreateRequest {

    @NotBlank(message = "职级编码不能为空")
    @Size(max = 64, message = "职级编码长度不能超过 64")
    private String code;

    @NotBlank(message = "职级名称不能为空")
    @Size(max = 128, message = "职级名称长度不能超过 128")
    private String name;

    @Size(max = 64, message = "职级序列长度不能超过 64")
    private String jobFamily;

    private Integer rankNo;

    private Integer sortOrder;

    /** 状态：ENABLED/DISABLED，默认 ENABLED。 */
    private String status;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
