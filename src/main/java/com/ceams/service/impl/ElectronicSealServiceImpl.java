package com.ceams.service.impl;

import com.ceams.dao.ElectronicSealMapper;
import com.ceams.model.entity.ElectronicSeal;
import com.ceams.model.entity.ElectronicSealApplication;
import com.ceams.model.entity.ElectronicSealStamp;
import com.ceams.model.entity.ElectronicSealUsage;
import com.ceams.model.entity.StampApproval;
import com.ceams.service.ElectronicSealService;
import com.ceams.service.ContractPreviewService;
import com.ceams.service.BatchStampService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
public class ElectronicSealServiceImpl implements ElectronicSealService {

    @Autowired
    private ElectronicSealMapper electronicSealMapper;

    @Autowired
    @Lazy
    private ContractPreviewService contractPreviewService;

    @Autowired
    private BatchStampService batchStampService;

    // 电子章管理
    @Override
    public List<ElectronicSeal> getAllSeals() {
        return electronicSealMapper.selectAll();
    }

    @Override
    public ElectronicSeal getSealById(Integer id) {
        return electronicSealMapper.selectById(id);
    }

    @Override
    public void addSeal(ElectronicSeal seal) {
        electronicSealMapper.insert(seal);
    }

    @Override
    public void updateSeal(ElectronicSeal seal) {
        electronicSealMapper.update(seal);
    }

    @Override
    public void deleteSeal(Integer id) {
        electronicSealMapper.deleteStampBySealId(id);
        electronicSealMapper.deleteApplicationBySealId(id);
        electronicSealMapper.deleteUsageBySealId(id);
        electronicSealMapper.delete(id);
    }

    // 电子章申请管理
    @Override
    public void submitApplication(ElectronicSealApplication application) {
        application.setStatus("pending");
        electronicSealMapper.insertApplication(application);
    }

    @Override
    public List<ElectronicSealApplication> getApplicationsByApplicantId(Integer applicantId) {
        return electronicSealMapper.selectApplicationsByApplicantId(applicantId);
    }

    @Override
    public List<ElectronicSealApplication> getPendingApplications() {
        return electronicSealMapper.selectApplicationsByStatus("pending");
    }

    @Override
    public void approveApplication(Integer applicationId, Integer approverId, String approverName, String comment) {
        ElectronicSealApplication application = electronicSealMapper.selectApplicationById(applicationId);
        if (application != null) {
            application.setStatus("approved");
            application.setApproverId(approverId);
            application.setApproverName(approverName);
            application.setApprovalComment(comment);
            electronicSealMapper.updateApplication(application);
        }
    }

    @Override
    public void rejectApplication(Integer applicationId, Integer approverId, String approverName, String comment) {
        ElectronicSealApplication application = electronicSealMapper.selectApplicationById(applicationId);
        if (application != null) {
            application.setStatus("rejected");
            application.setApproverId(approverId);
            application.setApproverName(approverName);
            application.setApprovalComment(comment);
            electronicSealMapper.updateApplication(application);
        }
    }

    @Override
    public ElectronicSealApplication getApplicationById(Integer id) {
        return electronicSealMapper.selectApplicationById(id);
    }

