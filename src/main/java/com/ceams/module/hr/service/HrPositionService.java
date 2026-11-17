package com.ceams.module.hr.service;

import com.ceams.module.hr.dto.PositionCreateRequest;
import com.ceams.module.hr.dto.PositionUpdateRequest;
import com.ceams.module.hr.vo.PositionVO;
import java.util.List;

/** 岗位服务。 */
public interface HrPositionService {

    /** 全量岗位（按排序号升序）。 */
    List<PositionVO> list();

    /** 新建岗位，返回新增 ID。 */
    String create(PositionCreateRequest request);

    /** 修改岗位（字段为 null 表示不修改）。 */
    void update(Long id, PositionUpdateRequest request);
}
