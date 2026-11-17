package com.ceams.module.iam.dto.dept;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新建部门请求。 */
@Data
public class DeptCreateRequest {

    @NotBlank(message = "部门名称不能为空")
    @Size(max = 100, message = "部门名称长度不能超过 100")
    private String name;

    /** 上级部门 ID（为空表示根部门）。 */
    private Long parentId;

    /** 部门主管用户 ID（可空）。 */
    private Long leaderId;

    /** 同级排序号（为空按 0 处理）。 */
    private Integer sortOrder;
}
