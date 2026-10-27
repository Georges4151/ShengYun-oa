package com.ceams.module.iam.service;

import com.ceams.module.iam.vo.PermissionVO;

import java.util.List;

/**
 * 权限服务。
 */
public interface PermissionService {

    /** 权限树（按 parent_id 组装）。 */
    List<PermissionVO> tree();

    /** 当前登录用户权限码集合（去重）。 */
    List<String> currentUserPermissionCodes();
}