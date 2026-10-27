package com.ceams.module.iam.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.iam.dto.user.ResetPasswordRequest;
import com.ceams.module.iam.dto.user.UserCreateRequest;
import com.ceams.module.iam.dto.user.UserUpdateRequest;
import com.ceams.module.iam.vo.IdResponse;
import com.ceams.module.iam.vo.RoleVO;
import com.ceams.module.iam.vo.UserVO;

import java.util.List;

/**
 * 用户服务。
 */
public interface UserService {

    PageResult<UserVO> page(long page, long size, String keyword, String status);

    UserVO detail(Long id);

    IdResponse create(UserCreateRequest request);

    void update(Long id, UserUpdateRequest request);

    void delete(Long id);

    void assignRoles(Long id, List<Long> roleIds);

    void resetPassword(Long id, ResetPasswordRequest request);

    /** 查询用户拥有的角色（供详情与认证复用）。 */
    List<RoleVO> listRolesByUser(Long userId);
}