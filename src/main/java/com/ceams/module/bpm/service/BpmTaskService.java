package com.ceams.module.bpm.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.bpm.dto.TaskApproveRequest;
import com.ceams.module.bpm.dto.TaskDelegateRequest;
import com.ceams.module.bpm.dto.TaskRejectRequest;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.vo.CcRecordVO;
import com.ceams.module.bpm.vo.InstanceVO;
import com.ceams.module.bpm.vo.TaskVO;

/** 审批任务服务：统一待办与四个查询视图。 */
public interface BpmTaskService {

    /** 我的待办：状态 PENDING 且处理人为当前用户（不含 PENDING_ASSIGNEE）。 */
    PageResult<TaskVO> pagePending(long page, long size, String bizType, Boolean overdue);

    /** 我发起：当前用户发起的实例。 */
    PageResult<InstanceVO> pageInitiated(
            long page, long size, BpmInstanceStatus status, String bizType);

    /** 我已办：当前用户已同意/已驳回的任务。 */
    PageResult<TaskVO> pageDone(long page, long size, String bizType);

    /** 抄送我的：可选按业务类型、阅读状态筛选。 */
    PageResult<CcRecordVO> pageCc(
            long page, long size, String bizType, com.ceams.module.bpm.enums.CcReadStatus readStatus);

    /** 同意：会签全部同意才流转，或签任一同意即流转。 */
    void approve(Long taskId, TaskApproveRequest request);

    /** 驳回：可驳回发起人或上一级。 */
    void reject(Long taskId, TaskRejectRequest request);

    /** 改派：管理员或具备 bpm:task:delegate 权限的用户。 */
    void delegate(Long taskId, TaskDelegateRequest request);
}
