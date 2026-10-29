package com.ceams.module.masterdata.dto.category;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 更新分类请求。 */
@Data
public class CategoryUpdateRequest {

    @Size(max = 100, message = "分类名长度不能超过 100")
    private String name;

    @Size(max = 50, message = "分类编码长度不能超过 50")
    private String code;

    private Long parentId;

    private Integer sort;

    private Boolean enabled;
}
