package com.ceams.module.iam.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.iam.dto.user.ResetPasswordRequest;
import com.ceams.module.iam.dto.user.UserCreateRequest;
import com.ceams.module.iam.dto.user.UserUpdateRequest;
import com.ceams.module.iam.vo.RoleVO;
import com.ceams.module.iam.vo.UserVO;
import java.util.Collection;
import java.util.List;

/** 用户服务。 */
public interface UserService {

    PageResult<UserVO> page(long page, long size, String keyword, String status);

    UserVO detail(Long id);

    String create(UserCreateRequest request);

    void update(Long id, UserUpdateRequest request);

    void delete(Long id);

    void assignRoles(Long id, List<Long> roleIds);

    void resetPassword(Long id, ResetPasswordRequest request);

    /** 查询用户拥有的角色（供详情与认证复用）。 */
    List<RoleVO> listRolesByUser(Long userId);

    /**
     * 按角色编码查询活跃用户 ID（角色与用户均需 ACTIVE）。
     *
     * @param roleCodes 角色编码集合（空入参返回空列表）
     * @return 去重后的用户 ID 列表
     */
    List<Long> listActiveUserIdsByRoleCodes(Collection<String> roleCodes);
}
