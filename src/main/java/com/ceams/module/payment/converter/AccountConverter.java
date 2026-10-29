package com.ceams.module.payment.converter;

import com.ceams.module.payment.entity.Account;
import com.ceams.module.payment.vo.AccountVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 收支流水实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface AccountConverter {

    default AccountVO toVO(Account entity) {
        if (entity == null) {
            return null;
        }
        AccountVO vo = new AccountVO();
        vo.setId(entity.getId());
        vo.setProjectId(entity.getProjectId());
        vo.setTransactionDate(entity.getTransactionDate());
        vo.setAmount(entity.getAmount());
        vo.setType(entity.getType());
        vo.setReceiptId(entity.getReceiptId());
        vo.setDescription(entity.getDescription());
        vo.setStatus(entity.getStatus());
        vo.setPaymentAccountId(entity.getPaymentAccountId());
        vo.setContractId(entity.getContractId());
        vo.setPaymentType(entity.getPaymentType());
        vo.setAllocationStatus(entity.getAllocationStatus());
        vo.setCategoryId(entity.getCategoryId());
        vo.setContactId(entity.getContactId());
        vo.setMaterialId(entity.getMaterialId());
        vo.setSubcontractorId(entity.getSubcontractorId());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    List<AccountVO> toVOList(List<Account> entities);
}
