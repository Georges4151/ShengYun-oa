package com.ceams.module.bpm.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.bpm.dto.InstanceActionRequest;
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.vo.InstanceDetailVO;
import com.ceams.module.bpm.vo.InstanceVO;

/** 流程实例服务。 */
public interface BpmInstanceService {

    /** 发起实例：校验模板已启用，写实例/节点/待办/变量。 */
    String start(InstanceStartRequest request);

    /** 实例详情（节点、任务、变量、抄送）。 */
    InstanceDetailVO detail(Long id);

    /** 按发起人分页查询（我发起）。 */
    PageResult<InstanceVO> pageByInitiator(
            long page, long size, BpmInstanceStatus status, String bizType);

    /** 撤回：仅发起人，且首节点未处理。 */
    void withdraw(Long id, InstanceActionRequest request);

    /** 终止：仅管理员。 */
    void terminate(Long id, InstanceActionRequest request);
}
