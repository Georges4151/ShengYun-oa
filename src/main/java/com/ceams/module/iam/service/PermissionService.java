package com.ceams.module.iam.service;

import com.ceams.module.iam.dto.permission.PermissionCreateRequest;
import com.ceams.module.iam.dto.permission.PermissionUpdateRequest;
import com.ceams.module.iam.vo.PermissionVO;
import java.util.List;

/** 权限服务。 */
public interface PermissionService {

    /** 权限树（按 parent_id 组装）。 */
    List<PermissionVO> tree();

    /** 当前登录用户权限码集合（去重）。 */
    List<String> currentUserPermissionCodes();

    /** 新建权限，返回新权限 ID。 */
    String create(PermissionCreateRequest request);

    /** 更新权限。 */
    void update(Long id, PermissionUpdateRequest request);

    /** 删除权限，并清理角色关联。 */
    void delete(Long id);
}
