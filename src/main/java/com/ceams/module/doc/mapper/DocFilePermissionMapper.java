package com.ceams.module.doc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.doc.entity.DocFilePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 文档文件级授权 Mapper。 */
@Mapper
public interface DocFilePermissionMapper extends BaseMapper<DocFilePermission> {

    /**
     * 查同一主体（含已逻辑删除）的授权记录。
     *
     * <p>授权表唯一键 (file_id, subject_type, subject_id) 不含 deleted，取消授权为软删除，
     * 再授权需要复用并复活旧记录，否则会触发唯一键冲突。
     */
    @Select(
            "SELECT id, file_id, subject_type, subject_id, actions, granted_by, deleted, version "
                    + "FROM doc_file_permission "
                    + "WHERE file_id = #{fileId} AND subject_type = #{subjectType} "
                    + "AND subject_id = #{subjectId} LIMIT 1")
    DocFilePermission selectAnyIncludingDeleted(
            @Param("fileId") Long fileId,
            @Param("subjectType") String subjectType,
            @Param("subjectId") Long subjectId);

    /** 复活已软删除的授权记录并覆盖动作集合。 */
    @Update(
            "UPDATE doc_file_permission "
                    + "SET deleted = 0, actions = #{actions}, granted_by = #{grantedBy}, "
                    + "version = version + 1, update_time = NOW() "
                    + "WHERE id = #{id}")
    int restore(
            @Param("id") Long id,
            @Param("actions") String actions,
            @Param("grantedBy") Long grantedBy);
}
