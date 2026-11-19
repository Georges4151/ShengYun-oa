package com.ceams.module.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

/** 资产批量盘点请求。 */
@Data
public class AssetInventoryRequest {

    /** 盘点明细，逐条更新并返回实际变更条数。 */
    @Valid
    @NotEmpty(message = "盘点明细不能为空")
    private List<AssetInventoryItem> items;
}
