package com.ceams.module.iam.converter;

import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.util.SensitiveMaskUtil;
import com.ceams.module.iam.vo.UserVO;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * 用户实体 <-> VO 转换。
 */
@Mapper(componentModel = "spring")
public interface UserConverter {

    UserVO toVO(SysUser user);

    List<UserVO> toVOList(List<SysUser> users);

    @AfterMapping
    default void mask(SysUser source, @MappingTarget UserVO vo) {
        vo.setPhone(SensitiveMaskUtil.maskPhone(source.getPhone()));
        vo.setEmail(SensitiveMaskUtil.maskEmail(source.getEmail()));
    }
}