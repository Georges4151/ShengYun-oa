package com.ceams.module.payment.converter;

import com.ceams.module.payment.entity.CostAllocation;
import com.ceams.module.payment.vo.CostAllocationVO;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 成本分摊实体 <-> VO 转换。
 */
@Mapper(componentModel = "spring")
public interface CostAllocationConverter {

    default CostAllocationVO toVO(CostAllocation entity) {
        if (entity == null) {
            return null;
        }
        CostAllocationVO vo = new CostAllocationVO();
        vo.setId(entity.getId());
        vo.setAccountRecordId(entity.getAccountRecordId());
        vo.setTargetType(entity.getTargetType());
        vo.setTargetId(entity.getTargetId());
        vo.setAllocationAmount(entity.getAllocationAmount());
        vo.setAllocationRatio(entity.getAllocationRatio());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    List<CostAllocationVO> toVOList(List<CostAllocation> entities);
}
