package com.ceams.module.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.hr.entity.HrLeaveQuota;
import java.math.BigDecimal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** 假期额度 Mapper。 */
@Mapper
public interface HrLeaveQuotaMapper extends BaseMapper<HrLeaveQuota> {

    /**
     * 原子扣减额度：仅当剩余额度足够时才更新，避免并发下的负库存。
     *
     * @param employeeId 员工 ID
     * @param year 额度年度
     * @param leaveType 假期类型（枚举 name）
     * @param days 扣减天数
     * @return 影响行数（0 表示额度不足或额度不存在）
     */
    @Update(
            "UPDATE hr_leave_quota"
                    + " SET used_days = used_days + #{days},"
                    + " remaining_days = remaining_days - #{days}"
                    + " WHERE employee_id = #{employeeId}"
                    + " AND year = #{year}"
                    + " AND leave_type = #{leaveType}"
                    + " AND deleted = 0"
                    + " AND remaining_days >= #{days}")
    int deduct(
            @Param("employeeId") Long employeeId,
            @Param("year") Integer year,
            @Param("leaveType") String leaveType,
            @Param("days") BigDecimal days);
}
