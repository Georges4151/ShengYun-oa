package com.ceams.service;

import com.ceams.model.entity.ElectronicSeal;
import com.ceams.model.entity.ElectronicSealApplication;
import com.ceams.model.entity.ElectronicSealStamp;
import com.ceams.model.entity.ElectronicSealUsage;
import com.ceams.model.entity.StampApproval;

import java.util.List;

public interface ElectronicSealService {
    // 电子章管理
    List<ElectronicSeal> getAllSeals();

    ElectronicSeal getSealById(Integer id);

    void addSeal(ElectronicSeal seal);

    void updateSeal(ElectronicSeal seal);

    void deleteSeal(Integer id);

    // 电子章申请管理
    void submitApplication(ElectronicSealApplication application);

    List<ElectronicSealApplication> getApplicationsByApplicantId(Integer applicantId);

    List<ElectronicSealApplication> getPendingApplications();

    void approveApplication(Integer applicationId, Integer approverId, String approverName, String comment);

    void rejectApplication(Integer applicationId, Integer approverId, String approverName, String comment);

    ElectronicSealApplication getApplicationById(Integer id);

    // 电子章使用管理
    boolean canUseSeal(Integer userId, Integer sealId);

    void recordSealUsage(ElectronicSealUsage usage);

    List<ElectronicSealUsage> getUsagesBySealId(Integer sealId);

    List<ElectronicSealUsage> getUsagesByUserId(Integer userId);

    // 电子章盖章管理
    void stampContract(ElectronicSealStamp stamp);

    List<ElectronicSealStamp> getStampsByProjectId(Integer projectId);

    List<ElectronicSealStamp> getStampsByUserId(Integer userId);

    // 新增方法：获取所有盖章记录
    List<ElectronicSealStamp> getAllStamps();

    // 电子章导入导出
    void importSeal(ElectronicSeal seal);

    List<ElectronicSeal> exportSeals();

    // 盖章审批管理
    void submitStampApproval(StampApproval approval);

    StampApproval getStampApprovalById(Integer id);

    StampApproval getStampApprovalByStampId(Integer stampId);

    List<StampApproval> getStampApprovalsByApplicantId(Integer applicantId);

    List<StampApproval> getPendingStampApprovals();

    List<StampApproval> getAllStampApprovals();

    void approveStamp(Integer approvalId, Integer approverId, String approverName);

    void rejectStamp(Integer approvalId, Integer approverId, String approverName, String rejectReason);

    BatchStampService.BatchStampResult batchStampContract(String contractPath, Integer sealId,
            float xPercent, float yPercent, float scale, String pageRange, Integer userId);
}
