package com.ceams.module.admin.dto;

import com.ceams.module.admin.enums.AssetStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 资产盘点明细项。 */
@Data
public class AssetInventoryItem {

    /** 资产 ID。 */
    @NotNull(message = "盘点资产 ID 不能为空")
    private Long assetId;

    /** 盘点后的存放位置。 */
    private String location;

    /** 盘点后的保管人 ID。 */
    private Long keeperId;

    /** 盘点后的状态，仅允许 IN_STOCK / REPAIRING。 */
    private AssetStatus status;
}
