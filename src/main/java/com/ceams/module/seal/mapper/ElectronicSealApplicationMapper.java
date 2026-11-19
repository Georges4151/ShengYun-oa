package com.ceams.module.seal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.seal.entity.ElectronicSealApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 用印申请 Mapper。 */
@Mapper
public interface ElectronicSealApplicationMapper extends BaseMapper<ElectronicSealApplication> {

    /** 按主键加行锁读取用印申请，用于串行化同一申请的并发审批创建。 */
    @Select(
            "SELECT * FROM biz_electronic_seal_application "
                    + "WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    ElectronicSealApplication selectByIdForUpdate(@Param("id") Long id);
}
