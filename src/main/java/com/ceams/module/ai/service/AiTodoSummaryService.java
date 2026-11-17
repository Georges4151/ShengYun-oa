package com.ceams.module.ai.service;

import com.ceams.module.ai.vo.AiTodoSummaryVO;

/** AI 待办摘要服务（只读，不落库、不发通知）。 */
public interface AiTodoSummaryService {

    /**
     * 汇总当前登录用户数据范围内的待办。
     *
     * @return 待办统计、分布、最近到期清单与摘要正文
     */
    AiTodoSummaryVO summary();
}
