package com.ceams.module.project.converter;

import com.ceams.module.project.entity.ProgressPayment;
import com.ceams.module.project.vo.ProgressPaymentVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 进度款实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface ProgressPaymentConverter {

    default ProgressPaymentVO toVO(ProgressPayment payment) {
        if (payment == null) {
            return null;
        }
        ProgressPaymentVO vo = new ProgressPaymentVO();
        vo.setId(payment.getId());
        vo.setProjectId(payment.getProjectId());
        vo.setPaymentPeriod(payment.getPaymentPeriod());
        vo.setApplicationAmount(payment.getApplicationAmount());
        vo.setApprovedAmount(payment.getApprovedAmount());
        vo.setPaymentAmount(payment.getPaymentAmount());
        vo.setApplicationDate(
                payment.getApplicationDate() == null
                        ? null
                        : payment.getApplicationDate().toLocalDate());
        vo.setApprovalDate(
                payment.getApprovalDate() == null ? null : payment.getApprovalDate().toLocalDate());
        vo.setPaymentDate(
                payment.getPaymentDate() == null ? null : payment.getPaymentDate().toLocalDate());
        vo.setStatus(payment.getStatus());
        vo.setApplicationBy(payment.getApplicationBy());
        vo.setApprovedBy(payment.getApprovedBy());
        vo.setCreateTime(payment.getCreateTime());
        return vo;
    }

    List<ProgressPaymentVO> toVOList(List<ProgressPayment> payments);
}
