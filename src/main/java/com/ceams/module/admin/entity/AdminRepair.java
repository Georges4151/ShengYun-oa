package com.ceams.module.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.admin.enums.RepairCategory;
import com.ceams.module.admin.enums.RepairStatus;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 行政报修工单（admin_repair）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_repair")
public class AdminRepair extends BaseEntity {

    /** 工单号（RP-yyyyMMdd-####）。 */
    private String repairNo;

    /** 报修人 ID（sys_user.id）。 */
    private Long reporterId;

    /** 报修人部门快照。 */
    private Long deptId;

    /** 报修位置。 */
    private String location;

    /** 分类：ELECTRIC/WATER/FURNITURE/IT/OTHER。 */
    private RepairCategory category;

    /** 故障描述。 */
    private String description;

    /** 图片文件名数组（JSON 文本）。 */
    private String imagesJson;

    /** 完工图片文件名数组（JSON 文本）。 */
    private String finishImagesJson;

    /** 维修人 ID。 */
    private Long assigneeId;

    /** BPM 实例 ID。 */
    private Long bpmInstanceId;

    /** 状态：SUBMITTED/ASSIGNED/PROCESSING/DONE/ACCEPTED/REJECTED。 */
    private RepairStatus status;

    /** 派单时间。 */
    private LocalDateTime assignedAt;

    /** 开始处理时间。 */
    private LocalDateTime processedAt;

    /** 完工时间。 */
    private LocalDateTime finishedAt;

    /** 验收时间。 */
    private LocalDateTime acceptedAt;

    /** 处理说明。 */
    private String remark;
}
