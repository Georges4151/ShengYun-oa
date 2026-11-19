package com.ceams.module.bank.converter;

import com.ceams.module.bank.entity.ReconciliationResult;
import com.ceams.module.bank.vo.ReconciliationResultVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 对账结果实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface ReconciliationResultConverter {

    default ReconciliationResultVO toVO(ReconciliationResult entity) {
        if (entity == null) {
            return null;
        }
        ReconciliationResultVO vo = new ReconciliationResultVO();
        vo.setId(entity.getId());
        vo.setAccountId(entity.getAccountId());
        vo.setStatementId(entity.getStatementId());
        vo.setTransactionId(entity.getTransactionId());
        vo.setStatus(entity.getStatus());
        vo.setDiscrepancyType(entity.getDiscrepancyType());
        vo.setBankAmount(entity.getBankAmount());
        vo.setSystemAmount(entity.getSystemAmount());
        vo.setBankDate(entity.getBankDate());
        vo.setSystemDate(entity.getSystemDate());
        vo.setBankDescription(entity.getBankDescription());
        vo.setSystemDescription(entity.getSystemDescription());
        vo.setStatementReference(entity.getStatementReference());
        vo.setTransactionReference(entity.getTransactionReference());
        vo.setResolutionStatus(entity.getResolutionStatus());
        vo.setResolutionNotes(entity.getResolutionNotes());
        vo.setReconciliationDate(entity.getReconciliationDate());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    List<ReconciliationResultVO> toVOList(List<ReconciliationResult> entities);
}
