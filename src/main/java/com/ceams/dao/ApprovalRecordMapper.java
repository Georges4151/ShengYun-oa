package com.ceams.dao;

import com.ceams.model.entity.ApprovalRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface ApprovalRecordMapper {
    int insert(ApprovalRecord record);
    int update(ApprovalRecord record);
    int deleteById(Integer id);
    ApprovalRecord selectById(Integer id);
    List<ApprovalRecord> selectByReceiptId(@Param("receiptId") Integer receiptId);
    List<ApprovalRecord> selectByApprovalLevel(@Param("approvalLevel") Integer approvalLevel);
    List<ApprovalRecord> selectByStatus(@Param("status") String status);
    List<ApprovalRecord> selectAll();
    List<ApprovalRecord> selectByDateRange(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
    ApprovalRecord selectLatestByReceiptId(@Param("receiptId") Integer receiptId);

    /**
     * 检查指定审批人是否已审批过指定票据
     * 
     * @param receiptId  票据ID
     * @param approverId 审批人ID
     * @return 审批记录列表
     */
    List<ApprovalRecord> selectByReceiptIdAndApproverId(
            @Param("receiptId") Integer receiptId,
            @Param("approverId") Integer approverId);

    /**
     * 检查指定票据在指定审批级别是否已有审批记录
     * 
     * @param receiptId     票据ID
     * @param approvalLevel 审批级别
     * @return 审批记录
     */
    ApprovalRecord selectByReceiptIdAndLevel(
            @Param("receiptId") Integer receiptId,
            @Param("approvalLevel") Integer approvalLevel);
}
