package com.ceams.service;

import com.ceams.model.entity.ApprovalRecord;
import com.ceams.model.entity.Receipt;

import java.util.List;
import java.util.Map;

public interface ApprovalFlowService {
    boolean processApproval(Integer receiptId, Integer approverId, String approverName, String status, String comment);

    List<ApprovalRecord> getApprovalHistory(Integer receiptId);

    Receipt getReceiptApprovalStatus(Integer receiptId);

    boolean canApprove(Integer receiptId, Integer approverId, String role);

    List<Receipt> getPendingApprovals(Integer approvalLevel);

    /**
     * 分页获取待审核票据列表
     * 
     * @param approvalLevel 审核级别
     * @param page          页码（从1开始）
     * @param pageSize      每页大小
     * @return 包含票据列表和总数的Map
     */
    Map<String, Object> getPendingApprovals(Integer approvalLevel, int page, int pageSize);

    /**
     * 获取项目经理可审核的待审核票据列表
     * 只返回项目经理绑定的项目的票据
     * 
     * @param managerId 项目经理ID
     * @return 可审核的票据列表
     */
    List<Receipt> getPendingApprovalsForManager(Integer managerId);

    /**
     * 分页获取项目经理可审核的待审核票据列表
     * 
     * @param managerId 项目经理ID
     * @param page      页码（从1开始）
     * @param pageSize  每页大小
     * @return 包含票据列表和总数的Map
     */
    Map<String, Object> getPendingApprovalsForManager(Integer managerId, int page, int pageSize);

    /**
     * 验证项目经理是否有权限审核指定票据
     * 
     * @param receiptId 票据ID
     * @param managerId 项目经理ID
     * @return 是否有权限
     */
    boolean validateProjectManagerPermission(Integer receiptId, Integer managerId);

    /**
     * 获取票据所属项目的项目经理名称
     * 
     * @param receiptId 票据ID
     * @return 项目经理名称，如果未分配则返回null
     */
    String getProjectManagerName(Integer receiptId);
}
