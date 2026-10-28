package com.ceams.module.payment.converter;

import com.ceams.module.payment.entity.PaymentAccount;
import com.ceams.module.payment.vo.PaymentAccountVO;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 付款账户实体 <-> VO 转换。
 */
@Mapper(componentModel = "spring")
public interface PaymentAccountConverter {

    default PaymentAccountVO toVO(PaymentAccount entity) {
        if (entity == null) {
            return null;
        }
        PaymentAccountVO vo = new PaymentAccountVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setType(entity.getType());
        vo.setBalance(entity.getBalance());
        vo.setAccountNumber(entity.getAccountNumber());
        vo.setDescription(entity.getDescription());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    List<PaymentAccountVO> toVOList(List<PaymentAccount> entities);
}
