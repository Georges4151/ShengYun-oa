package com.ceams.service.impl;

import com.ceams.constant.RoleConstants;
import com.ceams.dao.ApprovalRecordMapper;
import com.ceams.dao.ReceiptMapper;
import com.ceams.exception.BusinessException;
import com.ceams.model.entity.ApprovalRecord;
import com.ceams.model.entity.Receipt;
import com.ceams.service.ApprovalFlowService;
import com.ceams.service.ProjectService;
import com.ceams.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ApprovalFlowServiceImpl implements ApprovalFlowService {

    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;

    @Autowired
    private ReceiptMapper receiptMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectService projectService;

    @Override
    @Transactional
    public boolean processApproval(Integer receiptId, Integer approverId, String approverName, String status,
            String comment) {
        try {
            Receipt receipt = receiptMapper.selectByIdForUpdate(receiptId);
            if (receipt == null) {
                System.out.println("票据不存在: " + receiptId);
                return false;
            }

            Integer currentLevel = receipt.getCurrentApprovalLevel();
            if (currentLevel == null) {
                currentLevel = 0;
            }

            Integer nextApprovalLevel = currentLevel + 1;

            List<ApprovalRecord> existingRecords = approvalRecordMapper.selectByReceiptIdAndApproverId(receiptId,
                    approverId);
            if (!existingRecords.isEmpty()) {
                System.out.println("审批人已审批过此票据: receiptId=" + receiptId + ", approverId=" + approverId);
                throw new BusinessException("APPROVAL_DUPLICATE", "您已审批过此票据，请勿重复操作");
            }

            ApprovalRecord existingLevelRecord = approvalRecordMapper.selectByReceiptIdAndLevel(receiptId,
                    nextApprovalLevel);
            if (existingLevelRecord != null) {
                System.out.println("该审批级别已有审批记录: receiptId=" + receiptId + ", level=" + nextApprovalLevel);
                throw new BusinessException("APPROVAL_LEVEL_EXISTS", "该票据在此审批级别已有审批记录，请刷新页面后重试");
            }

            String approverRole;
            switch (currentLevel) {
                case 0:
                    approverRole = RoleConstants.FINANCE;
                    break;
                case 1:
                    approverRole = RoleConstants.PROJECT_MANAGER;
                    break;
                case 2:
                    approverRole = RoleConstants.MANAGER;
                    break;
                default:
                    System.out.println("票据已完成审核");
                    return false;
            }

            ApprovalRecord record = new ApprovalRecord();
            record.setReceiptId(receiptId);
            record.setApprovalLevel(nextApprovalLevel);
            record.setApproverRole(approverRole);
            record.setApproverId(approverId);
            record.setApproverName(approverName);
            record.setStatus(status);
            record.setComment(comment);
            record.setApprovalTime(new Date());
            approvalRecordMapper.insert(record);

            receipt.setUpdateTime(new Date());

            if ("approved".equals(status)) {
                if (currentLevel == 0) {
                    receipt.setFinanceStatus("approved");
                    receipt.setFinanceVerifiedBy(approverId);
                    receipt.setFinanceVerifiedAt(new Date());
                    receipt.setFinanceComment(comment);
                    receipt.setCurrentApprovalLevel(1);
                } else if (currentLevel == 1) {
                    receipt.setProjectManagerStatus("approved");
                    receipt.setProjectManagerVerifiedBy(approverId);
                    receipt.setProjectManagerVerifiedAt(new Date());
                    receipt.setProjectManagerComment(comment);
                    receipt.setCurrentApprovalLevel(2);
                } else if (currentLevel == 2) {
                    receipt.setManagerStatus("approved");
                    receipt.setManagerVerifiedBy(approverId);
                    receipt.setManagerVerifiedAt(new Date());
                    receipt.setManagerComment(comment);
                    receipt.setCurrentApprovalLevel(3);
                    receipt.setVerificationStatus("verified");
                }
            } else if ("rejected".equals(status)) {
                if (currentLevel == 0) {
                    receipt.setFinanceStatus("rejected");
                    receipt.setFinanceVerifiedBy(approverId);
                    receipt.setFinanceVerifiedAt(new Date());
                    receipt.setFinanceComment(comment);
                } else if (currentLevel == 1) {
                    receipt.setProjectManagerStatus("rejected");
                    receipt.setProjectManagerVerifiedBy(approverId);
                    receipt.setProjectManagerVerifiedAt(new Date());
                    receipt.setProjectManagerComment(comment);
                } else if (currentLevel == 2) {
                    receipt.setManagerStatus("rejected");
                    receipt.setManagerVerifiedBy(approverId);
                    receipt.setManagerVerifiedAt(new Date());
                    receipt.setManagerComment(comment);
                }
                receipt.setVerificationStatus("rejected");
            }

            receiptMapper.updateApprovalStatus(receipt);
            System.out.println("审核处理完成: 票据ID=" + receiptId + ", 审核级别=" + nextApprovalLevel + ", 状态=" + status);
            return true;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException("APPROVAL_ERROR", "审批处理失败: " + e.getMessage());
        }
    }

    @Override
    public List<ApprovalRecord> getApprovalHistory(Integer receiptId) {
        return approvalRecordMapper.selectByReceiptId(receiptId);
    }

    @Override
    public Receipt getReceiptApprovalStatus(Integer receiptId) {
        return receiptMapper.selectById(receiptId);
    }

    @Override
    public boolean canApprove(Integer receiptId, Integer approverId, String role) {
        Receipt receipt = receiptMapper.selectById(receiptId);
        if (receipt == null) {
            return false;
        }

        Integer currentLevel = receipt.getCurrentApprovalLevel();
        if (currentLevel == null) {
            currentLevel = 0;
        }

        if (currentLevel >= 3) {
            return false;
        }

        switch (currentLevel) {
            case 0:
                return RoleConstants.FINANCE.equals(role);
            case 1:
                if (!RoleConstants.PROJECT_MANAGER.equals(role)) {
                    return false;
                }
                return canProjectManagerApprove(receipt, approverId);
            case 2:
                return RoleConstants.MANAGER.equals(role) || RoleConstants.ADMIN.equals(role);
            default:
                return false;
        }
    }

    private boolean canProjectManagerApprove(Receipt receipt, Integer approverId) {
        Integer projectId = receipt.getProjectId();
        if (projectId == null) {
            return false;
        }
        return projectService.isProjectManager(approverId, projectId);
    }

    @Override
    public List<Receipt> getPendingApprovals(Integer approvalLevel) {
        List<Receipt> allReceipts = receiptMapper.selectAll();
        java.util.stream.Collectors.toList();
        return allReceipts.stream()
                .filter(r -> {
                    Integer level = r.getCurrentApprovalLevel();
                    if (level == null) {
                        level = 0;
                    }
                    return level.equals(approvalLevel);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public Map<String, Object> getPendingApprovals(Integer approvalLevel, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Receipt> list = receiptMapper.selectPendingApprovalsByLevelWithPagination(approvalLevel, pageSize, offset);
        int total = receiptMapper.countPendingApprovalsByLevel(approvalLevel);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", (int) Math.ceil((double) total / pageSize));

        return result;
    }

    @Override
    public List<Receipt> getPendingApprovalsForManager(Integer managerId) {
        List<Receipt> allReceipts = receiptMapper.selectAll();
        return allReceipts.stream()
                .filter(r -> {
                    Integer level = r.getCurrentApprovalLevel();
                    if (level == null) {
                        level = 0;
                    }
                    if (!level.equals(1)) {
                        return false;
                    }
                    Integer projectId = r.getProjectId();
                    if (projectId == null) {
                        return false;
                    }
                    return projectService.isProjectManager(managerId, projectId);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public Map<String, Object> getPendingApprovalsForManager(Integer managerId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Receipt> list = receiptMapper.selectPendingApprovalsForManagerWithPagination(managerId, pageSize, offset);
        int total = receiptMapper.countPendingApprovalsForManager(managerId);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", (int) Math.ceil((double) total / pageSize));

        return result;
    }

    @Override
    public boolean validateProjectManagerPermission(Integer receiptId, Integer managerId) {
        Receipt receipt = receiptMapper.selectById(receiptId);
        if (receipt == null) {
            return false;
        }
        Integer projectId = receipt.getProjectId();
        if (projectId == null) {
            return false;
        }
        return projectService.isProjectManager(managerId, projectId);
    }

    @Override
    public String getProjectManagerName(Integer receiptId) {
        Receipt receipt = receiptMapper.selectById(receiptId);
        if (receipt == null) {
            return null;
        }
        Integer projectId = receipt.getProjectId();
        if (projectId == null) {
            return null;
        }
        com.ceams.model.entity.Project project = projectService.getProjectDetail(projectId);
        if (project == null) {
            return null;
        }
        return project.getManagerName();
    }
}
