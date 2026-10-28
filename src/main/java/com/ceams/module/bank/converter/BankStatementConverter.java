package com.ceams.module.bank.converter;

import com.ceams.module.bank.entity.BankStatement;
import com.ceams.module.bank.vo.BankStatementVO;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 银行流水实体 <-> VO 转换。
 */
@Mapper(componentModel = "spring")
public interface BankStatementConverter {

    default BankStatementVO toVO(BankStatement entity) {
        if (entity == null) {
            return null;
        }
        BankStatementVO vo = new BankStatementVO();
        vo.setId(entity.getId());
        vo.setAccountId(entity.getAccountId());
        vo.setTransactionDate(entity.getTransactionDate());
        vo.setAmount(entity.getAmount());
        vo.setType(entity.getType());
        vo.setDescription(entity.getDescription());
        vo.setCounterparty(entity.getCounterparty());
        vo.setBankReference(entity.getBankReference());
        vo.setBalance(entity.getBalance());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    List<BankStatementVO> toVOList(List<BankStatement> entities);
}
