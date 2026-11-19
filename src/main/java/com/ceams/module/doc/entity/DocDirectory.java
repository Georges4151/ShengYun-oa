package com.ceams.module.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.doc.enums.LibraryType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 文档目录（doc_directory）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_directory")
public class DocDirectory extends BaseEntity {

    /** 目录名（同级不重名，业务层校验）。 */
    private String name;

    /** 父目录 ID（逻辑引用 doc_directory.id，null 为库根）。 */
    private Long parentId;

    /** 库类型：PERSONAL/DEPT/PROJECT/PUBLIC。 */
    private LibraryType libraryType;

    /** 所有者用户 ID（PERSONAL 必填）。 */
    private Long ownerUserId;

    /** 所有者部门 ID（DEPT 必填）。 */
    private Long ownerDeptId;

    /** 项目 ID（PROJECT 必填，逻辑引用 biz_project.id）。 */
    private Long projectId;

    /** 目录路径（/id/id/ 形式，支撑子树查询与移动校验）。 */
    private String path;

    /** 同级排序号。 */
    private Integer sortOrder;

    /** 状态：NORMAL/ARCHIVED。 */
    private String status;
}
