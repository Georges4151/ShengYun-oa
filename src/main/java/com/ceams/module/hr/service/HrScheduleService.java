package com.ceams.module.hr.service;

import com.ceams.module.hr.dto.ScheduleBatchRequest;
import com.ceams.module.hr.dto.ScheduleCreateRequest;
import com.ceams.module.hr.dto.ScheduleUpdateRequest;
import com.ceams.module.hr.vo.ScheduleVO;
import java.util.List;

/** 排班服务。 */
public interface HrScheduleService {

    /** 按员工 + 月份（yyyy-MM）查询排班。 */
    List<ScheduleVO> list(Long employeeId, String month);

    /** 新建排班，返回新增 ID。 */
    String create(ScheduleCreateRequest request);

    /** 修改排班（字段为 null 表示不修改）。 */
    void update(Long id, ScheduleUpdateRequest request);

    /** 批量排班：按员工 × 日期区间逐日 upsert，返回处理条数。 */
    int batch(ScheduleBatchRequest request);
}
