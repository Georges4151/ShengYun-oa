package com.ceams.module.bank.converter;

import com.ceams.module.bank.entity.GeneralLedger;
import com.ceams.module.bank.vo.GeneralLedgerVO;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 总账实体 <-> VO 转换。
 */
@Mapper(componentModel = "spring")
public interface GeneralLedgerConverter {

    default GeneralLedgerVO toVO(GeneralLedger entity) {
        if (entity == null) {
            return null;
        }
        GeneralLedgerVO vo = new GeneralLedgerVO();
        vo.setId(entity.getId());
        vo.setAccountName(entity.getAccountName());
        vo.setAccountNumber(entity.getAccountNumber());
        vo.setBankName(entity.getBankName());
        vo.setBalance(entity.getBalance());
        vo.setAccountType(entity.getAccountType());
        vo.setSummary(entity.getSummary());
        vo.setTransactionTime(entity.getTransactionTime());
        vo.setTransactionType(entity.getTransactionType());
        vo.setAmount(entity.getAmount());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    List<GeneralLedgerVO> toVOList(List<GeneralLedger> entities);
}
