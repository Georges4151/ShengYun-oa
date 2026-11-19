package com.ceams.module.admin.vo;

import com.ceams.module.admin.enums.RepairCategory;
import com.ceams.module.admin.enums.RepairStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** 报修工单展示对象。 */
@Data
public class RepairVO {

    /** 工单 ID。 */
    private Long id;

    /** 工单号。 */
    private String repairNo;

    /** 报修人 ID。 */
    private Long reporterId;

    /** 报修人部门快照。 */
    private Long deptId;

    /** 报修位置。 */
    private String location;

    /** 分类。 */
    private RepairCategory category;

    /** 故障描述。 */
    private String description;

    /** 图片文件名列表。 */
    private List<String> images;

    /** 完工图片文件名列表。 */
    private List<String> finishImages;

    /** 维修人 ID。 */
    private Long assigneeId;

    /** 状态。 */
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
