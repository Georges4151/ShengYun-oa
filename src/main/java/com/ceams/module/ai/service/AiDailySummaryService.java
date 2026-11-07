package com.ceams.module.ai.service;

import com.ceams.module.ai.vo.AiDailySummaryVO;
import java.time.LocalDate;
import java.util.List;

/** AI 经营日报摘要服务。 */
public interface AiDailySummaryService {

    /** 最近 N 条摘要（summaryDate 倒序，limit 1~30，非法值兜底 10）。 */
    List<AiDailySummaryVO> list(int limit);

    /** 最新一条摘要；无记录返回 null。 */
    AiDailySummaryVO latest();

    /** 指定日期摘要；无记录返回 null。 */
    AiDailySummaryVO byDate(LocalDate date);

    /**
     * 生成（或覆盖）指定日期的摘要。
     *
     * @param date 摘要日期，可空（默认昨日）；不得晚于昨日
     * @param trigger 触发方式：SCHEDULED / MANUAL
     * @return 生成后的摘要视图
     */
    AiDailySummaryVO generate(LocalDate date, TriggerType trigger);

    /** 逐日检查昨日往前 maxCatchUpDays 天内的缺失并补录（仅补缺）。 */
    void catchUpMissing();

    /** 摘要生成触发方式。 */
    enum TriggerType {
        /** 定时调度。 */
        SCHEDULED,
        /** 手动触发。 */
        MANUAL
    }
}
