package com.ceams.module.iam.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.iam.vo.OperationLogVO;

/** 操作日志服务。 */
public interface OperationLogService {

    PageResult<OperationLogVO> page(
            long page,
            long size,
            String userId,
            String operationType,
            String startTime,
            String endTime);
}
