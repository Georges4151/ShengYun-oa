package com.ceams.module.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.doc.enums.DocSubjectType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档目录级授权（doc_dir_permission）。
 *
 * <p>{@code actions} 为 MySQL JSON 列，实体用 String 承接原文， 序列化 / 反序列化由服务层通过 Jackson ObjectMapper
 * 处理，不引入自定义 TypeHandler。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_dir_permission")
public class DocDirPermission extends BaseEntity {

    /** 目录 ID（逻辑引用 doc_directory.id）。 */
    private Long dirId;

    /** 主体类型：USER/ROLE/DEPT。 */
    private DocSubjectType subjectType;

    /** 主体 ID（sys_user.id / sys_role.id / sys_dept.id）。 */
    private Long subjectId;

    /** 操作集合 JSON 数组原文：["PREVIEW","DOWNLOAD","EDIT","DELETE"]。 */
    private String actions;

    /** 授权人 ID（sys_user.id）。 */
    private Long grantedBy;
}
