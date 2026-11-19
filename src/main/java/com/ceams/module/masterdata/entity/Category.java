package com.ceams.module.masterdata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.masterdata.enums.CategoryType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 分类树（biz_category）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_category")
public class Category extends BaseEntity {

    /** 分类名。 */
    private String name;

    /** 业务类型。 */
    private CategoryType type;

    /** 分类编码。 */
    private String code;

    /** 父分类 ID（逻辑引用 biz_category.id）。 */
    private Long parentId;

    /** 排序。 */
    private Integer sortOrder;

    /** 启用：true 启用 false 禁用。 */
    private Boolean enabled;
}
