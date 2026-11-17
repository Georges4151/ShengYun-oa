package com.ceams.module.admin.dto;

import lombok.Data;

/** 资产报废 / 归还的备注请求（仅 remark 一个可选字段）。 */
@Data
public class AssetScrapRequest {

    /** 备注。 */
    private String remark;
}
