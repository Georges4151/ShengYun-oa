package com.ceams.module.ai.service;

import com.ceams.module.ai.vo.AiStatusVO;

/** AI 服务状态查询。 */
public interface AiStatusService {

    /** 获取当前 AI 能力状态（含 60 秒进程内探活缓存）。 */
    AiStatusVO getStatus();
}
