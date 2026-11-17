package com.ceams.module.hr.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.hr.dto.PerformanceLeaderRequest;
import com.ceams.module.hr.dto.PerformanceReviewBatchRequest;
import com.ceams.module.hr.dto.PerformanceReviewRequest;
import com.ceams.module.hr.dto.PerformanceSelfRequest;
import com.ceams.module.hr.dto.PerformanceTemplateSaveRequest;
import com.ceams.module.hr.enums.PerformanceReviewStatus;
import com.ceams.module.hr.vo.PerformanceBatchResultVO;
import com.ceams.module.hr.vo.PerformanceReviewVO;
import com.ceams.module.hr.vo.PerformanceTemplateVO;
import java.util.List;

/**
 * HR 绩效服务：模板维护、考评单批量生成、自评 / 上级打分 / HR 复核。
 *
 * <p>自评提交即发起 BPM（bizType=HR_PERFORMANCE），发起失败由事务回滚；审批终态由
 * {@code HrPerformanceBpmCallback} 派生到考评单状态。
 */
public interface HrPerformanceService {

    /** 保存绩效模板：id 为空新建（按 code 判重），否则按 id 修改。返回模板 ID。 */
    String saveTemplate(Long id, PerformanceTemplateSaveRequest request);

    /** 绩效模板列表（按创建时间倒序）。 */
    List<PerformanceTemplateVO> listTemplates();

    /** 考评单分页（支持模板、周期、状态、员工过滤）。 */
    PageResult<PerformanceReviewVO> pageReviews(
            long page,
            long size,
            Long templateId,
            String period,
            PerformanceReviewStatus status,
            Long employeeId);

    /** 当前登录员工的考评单分页。 */
    PageResult<PerformanceReviewVO> myReviews(long page, long size);

    /** 按模板 + 周期（可选部门）批量生成考评单。 */
    PerformanceBatchResultVO batchGenerate(PerformanceReviewBatchRequest request);

    /** 提交自评并启动 BPM（仅本人、仅 DRAFT）。 */
    void submitSelf(Long reviewId, PerformanceSelfRequest request);

    /** 直属上级打分（仅本人是直属上级、仅 SELF_SUBMITTED）。 */
    void submitLeader(Long reviewId, PerformanceLeaderRequest request);

    /** HR 复核定稿（仅 LEADER_DONE），并完成对应 BPM 实例。 */
    void review(Long reviewId, PerformanceReviewRequest request);
}
