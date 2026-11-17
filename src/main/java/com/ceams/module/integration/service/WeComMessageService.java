package com.ceams.module.integration.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.integration.vo.WeComMessageVO;

/** 企业微信应用消息（发件箱）服务。 */
public interface WeComMessageService {

    /** 审计目标类型：企业微信应用消息。 */
    String TARGET_WECOM_MESSAGE = "INTEGRATION_WECOM_MESSAGE";

    /**
     * 发件箱分页查询（按 id 倒序）。
     *
     * @param page 页码（从 1 开始）
     * @param size 每页条数（1~100）
     * @param status 状态过滤：PENDING/SENT/FAILED（为空不过滤）
     * @return 分页结果
     */
    PageResult<WeComMessageVO> page(int page, int size, String status);

    /**
     * 手工重投失败消息（仅 FAILED 可重投）。
     *
     * @param id 消息 ID
     */
    void retry(Long id);
}
