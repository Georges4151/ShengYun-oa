package com.ceams.module.ai.dto;

import java.time.LocalDate;
import lombok.Data;

/** 手动生成经营日报摘要请求。 */
@Data
public class AiSummaryGenerateRequest {

    /** 摘要日期（可空，默认昨日）。 */
    private LocalDate date;
}
