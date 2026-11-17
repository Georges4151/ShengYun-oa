package com.ceams.module.integration.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.integration.enums.WeComSyncType;
import com.ceams.module.integration.vo.WeComSyncLogVO;
import com.ceams.module.integration.vo.WeComSyncResultVO;

/** 企业微信组织同步服务。 */
public interface WeComSyncService {

    /** 审计目标类型：企业微信组织同步批次。 */
    String TARGET_WECOM_SYNC = "INTEGRATION_WECOM_SYNC";

    /**
     * 手动触发一次组织同步。
     *
     * @param type 触发方式
     * @return 同步批次号（即使企业微信接口失败也返回批次号，便于日志追溯）
     */
    String sync(WeComSyncType type);

    /** 按批次号查询同步结果（无记录时抛 404）。 */
    WeComSyncResultVO result(String batchNo);

    /** 同步日志分页查询（按开始时间倒序）。 */
    PageResult<WeComSyncLogVO> logs(int page, int size, String status);
}
