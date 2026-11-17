package com.ceams.module.admin.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.admin.dto.MaterialApplyRequest;
import com.ceams.module.admin.dto.MaterialCreateRequest;
import com.ceams.module.admin.dto.MaterialStockInRequest;
import com.ceams.module.admin.dto.MaterialUpdateRequest;
import com.ceams.module.admin.enums.MaterialStatus;
import com.ceams.module.admin.vo.MaterialApplyVO;
import com.ceams.module.admin.vo.MaterialVO;
import java.util.List;

/** 行政易耗品服务。 */
public interface AdminMaterialService {

    /** 分页查询物料台账（keyword 命中编码或名称）。 */
    PageResult<MaterialVO> page(String keyword, MaterialStatus status, long page, long size);

    /** 物料建档，返回物料 ID。 */
    String create(MaterialCreateRequest request);

    /** 编辑物料基础信息（库存不在此接口调整）。 */
    void update(Long id, MaterialUpdateRequest request);

    /** 入库，库存累加。 */
    void stockIn(Long id, MaterialStockInRequest request);

    /** 发起申领（BPM），返回申领单 ID。 */
    String apply(MaterialApplyRequest request);

    /** 本人申领记录。 */
    List<MaterialApplyVO> myApplies();

    /** 确认发放（仅 APPROVED → ISSUED）。 */
    void issue(Long applyId);

    /** 低于安全库存的启用物料，按缺口比例降序。 */
    List<MaterialVO> alerts();
}
