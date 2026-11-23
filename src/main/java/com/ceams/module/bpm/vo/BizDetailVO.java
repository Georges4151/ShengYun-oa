package com.ceams.module.bpm.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 审批单据业务详情视图（待办“查看详情”）。
 *
 * <p>所有业务主键在视图层统一转为字符串，避免雪花 ID 超出 JS 安全整数范围。
 */
@Data
public class BizDetailVO {

    /** 业务类型编码（BpmBizType 名称）。 */
    private String bizType;

    /** 业务单据 ID（雪花 ID 已转字符串，可能为空）。 */
    private String bizId;

    /** 弹窗标题，如“报销单详情”。 */
    private String title;

    /** 业务单据状态文案（如“审批中”）；不支持的业务类型为提示文案。 */
    private String statusText;

    /** 详情字段列表（label/value）。 */
    private List<BizDetailItemVO> items = new ArrayList<>();

    /** 附件分组列表（无附件时为空列表，序列化不返回 null）。 */
    private List<BizAttachmentGroupVO> attachmentGroups = new ArrayList<>();
}
