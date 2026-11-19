package com.ceams.module.hr.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.hr.dto.CheckinClockRequest;
import com.ceams.module.hr.dto.CheckinSupplementCreateRequest;
import com.ceams.module.hr.enums.CheckinStatus;
import com.ceams.module.hr.vo.AttendanceSummaryVO;
import com.ceams.module.hr.vo.CheckinSupplementVO;
import com.ceams.module.hr.vo.CheckinVO;
import java.time.LocalDate;
import java.util.List;

/** 打卡与补卡服务。 */
public interface HrCheckinService {

    /** 员工打卡（按当天排班或默认班次判定迟到/早退）。 */
    CheckinVO clock(CheckinClockRequest request);

    /** 我的打卡记录（按月份 yyyy-MM）。 */
    List<CheckinVO> my(String month);

    /** 打卡记录分页（管理端）。 */
    PageResult<CheckinVO> page(
            long page,
            long size,
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate,
            CheckinStatus status);

    /** 提交补卡申请并发起审批，返回申请单 ID。 */
    String createSupplement(CheckinSupplementCreateRequest request);

    /** 我的补卡申请。 */
    List<CheckinSupplementVO> mySupplements();

    /**
     * 考勤汇总（只读聚合，无事务）。
     *
     * <p>口径：打卡记录按 {@code checkinDate} 落在 [start, end] 过滤；employeeCount 为区间内
     * 有打卡记录的 distinct 员工数；正常 / 迟到 / 早退 / 缺卡按 {@code hr_checkin.status} 计数；
     * supplementCount 统计区间内审批通过（APPROVED）的补卡单条数。
     * 传 deptId 时先解析该部门员工集合再过滤，部门下无员工直接返回全 0；显式 employeeId 优先，
     * 与 deptId 同时给出时取交集。
     *
     * @param start 开始日期，可空表示不设下界
     * @param end 结束日期，可空表示不设上界
     * @param deptId 部门 ID，可空
     * @param employeeId 员工 ID，可空
     * @return 考勤汇总，无数据时为全 0
     */
    AttendanceSummaryVO summary(LocalDate start, LocalDate end, Long deptId, Long employeeId);
}
