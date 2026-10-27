package com.ceams.dao;

import com.ceams.model.entity.ElectronicSeal;
import com.ceams.model.entity.ElectronicSealApplication;
import com.ceams.model.entity.ElectronicSealStamp;
import com.ceams.model.entity.ElectronicSealUsage;
import com.ceams.model.entity.StampApproval;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ElectronicSealMapper {
    // 电子章相关
    ElectronicSeal selectById(@Param("id") Integer id);

    List<ElectronicSeal> selectAll();

    int insert(ElectronicSeal electronicSeal);

    int update(ElectronicSeal electronicSeal);

    int delete(@Param("id") Integer id);

    int deleteStampBySealId(@Param("sealId") Integer sealId);

    int deleteApplicationBySealId(@Param("sealId") Integer sealId);

    int deleteUsageBySealId(@Param("sealId") Integer sealId);

    // 电子章申请相关
    ElectronicSealApplication selectApplicationById(@Param("id") Integer id);

    List<ElectronicSealApplication> selectApplicationsByApplicantId(@Param("applicantId") Integer applicantId);

    List<ElectronicSealApplication> selectApplicationsByStatus(@Param("status") String status);

    int insertApplication(ElectronicSealApplication application);

    int updateApplication(ElectronicSealApplication application);

    // 电子章使用记录相关
    ElectronicSealUsage selectUsageById(@Param("id") Integer id);

    List<ElectronicSealUsage> selectUsagesBySealId(@Param("sealId") Integer sealId);

    List<ElectronicSealUsage> selectUsagesByUserId(@Param("userId") Integer userId);

    int insertUsage(ElectronicSealUsage usage);

    int deleteUsage(@Param("id") Integer id);

    // 电子章盖章相关
    int insertStamp(ElectronicSealStamp stamp);

    List<ElectronicSealStamp> selectStampsByProjectId(@Param("projectId") Integer projectId);

    List<ElectronicSealStamp> selectStampsByUserId(@Param("userId") Integer userId);

    List<ElectronicSealStamp> selectAllStamps();

    // 盖章审批相关
    int insertStampApproval(StampApproval approval);

    StampApproval selectStampApprovalById(@Param("id") Integer id);

    StampApproval selectStampApprovalByStampId(@Param("stampId") Integer stampId);

    List<StampApproval> selectStampApprovalsByApplicantId(@Param("applicantId") Integer applicantId);

    List<StampApproval> selectStampApprovalsByStatus(@Param("status") String status);

    List<StampApproval> selectAllStampApprovals();

    int updateStampApproval(StampApproval approval);
}
