package com.ceams.module.payment.converter;

import com.ceams.module.payment.entity.PaymentTransaction;
import com.ceams.module.payment.vo.PaymentTransactionVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 付款交易实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface PaymentTransactionConverter {

    default PaymentTransactionVO toVO(PaymentTransaction entity) {
        if (entity == null) {
            return null;
        }
        PaymentTransactionVO vo = new PaymentTransactionVO();
        vo.setId(entity.getId());
        vo.setAccountId(entity.getAccountId());
        vo.setReceiptId(entity.getReceiptId());
        vo.setProjectId(entity.getProjectId());
        vo.setAmount(entity.getAmount());
        vo.setType(entity.getType());
        vo.setTransactionDate(entity.getTransactionDate());
        vo.setDescription(entity.getDescription());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    List<PaymentTransactionVO> toVOList(List<PaymentTransaction> entities);
}
