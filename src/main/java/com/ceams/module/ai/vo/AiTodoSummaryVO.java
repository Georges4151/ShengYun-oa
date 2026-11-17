package com.ceams.module.ai.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** AI 待办摘要视图对象（{@code GET /api/v1/ai/todo-summary}）。 */
@Data
public class AiTodoSummaryVO {

    /** 生成时间。 */
    private LocalDateTime generatedAt;

    /** 当前数据范围内的待办总数。 */
    private long totalPending;

    /** 已超时待办数。 */
    private long overdue;

    /** 今日到期待办数。 */
    private long dueToday;

    /** 按业务类型分布（按固定顺序：报销、项目变更、其他）。 */
    private List<BizTypeCount> byBizType;

    /** 最近到期清单（按到期时间升序）。 */
    private List<UpcomingItem> upcoming;

    /** 摘要正文：规则模板或 AI 润色结果。 */
    private String content;

    /** 生成模型：TEMPLATE 或实际模型名。 */
    private String model;

    /** AI 能力是否开启（决定是否尝试润色）。 */
    private boolean aiEnabled;

    /** 免责声明。 */
    private String disclaimer;

    /** 业务类型分布项。 */
    @Data
    public static class BizTypeCount {

        /** 业务类型编码，如 EXPENSE。 */
        private String bizType;

        /** 业务类型中文名。 */
        private String label;

        /** 待办数量。 */
        private long count;
    }

    /** 最近到期待办项。 */
    @Data
    public static class UpcomingItem {

        /** 任务 ID（雪花 ID 序列化为字符串）。 */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long taskId;

        /** 流程实例 ID（雪花 ID 序列化为字符串）。 */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long instanceId;

        /** 业务类型编码。 */
        private String bizType;

        /** 业务类型中文名。 */
        private String label;

        /** 发起人姓名。 */
        private String initiatorName;

        /** 到期时间。 */
        private LocalDateTime dueAt;

        /** 是否已超时。 */
        private boolean overdue;
    }
}
