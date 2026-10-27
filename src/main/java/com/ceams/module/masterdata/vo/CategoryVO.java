package com.ceams.module.masterdata.vo;

import com.ceams.module.masterdata.enums.CategoryType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分类视图对象。
 */
@Data
public class CategoryVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String name;

    private CategoryType type;

    private String code;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    private Integer sort;

    private Boolean enabled;

    private List<CategoryVO> children;

    private LocalDateTime createTime;
}