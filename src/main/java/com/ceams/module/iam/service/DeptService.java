package com.ceams.module.iam.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.iam.dto.dept.DeptCreateRequest;
import com.ceams.module.iam.dto.dept.DeptUpdateRequest;
import com.ceams.module.iam.vo.DeptVO;
import java.util.List;

/** 部门服务：最小组织树维护。 */
public interface DeptService {

    /** 部门树（根部门列表，子部门挂在 children 上）。 */
    List<DeptVO> tree();

    /** 部门分页列表（按名称模糊搜索）。 */
    PageResult<DeptVO> page(long page, long size, String keyword);

    /** 新建部门，返回新部门 ID。 */
    String create(DeptCreateRequest request);

    /** 修改部门。 */
    void update(Long id, DeptUpdateRequest request);

    /** 删除部门（存在子部门或关联用户时拒绝）。 */
    void delete(Long id);
}
