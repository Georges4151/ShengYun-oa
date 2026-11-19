package com.ceams.module.ai.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;

/** AI 经营日报摘要视图对象（{@code /api/v1/ai/summaries}）。 */
@Data
public class AiDailySummaryVO {

    /** 摘要 ID（雪花 ID 序列化为字符串）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 摘要日期。 */
    private LocalDate summaryDate;

    /** 周期类型：DAILY。 */
    private String periodType;

    /** 最终展示文本。 */
    private String content;

    /** 生成模型：TEMPLATE 或实际模型名。 */
    private String model;

    /** 触发方式：SCHEDULED / MANUAL。 */
    private String triggerType;

    /** 关联通知 ID（雪花 ID 序列化为字符串）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long notificationId;

    /** 结构化指标。 */
    private Map<String, Object> metrics;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;
}
