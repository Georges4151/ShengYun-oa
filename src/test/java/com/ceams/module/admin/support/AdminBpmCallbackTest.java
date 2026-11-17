package com.ceams.module.admin.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.admin.entity.AdminAsset;
import com.ceams.module.admin.entity.AdminAssetBorrow;
import com.ceams.module.admin.entity.AdminMaterial;
import com.ceams.module.admin.entity.AdminMaterialApply;
import com.ceams.module.admin.enums.AssetBorrowStatus;
import com.ceams.module.admin.enums.AssetStatus;
import com.ceams.module.admin.enums.MaterialApplyStatus;
import com.ceams.module.admin.mapper.AdminAssetBorrowMapper;
import com.ceams.module.admin.mapper.AdminAssetMapper;
import com.ceams.module.admin.mapper.AdminMaterialApplyMapper;
import com.ceams.module.admin.mapper.AdminMaterialMapper;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.service.BpmAuditService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 行政 BPM 回调单元测试：资产领用与易耗品申领的通过 / 驳回 / 幂等 / 库存不足。 */
@ExtendWith(MockitoExtension.class)
class AdminBpmCallbackTest {

    private static final Long BORROW_ID = 5001L;
    private static final Long ASSET_ID = 1001L;
    private static final Long APPLY_ID = 6001L;
    private static final Long MATERIAL_ID = 3001L;

    @Mock private AdminAssetMapper assetMapper;
    @Mock private AdminAssetBorrowMapper borrowMapper;
    @Mock private AdminMaterialMapper materialMapper;
    @Mock private AdminMaterialApplyMapper applyMapper;
    @Mock private BpmAuditService bpmAuditService;

    private AdminAssetBpmCallback assetCallback;
    private AdminMaterialBpmCallback materialCallback;

    @BeforeEach
    void setUp() {
        assetCallback = new AdminAssetBpmCallback(assetMapper, borrowMapper);
        materialCallback = new AdminMaterialBpmCallback(materialMapper, applyMapper, bpmAuditService);
    }

    @Test
    void assetApprovedShouldMarkBorrowingAndAssetBorrowed() {
        when(borrowMapper.selectById(BORROW_ID)).thenReturn(borrow(AssetBorrowStatus.APPROVING));
        when(borrowMapper.update(any(AdminAssetBorrow.class), any(Wrapper.class))).thenReturn(1);
        when(assetMapper.update(any(AdminAsset.class), any(Wrapper.class))).thenReturn(1);

        assetCallback.onInstanceFinished(instance(BORROW_ID), BpmInstanceStatus.APPROVED);

        ArgumentCaptor<AdminAssetBorrow> borrowCaptor =
                ArgumentCaptor.forClass(AdminAssetBorrow.class);
        verify(borrowMapper).update(borrowCaptor.capture(), any(Wrapper.class));
        assertThat(borrowCaptor.getValue().getStatus()).isEqualTo(AssetBorrowStatus.BORROWING);

        ArgumentCaptor<AdminAsset> assetCaptor = ArgumentCaptor.forClass(AdminAsset.class);
        verify(assetMapper).update(assetCaptor.capture(), any(Wrapper.class));
        assertThat(assetCaptor.getValue().getStatus()).isEqualTo(AssetStatus.BORROWED);
    }

    @Test
    void assetRejectedShouldMarkRejectedAndKeepAsset() {
        when(borrowMapper.selectById(BORROW_ID)).thenReturn(borrow(AssetBorrowStatus.APPROVING));
        when(borrowMapper.update(any(AdminAssetBorrow.class), any(Wrapper.class))).thenReturn(1);

        assetCallback.onInstanceFinished(instance(BORROW_ID), BpmInstanceStatus.REJECTED);

        ArgumentCaptor<AdminAssetBorrow> borrowCaptor =
                ArgumentCaptor.forClass(AdminAssetBorrow.class);
        verify(borrowMapper).update(borrowCaptor.capture(), any(Wrapper.class));
        assertThat(borrowCaptor.getValue().getStatus()).isEqualTo(AssetBorrowStatus.REJECTED);
        verifyNoInteractions(assetMapper);
    }

