package com.ceams.module.bpm.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.bpm.dto.FlowPreviewRequest;
import com.ceams.module.bpm.dto.InstanceActionRequest;
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.vo.BizDetailVO;
import com.ceams.module.bpm.vo.FlowPreviewVO;
import com.ceams.module.bpm.vo.InstanceDetailVO;
import com.ceams.module.bpm.vo.InstanceVO;

/** 流程实例服务。 */
public interface BpmInstanceService {

    /** 发起实例：校验模板已启用，写实例/节点/待办/变量。 */
    String start(InstanceStartRequest request);

    /** 实例详情（节点、任务、变量、抄送）。 */
    InstanceDetailVO detail(Long id);

    /** 审批单据业务详情：按实例关联的业务类型/单据 ID 展示业务字段（沿用 detail 的可见性校验）。 */
    BizDetailVO bizDetail(Long instanceId);

    /**
     * 审批附件受控下载：先按实例校验可见性，再委托文件模块读取存储名对应文件（BPM 不拼接目录）。
     *
     * @param instanceId 流程实例 ID
     * @param name 文件模块返回的服务端存储名
     * @return 文件字节
     */
    byte[] loadAttachment(Long instanceId, String name);

    /** 发起前流程预览：按模板与业务数据解析各节点预计审批人。 */
    FlowPreviewVO preview(FlowPreviewRequest request);

    /** 按发起人分页查询（我发起）。 */
    PageResult<InstanceVO> pageByInitiator(
            long page, long size, BpmInstanceStatus status, String bizType);

    /** 撤回：仅发起人，且首节点未处理。 */
    void withdraw(Long id, InstanceActionRequest request);

    /** 终止：仅管理员。 */
    void terminate(Long id, InstanceActionRequest request);
}
