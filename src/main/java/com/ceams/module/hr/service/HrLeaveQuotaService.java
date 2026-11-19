package com.ceams.module.hr.service;

import com.ceams.module.hr.enums.LeaveType;
import com.ceams.module.hr.vo.LeaveQuotaVO;
import java.math.BigDecimal;
import java.util.List;

/** 假期额度服务。 */
public interface HrLeaveQuotaService {

    /** 按员工 + 年度查询额度（年度为空取当前年）。 */
    List<LeaveQuotaVO> list(Long employeeId, Integer year);

    /** 调整总额度并重算剩余额度（已使用大于总额度时拒绝）。 */
    void adjust(Long id, BigDecimal totalDays);

    /** 按默认口径初始化员工年度额度（已存在则跳过）。 */
    void init(Long employeeId, Integer year);

    /**
     * 原子扣减额度：剩余额度不足或额度不存在时抛 BIZ_STATE_CONFLICT。
     *
     * @param employeeId 员工 ID
     * @param year 额度年度
     * @param leaveType 假期类型
     * @param days 扣减天数
     */
    void deduct(Long employeeId, Integer year, LeaveType leaveType, BigDecimal days);
}
