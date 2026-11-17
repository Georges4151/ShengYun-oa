package com.ceams.module.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.admin.enums.AssetBorrowStatus;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 行政资产领用 / 借还记录（admin_asset_borrow）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_asset_borrow")
public class AdminAssetBorrow extends BaseEntity {

    /** 资产 ID（逻辑引用 admin_asset.id）。 */
    private Long assetId;

    /** 借用人 ID（sys_user.id）。 */
    private Long borrowerId;

    /** 领用日期。 */
    private LocalDate borrowDate;

    /** 预计归还日期。 */
    private LocalDate expectedReturnDate;

    /** 实际归还日期。 */
    private LocalDate actualReturnDate;

    /** BPM 实例 ID。 */
    private Long bpmInstanceId;

    /** 状态：APPROVING/BORROWING/RETURNED/REJECTED。 */
    private AssetBorrowStatus status;

    /** 备注。 */
    private String remark;
}
