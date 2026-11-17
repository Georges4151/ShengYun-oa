package com.ceams.module.admin.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.admin.dto.RepairAssignRequest;
import com.ceams.module.admin.dto.RepairCreateRequest;
import com.ceams.module.admin.dto.RepairFinishRequest;
import com.ceams.module.admin.dto.RepairProcessRequest;
import com.ceams.module.admin.enums.RepairStatus;
import com.ceams.module.admin.vo.RepairStatsVO;
import com.ceams.module.admin.vo.RepairVO;
import java.time.LocalDate;
import java.util.List;

/** 行政报修服务。 */
public interface AdminRepairService {

    /** 提交报修（BPM），返回工单 ID。 */
    String create(RepairCreateRequest request);

    /** 工单分页查询（管理端，支持状态 / 维修人过滤）。 */
    PageResult<RepairVO> page(RepairStatus status, Long assigneeId, long page, long size);

    /** 本人报修工单。 */
    List<RepairVO> my();

    /** 派单 / 改派（仅 ASSIGNED）。 */
    void assign(Long id, RepairAssignRequest request);

    /** 开始处理（ASSIGNED → PROCESSING，维修人或管理员）。 */
    void process(Long id, RepairProcessRequest request);

    /** 完工（PROCESSING → DONE，维修人或管理员）。 */
    void finish(Long id, RepairFinishRequest request);

    /** 验收（DONE → ACCEPTED，报修人或管理员）。 */
    void accept(Long id);

    /**
     * 报修工单统计（只读聚合，无事务）。
     *
     * <p>口径：按 {@code createTime} 的日期落在 [start, end] 过滤；category 非空时按报修分类过滤
     * （非法值抛 BusinessException）；employeeUserId 非空时按 reporterId 过滤（本人视图）。
     * submitted=SUBMITTED，processing=ASSIGNED+PROCESSING，completed=DONE+ACCEPTED，
     * completionRate=completed/totalCount（0～1，4 位小数，total 为 0 时取 0），
     * avgHandleHours=assignedAt 与 finishedAt 均非空样本的平均小时（2 位小数，无样本为 0）。
     *
     * @param start 开始日期，可空
     * @param end 结束日期，可空
     * @param category 报修分类编码，可空
     * @param employeeUserId 报修人 sys_user.id，可空
     * @return 报修统计
     */
    RepairStatsVO stats(LocalDate start, LocalDate end, String category, Long employeeUserId);
}
