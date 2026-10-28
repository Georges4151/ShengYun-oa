package com.ceams.module.iam.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.iam.dto.role.RoleCreateRequest;
import com.ceams.module.iam.dto.role.RoleUpdateRequest;
import com.ceams.module.iam.vo.RoleVO;

import java.util.List;

/**
 * 角色服务。
 */
public interface RoleService {

    PageResult<RoleVO> page(long page, long size, String keyword);

    RoleVO detail(Long id);

    String create(RoleCreateRequest request);

    void update(Long id, RoleUpdateRequest request);

    void delete(Long id);

    void assignPermissions(Long id, List<Long> permissionIds);
}