    @Test
    void assetCallbackShouldBeIdempotentWhenAlreadyBorrowing() {
        when(borrowMapper.selectById(BORROW_ID)).thenReturn(borrow(AssetBorrowStatus.BORROWING));

        assetCallback.onInstanceFinished(instance(BORROW_ID), BpmInstanceStatus.APPROVED);

        verify(borrowMapper, never()).update(any(AdminAssetBorrow.class), any(Wrapper.class));
        verifyNoInteractions(assetMapper);
    }

    @Test
    void materialApprovedShouldDeductStockAndApprove() {
        when(applyMapper.selectById(APPLY_ID)).thenReturn(apply());
        when(materialMapper.selectById(MATERIAL_ID)).thenReturn(material("100"));
        when(materialMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(applyMapper.update(any(AdminMaterialApply.class), any(Wrapper.class))).thenReturn(1);

        materialCallback.onInstanceFinished(instance(APPLY_ID), BpmInstanceStatus.APPROVED);

        verify(materialMapper).update(isNull(), any(Wrapper.class));
        ArgumentCaptor<AdminMaterialApply> applyCaptor =
                ArgumentCaptor.forClass(AdminMaterialApply.class);
        verify(applyMapper).update(applyCaptor.capture(), any(Wrapper.class));
        assertThat(applyCaptor.getValue().getStatus()).isEqualTo(MaterialApplyStatus.APPROVED);
        verify(bpmAuditService)
                .record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void materialApprovedShouldThrowConflictWhenStockInsufficient() {
        when(applyMapper.selectById(APPLY_ID)).thenReturn(apply());
        when(materialMapper.selectById(MATERIAL_ID)).thenReturn(material("1"));
        when(materialMapper.update(isNull(), any(Wrapper.class))).thenReturn(0);

        assertThatThrownBy(
                        () ->
                                materialCallback.onInstanceFinished(
                                        instance(APPLY_ID), BpmInstanceStatus.APPROVED))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
        verify(applyMapper, never()).update(any(AdminMaterialApply.class), any(Wrapper.class));
    }

    @Test
    void materialRejectedShouldMarkRejectedAndKeepStock() {
        when(applyMapper.selectById(APPLY_ID)).thenReturn(apply());
        when(applyMapper.update(any(AdminMaterialApply.class), any(Wrapper.class))).thenReturn(1);

        materialCallback.onInstanceFinished(instance(APPLY_ID), BpmInstanceStatus.WITHDRAWN);

        ArgumentCaptor<AdminMaterialApply> applyCaptor =
                ArgumentCaptor.forClass(AdminMaterialApply.class);
        verify(applyMapper).update(applyCaptor.capture(), any(Wrapper.class));
        assertThat(applyCaptor.getValue().getStatus()).isEqualTo(MaterialApplyStatus.REJECTED);
        verifyNoInteractions(materialMapper);
    }

    private AdminAssetBorrow borrow(AssetBorrowStatus status) {
        AdminAssetBorrow borrow = new AdminAssetBorrow();
        borrow.setId(BORROW_ID);
        borrow.setAssetId(ASSET_ID);
        borrow.setStatus(status);
        return borrow;
    }

    private AdminMaterialApply apply() {
        AdminMaterialApply apply = new AdminMaterialApply();
        apply.setId(APPLY_ID);
        apply.setMaterialId(MATERIAL_ID);
        apply.setQuantity(new BigDecimal("5"));
        apply.setStatus(MaterialApplyStatus.APPROVING);
        return apply;
    }

    private AdminMaterial material(String stock) {
        AdminMaterial material = new AdminMaterial();
        material.setId(MATERIAL_ID);
        material.setStockQty(new BigDecimal(stock));
        material.setSafetyStock(new BigDecimal("10"));
        return material;
    }

    private BpmInstance instance(Long bizId) {
        BpmInstance instance = new BpmInstance();
        instance.setId(9001L);
        instance.setBizId(bizId);
        return instance;
    }
}
