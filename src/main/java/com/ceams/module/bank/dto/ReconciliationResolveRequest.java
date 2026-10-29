package com.ceams.module.bank.dto;

import lombok.Data;

/** 标记对账结果为已处理。 */
@Data
public class ReconciliationResolveRequest {

    /** 处理说明。 */
    private String resolutionNotes;
}
