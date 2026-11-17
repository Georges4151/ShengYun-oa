package com.ceams.module.hr.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.hr.dto.ContractCreateRequest;
import com.ceams.module.hr.dto.ContractUpdateRequest;
import com.ceams.module.hr.enums.ContractStatus;
import com.ceams.module.hr.vo.ContractVO;
import java.util.List;

/** 劳动合同服务。 */
public interface HrContractService {

    /** 合同分页（支持员工、状态过滤）。 */
    PageResult<ContractVO> page(long page, long size, Long employeeId, ContractStatus status);

    /** 新建合同，返回新增 ID。 */
    String create(ContractCreateRequest request);

    /** 修改合同（字段为 null 表示不修改）。 */
    void update(Long id, ContractUpdateRequest request);

    /** 即将到期合同：结束日期落在 [今天, 今天+days] 且状态为 ACTIVE。 */
    List<ContractVO> expiring(int days);
}
