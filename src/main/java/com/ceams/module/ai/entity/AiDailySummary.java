package com.ceams.module.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** AI 经营日报摘要（ai_daily_summary）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_daily_summary")
public class AiDailySummary extends BaseEntity {

    /** 摘要日期（Asia/Shanghai 业务日）。 */
    private LocalDate summaryDate;

    /** 周期类型：DAILY。 */
    private String periodType = "DAILY";

    /** 最终展示文本。 */
    private String content;

    /** 结构化指标（JSON 文本）。 */
    private String metricsJson;

    /** 生成模型：TEMPLATE 或实际模型名。 */
    private String model;

    /** 触发方式：SCHEDULED / MANUAL。 */
    private String triggerType;

    /** 关联通知 ID（逻辑引用 biz_notification.id）。 */
    private Long notificationId;
}
