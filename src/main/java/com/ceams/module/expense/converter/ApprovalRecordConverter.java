package com.ceams.module.expense.converter;

import com.ceams.module.expense.entity.ApprovalRecord;
import com.ceams.module.expense.vo.ApprovalRecordVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 审批记录实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface ApprovalRecordConverter {

    default ApprovalRecordVO toVO(ApprovalRecord record) {
        if (record == null) {
            return null;
        }
        ApprovalRecordVO vo = new ApprovalRecordVO();
        vo.setId(record.getId());
        vo.setReceiptId(record.getReceiptId());
        vo.setApprovalLevel(record.getApprovalLevel());
        vo.setApproverRole(record.getApproverRole());
        vo.setApproverId(record.getApproverId());
        vo.setApproverName(record.getApproverName());
        vo.setStatus(record.getStatus());
        vo.setApprovalComment(record.getApprovalComment());
        vo.setApprovalTime(record.getApprovalTime());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    List<ApprovalRecordVO> toVOList(List<ApprovalRecord> records);
}
