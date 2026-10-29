package com.ceams.module.payment.converter;

import com.ceams.common.mask.SensitiveMaskUtil;
import com.ceams.module.payment.entity.PaymentAccount;
import com.ceams.module.payment.vo.PaymentAccountVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 付款账户实体 <-> VO 转换。 */
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
        vo.setAccountNumber(SensitiveMaskUtil.maskBankAccount(entity.getAccountNumber()));
        vo.setDescription(entity.getDescription());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    List<PaymentAccountVO> toVOList(List<PaymentAccount> entities);
}
