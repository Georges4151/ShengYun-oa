package com.ceams.module.admin.dto;

import lombok.Data;

/** 报修完工请求。 */
@Data
public class RepairFinishRequest {

    /** 处理说明。 */
    private String remark;
}
