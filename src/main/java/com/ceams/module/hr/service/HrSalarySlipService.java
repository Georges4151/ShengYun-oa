package com.ceams.module.hr.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.hr.dto.SalarySlipPublishRequest;
import com.ceams.module.hr.enums.SalarySlipReadStatus;
import com.ceams.module.hr.vo.SalarySlipPublishResultVO;
import com.ceams.module.hr.vo.SalarySlipVO;
import java.io.OutputStream;
import java.util.List;

/**
 * 工资条服务。
 *
 * <p>口径：仅已确认（CONFIRMED）的核算记录可发布工资条；发布按 (employee_id, calc_month) 幂等；
 * 本人查看与已读回写只允许操作自己的工资条，越权由服务层抛 AUTH_ACCESS_DENIED。
 */
public interface HrSalarySlipService {

    /** 按月生成并发布工资条（幂等：已存在的工资条跳过并计数）。 */
    SalarySlipPublishResultVO publish(SalarySlipPublishRequest request);

    /** 工资条分页（支持核算月、员工、阅读状态过滤）。 */
    PageResult<SalarySlipVO> page(
            long page, long size, String calcMonth, Long employeeId, SalarySlipReadStatus readStatus);

    /** 本人工资条列表（按核算月倒序，金额解密）。 */
    List<SalarySlipVO> listMine();

    /** 标记本人工资条已读（幂等），越权抛 AUTH_ACCESS_DENIED。 */
    SalarySlipVO markRead(Long id);

    /** 按月导出全部工资条为 xlsx，直接写入给定输出流。 */
    void export(String calcMonth, OutputStream out);
}
