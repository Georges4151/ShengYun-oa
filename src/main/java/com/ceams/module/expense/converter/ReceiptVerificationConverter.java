package com.ceams.module.expense.converter;

import com.ceams.module.expense.entity.ReceiptVerification;
import com.ceams.module.expense.vo.ReceiptVerificationVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 财务核验实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface ReceiptVerificationConverter {

    default ReceiptVerificationVO toVO(ReceiptVerification verification) {
        if (verification == null) {
            return null;
        }
        ReceiptVerificationVO vo = new ReceiptVerificationVO();
        vo.setId(verification.getId());
        vo.setReceiptId(verification.getReceiptId());
        vo.setPaymentAccountId(verification.getPaymentAccountId());
        vo.setAmount(verification.getAmount());
        vo.setPaymentMethod(verification.getPaymentMethod());
        vo.setVerificationTime(verification.getVerificationTime());
        vo.setVerifiedBy(verification.getVerifiedBy());
        vo.setRemark(verification.getRemark());
        vo.setCreateTime(verification.getCreateTime());
        return vo;
    }

    List<ReceiptVerificationVO> toVOList(List<ReceiptVerification> verifications);
}
