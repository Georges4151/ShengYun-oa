package com.ceams.module.expense.converter;

import com.ceams.module.expense.entity.Receipt;
import com.ceams.module.expense.vo.ReceiptVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 票据实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface ReceiptConverter {

    default ReceiptVO toVO(Receipt receipt) {
        if (receipt == null) {
            return null;
        }
        ReceiptVO vo = new ReceiptVO();
        vo.setId(receipt.getId());
        vo.setReceiptNumber(receipt.getReceiptNumber());
        vo.setUserId(receipt.getUserId());
        vo.setReporterId(receipt.getReporterId());
        vo.setProjectId(receipt.getProjectId());
        vo.setReporterName(receipt.getReporterName());
        vo.setReceiptDate(receipt.getReceiptDate());
        vo.setAmount(receipt.getAmount());
        vo.setType(receipt.getType());
        vo.setDescription(receipt.getDescription());
        vo.setFilePath(receipt.getFilePath());
        vo.setFileHash(receipt.getFileHash());
        vo.setOcrData(receipt.getOcrData());
        vo.setVerificationStatus(receipt.getVerificationStatus());
        vo.setVerifiedBy(receipt.getVerifiedBy());
        vo.setVerifiedAt(receipt.getVerifiedAt());
        vo.setPaymentStatus(receipt.getPaymentStatus());
        vo.setPaidAmount(receipt.getPaidAmount());
        vo.setPaidAt(receipt.getPaidAt());
        vo.setPaidBy(receipt.getPaidBy());
        vo.setPaymentMethod(receipt.getPaymentMethod());
        vo.setCurrentApprovalLevel(receipt.getCurrentApprovalLevel());
        vo.setApprovalStatus(receipt.getApprovalStatus());
        vo.setCreateTime(receipt.getCreateTime());
        return vo;
    }

    List<ReceiptVO> toVOList(List<Receipt> receipts);
}
