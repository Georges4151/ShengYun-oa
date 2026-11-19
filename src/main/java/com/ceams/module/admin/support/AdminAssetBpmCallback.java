package com.ceams.module.admin.support;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.admin.entity.AdminAsset;
import com.ceams.module.admin.entity.AdminAssetBorrow;
import com.ceams.module.admin.enums.AssetBorrowStatus;
import com.ceams.module.admin.enums.AssetStatus;
import com.ceams.module.admin.mapper.AdminAssetBorrowMapper;
import com.ceams.module.admin.mapper.AdminAssetMapper;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.support.BpmBizCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 资产领用审批回调。
 *
 * <p>通过：领用记录 APPROVING → BORROWING，资产 IN_STOCK → BORROWED；
 * 驳回 / 撤回 / 终止：记录置 REJECTED，资产状态不变。
 *
 * <p>幂等：记录已是 BORROWING 直接返回，避免重复回调重复推进资产状态。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAssetBpmCallback implements BpmBizCallback {

    private final AdminAssetMapper assetMapper;
    private final AdminAssetBorrowMapper borrowMapper;

    @Override
    public BpmBizType bizType() {
        return BpmBizType.ADMIN_ASSET_BORROW;
    }

    @Override
    public void onInstanceFinished(BpmInstance instance, BpmInstanceStatus status) {
        if (instance == null || instance.getBizId() == null) {
            return;
        }
        AdminAssetBorrow record = borrowMapper.selectById(instance.getBizId());
        if (record == null) {
            log.warn("资产领用记录 {} 不存在，忽略回调", instance.getBizId());
            return;
        }
        if (record.getStatus() == AssetBorrowStatus.BORROWING) {
            log.info("资产领用记录 {} 已是 BORROWING，跳过重复回调", record.getId());
            return;
        }
        if (status == BpmInstanceStatus.APPROVED) {
            approve(record);
            return;
        }
        if (status == BpmInstanceStatus.REJECTED
                || status == BpmInstanceStatus.WITHDRAWN
                || status == BpmInstanceStatus.TERMINATED) {
            reject(record);
            return;
        }
        log.debug("资产领用记录 {} 收到非终态 {}，忽略", record.getId(), status);
    }

    /** 审批通过：记录置 BORROWING，资产置 BORROWED。 */
    private void approve(AdminAssetBorrow record) {
        AdminAssetBorrow patch = new AdminAssetBorrow();
        patch.setVersion(null);
        patch.setStatus(AssetBorrowStatus.BORROWING);
        int recordRows =
                borrowMapper.update(
                        patch,
                        new LambdaUpdateWrapper<AdminAssetBorrow>()
                                .eq(AdminAssetBorrow::getId, record.getId())
                                .eq(AdminAssetBorrow::getStatus, AssetBorrowStatus.APPROVING));
        if (recordRows == 0) {
            log.info("资产领用记录 {} 状态已变更，跳过重复回调", record.getId());
            return;
        }

        AdminAsset assetPatch = new AdminAsset();
        assetPatch.setVersion(null);
        assetPatch.setStatus(AssetStatus.BORROWED);
        int assetRows =
                assetMapper.update(
                        assetPatch,
                        new LambdaUpdateWrapper<AdminAsset>()
                                .eq(AdminAsset::getId, record.getAssetId())
                                .eq(AdminAsset::getStatus, AssetStatus.IN_STOCK));
        if (assetRows == 0) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "资产状态已变更，无法确认领用");
        }
        log.info("资产 {} 领用审批通过，记录 {}", record.getAssetId(), record.getId());
    }

    /** 驳回 / 撤回 / 终止：记录置 REJECTED，资产状态不变。 */
    private void reject(AdminAssetBorrow record) {
        AdminAssetBorrow patch = new AdminAssetBorrow();
        patch.setVersion(null);
        patch.setStatus(AssetBorrowStatus.REJECTED);
        borrowMapper.update(
                patch,
                new LambdaUpdateWrapper<AdminAssetBorrow>()
                        .eq(AdminAssetBorrow::getId, record.getId())
                        .eq(AdminAssetBorrow::getStatus, AssetBorrowStatus.APPROVING));
        log.info("资产领用记录 {} 审批驳回，置 REJECTED", record.getId());
    }
}
