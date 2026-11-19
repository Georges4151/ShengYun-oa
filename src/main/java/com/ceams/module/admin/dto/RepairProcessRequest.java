package com.ceams.module.admin.dto;

import lombok.Data;

/** 报修开始处理请求。 */
@Data
public class RepairProcessRequest {

    /** 处理说明。 */
    private String remark;
}