    // 电子章使用管理
    @Override
    public boolean canUseSeal(Integer userId, Integer sealId) {
        // 检查是否有已批准的申请
        List<ElectronicSealApplication> applications = electronicSealMapper.selectApplicationsByApplicantId(userId);
        for (ElectronicSealApplication application : applications) {
            if (application.getSealId().equals(sealId) && "approved".equals(application.getStatus())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void recordSealUsage(ElectronicSealUsage usage) {
        electronicSealMapper.insertUsage(usage);

        // 更新印章使用次数
        if (usage.getSealId() != null) {
            ElectronicSeal seal = electronicSealMapper.selectById(usage.getSealId());
            if (seal != null) {
                int currentCount = seal.getUsageCount() != null ? seal.getUsageCount() : 0;
                seal.setUsageCount(currentCount + 1);
                seal.setLastUsedTime(new java.util.Date());
                electronicSealMapper.update(seal);
            }
        }
    }

    @Override
    public List<ElectronicSealUsage> getUsagesBySealId(Integer sealId) {
        return electronicSealMapper.selectUsagesBySealId(sealId);
    }

    @Override
    public List<ElectronicSealUsage> getUsagesByUserId(Integer userId) {
        return electronicSealMapper.selectUsagesByUserId(userId);
    }

    // 电子章盖章管理
    @Override
    public void stampContract(ElectronicSealStamp stamp) {
        if (stamp.getStampPosition() == null) {
            stamp.setStampPosition("bottom_right");
        }
        if (stamp.getStampImage() == null) {
            stamp.setStampImage("");
        }
        if (stamp.getStampTime() == null) {
            stamp.setStampTime(new java.util.Date());
        }
        electronicSealMapper.insertStamp(stamp);

        // 更新印章使用次数
        if (stamp.getSealId() != null) {
            ElectronicSeal seal = electronicSealMapper.selectById(stamp.getSealId());
            if (seal != null) {
                int currentCount = seal.getUsageCount() != null ? seal.getUsageCount() : 0;
                seal.setUsageCount(currentCount + 1);
                seal.setLastUsedTime(new java.util.Date());
                electronicSealMapper.update(seal);
            }
        }
    }

    @Override
    public List<ElectronicSealStamp> getStampsByProjectId(Integer projectId) {
        return electronicSealMapper.selectStampsByProjectId(projectId);
    }

    @Override
    public List<ElectronicSealStamp> getStampsByUserId(Integer userId) {
        return electronicSealMapper.selectStampsByUserId(userId);
    }

    @Override
    public List<ElectronicSealStamp> getAllStamps() {
        return electronicSealMapper.selectAllStamps();
    }

    // 电子章导入导出
    @Override
    public void importSeal(ElectronicSeal seal) {
        try {
            // 设置默认状态为未启用
            if (seal.getStatus() == null) {
                seal.setStatus("inactive");
            }
            // 设置默认存储类型
            if (seal.getStorageType() == null) {
                seal.setStorageType("local");
            }
            // 设置默认密码保护为false
            if (seal.getPasswordProtected() == null) {
                seal.setPasswordProtected(false);
            }
            // 设置使用次数为0
            if (seal.getUsageCount() == null) {
                seal.setUsageCount(0);
            }
            electronicSealMapper.insert(seal);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("导入电子章失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ElectronicSeal> exportSeals() {
        return electronicSealMapper.selectAll();
    }

    // 盖章审批管理
    @Override
    public void submitStampApproval(StampApproval approval) {
        approval.setStatus("pending");
        electronicSealMapper.insertStampApproval(approval);
    }

    @Override
    public StampApproval getStampApprovalById(Integer id) {
        return electronicSealMapper.selectStampApprovalById(id);
    }

    @Override
    public StampApproval getStampApprovalByStampId(Integer stampId) {
        return electronicSealMapper.selectStampApprovalByStampId(stampId);
    }

    @Override
    public List<StampApproval> getStampApprovalsByApplicantId(Integer applicantId) {
        return electronicSealMapper.selectStampApprovalsByApplicantId(applicantId);
    }

    @Override
    public List<StampApproval> getPendingStampApprovals() {
        return electronicSealMapper.selectStampApprovalsByStatus("pending");
    }

    @Override
    public List<StampApproval> getAllStampApprovals() {
        return electronicSealMapper.selectAllStampApprovals();
    }

    @Override
    public void approveStamp(Integer approvalId, Integer approverId, String approverName) {
        StampApproval approval = electronicSealMapper.selectStampApprovalById(approvalId);
        if (approval != null) {
            System.out.println("=== 开始审批盖章 ===");
            System.out.println("审批ID: " + approvalId);
            System.out.println("合同名称: " + approval.getContractName());
            System.out.println("原始合同路径: " + approval.getOriginalContractPath());
            System.out.println("印章位置数据: " + (approval.getStampPositions() != null ? "存在" : "空"));

            approval.setApproverId(approverId);
            approval.setApproverName(approverName);
            approval.setStatus("approved");
            approval.setApprovalTime(new java.util.Date());

            File stampedFile = contractPreviewService.generateStampedContract(approval);
            if (stampedFile != null && stampedFile.exists()) {
                approval.setContractPath(stampedFile.getAbsolutePath());
                System.out.println("盖章合同路径已设置: " + stampedFile.getAbsolutePath());
            } else {
                System.err.println("盖章文件生成失败，contractPath 将为空");
                approval.setContractPath(null);
            }

            electronicSealMapper.updateStampApproval(approval);
            System.out.println("审批记录已更新");

            if (approval.getSealId() != null) {
                ElectronicSeal seal = electronicSealMapper.selectById(approval.getSealId());
                if (seal != null) {
                    int currentCount = seal.getUsageCount() != null ? seal.getUsageCount() : 0;
                    seal.setUsageCount(currentCount + 1);
                    seal.setLastUsedTime(new java.util.Date());
                    electronicSealMapper.update(seal);
                }
            }
        } else {
            System.err.println("未找到审批记录: " + approvalId);
        }
    }

    @Override
    public void rejectStamp(Integer approvalId, Integer approverId, String approverName, String rejectReason) {
        StampApproval approval = electronicSealMapper.selectStampApprovalById(approvalId);
        if (approval != null) {
            approval.setApproverId(approverId);
            approval.setApproverName(approverName);
            approval.setStatus("rejected");
            approval.setRejectReason(rejectReason);
            electronicSealMapper.updateStampApproval(approval);
        }
    }

    @Override
    public BatchStampService.BatchStampResult batchStampContract(String contractPath, Integer sealId,
            float xPercent, float yPercent, float scale, String pageRange, Integer userId) {
        ElectronicSeal seal = electronicSealMapper.selectById(sealId);
        if (seal == null) {
            return new BatchStampService.BatchStampResult(false, null, 0, "印章不存在");
        }

        String stampImagePath = seal.getSealImage();
        if (stampImagePath == null || stampImagePath.isEmpty()) {
            return new BatchStampService.BatchStampResult(false, null, 0, "印章图片路径为空");
        }

        String outputDir = "./stamped_contracts";
        BatchStampService.StampPosition position = new BatchStampService.StampPosition(xPercent, yPercent, scale);

        BatchStampService.BatchStampResult result = batchStampService.batchStampPdf(
                contractPath, stampImagePath, position, pageRange, outputDir);

        if (result.isSuccess()) {
            ElectronicSealStamp stamp = new ElectronicSealStamp();
            stamp.setSealId(sealId);
            stamp.setSealName(seal.getSealName());
            stamp.setContractPath(result.getOutputPath());
            stamp.setStampPosition(String.format("X:%.1f%%, Y:%.1f%%, Scale:%.1f%%, Pages:%s",
                    xPercent, yPercent, scale, pageRange));
            stamp.setStampTime(new java.util.Date());
            stamp.setUserId(userId);
            electronicSealMapper.insertStamp(stamp);

            int currentCount = seal.getUsageCount() != null ? seal.getUsageCount() : 0;
            seal.setUsageCount(currentCount + result.getStampedPages());
            seal.setLastUsedTime(new java.util.Date());
            electronicSealMapper.update(seal);
        }

        return result;
    }
}
