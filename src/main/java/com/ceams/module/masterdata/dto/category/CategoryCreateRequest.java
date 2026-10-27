package com.ceams.module.masterdata.dto.category;

import com.ceams.module.masterdata.enums.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新建分类请求。
 */
@Data
public class CategoryCreateRequest {

    @NotBlank(message = "分类名不能为空")
    @Size(max = 100, message = "分类名长度不能超过 100")
    private String name;

    @NotNull(message = "分类类型不能为空")
    private CategoryType type;

    @Size(max = 50, message = "分类编码长度不能超过 50")
    private String code;

    private Long parentId;

    private Integer sort;
}