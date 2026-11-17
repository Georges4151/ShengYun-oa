package com.ceams.module.iam.dto.dept;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 修改部门请求：字段为空表示不修改。 */
@Data
public class DeptUpdateRequest {

    @Size(max = 100, message = "部门名称长度不能超过 100")
    private String name;

    /**
     * 上级部门 ID。
     *
     * <p>为空表示保持原上级；传 {@code 0} 表示移动到根部门；其余值表示挂到指定上级部门下。
     */
    private Long parentId;

    /** 部门主管用户 ID（为空表示不修改）。 */
    private Long leaderId;

    /** 同级排序号（为空表示不修改）。 */
    private Integer sortOrder;
}
