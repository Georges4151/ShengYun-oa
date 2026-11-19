package com.ceams.module.doc.dto;

import com.ceams.module.doc.enums.LibraryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新建目录请求。 */
@Data
public class DirectoryCreateRequest {

    /** 目录名。 */
    @NotBlank(message = "目录名称不能为空")
    @Size(max = 200, message = "目录名称不能超过 200 个字符")
    private String name;

    /** 父目录 ID，为空表示库根。 */
    private Long parentId;

    /** 库类型。 */
    @NotNull(message = "库类型不能为空")
    private LibraryType libraryType;

    /** 所有者用户 ID（PERSONAL 由服务端取当前用户）。 */
    private Long ownerUserId;

    /** 所有者部门 ID（DEPT 为空时取当前用户部门）。 */
    private Long ownerDeptId;

    /** 项目 ID（PROJECT 必填）。 */
    private Long projectId;

    /** 同级排序号，为空按 0 处理。 */
    private Integer sortOrder;
}
