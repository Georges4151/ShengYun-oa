package com.ceams.module.admin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.module.admin.dto.AssetBorrowRequest;
import com.ceams.module.admin.dto.AssetInventoryItem;
import com.ceams.module.admin.dto.AssetInventoryRequest;
import com.ceams.module.admin.entity.AdminAsset;
import com.ceams.module.admin.entity.AdminAssetBorrow;
import com.ceams.module.admin.enums.AssetBorrowStatus;
import com.ceams.module.admin.enums.AssetStatus;
import com.ceams.module.admin.mapper.AdminAssetBorrowMapper;
import com.ceams.module.admin.mapper.AdminAssetMapper;
import com.ceams.module.admin.support.AdminNoGenerator;
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.iam.mapper.UserMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 资产服务单元测试：领用前置、归还、报废、盘点。 */
@ExtendWith(MockitoExtension.class)
class AdminAssetServiceImplTest {

    private static final Long ASSET_ID = 1001L;
    private static final Long BORROW_ID = 2001L;

    @Mock private AdminAssetMapper assetMapper;
    @Mock private AdminAssetBorrowMapper borrowMapper;
    @Mock private AdminNoGenerator noGenerator;
    @Mock private BpmInstanceService bpmInstanceService;
    @Mock private BpmAuditService bpmAuditService;
    @Mock private UserMapper userMapper;

    private AdminAssetServiceImpl assetService;

    @BeforeEach
    void setUp() {
        assetService =
                new AdminAssetServiceImpl(
                        assetMapper,
                        borrowMapper,
                        noGenerator,
                        bpmInstanceService,
                        bpmAuditService,
                        userMapper);
    }

    @Test
    void borrowShouldRejectWhenAssetNotInStock() {
        when(assetMapper.selectById(ASSET_ID)).thenReturn(asset(AssetStatus.BORROWED));

        assertThatThrownBy(() -> assetService.borrow(ASSET_ID, borrowRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅库存状态");
        verify(borrowMapper, never()).insert(any(AdminAssetBorrow.class));
    }

    @Test
    void borrowShouldCreateRecordAndStartBpm() {
        when(assetMapper.selectById(ASSET_ID)).thenReturn(asset(AssetStatus.IN_STOCK));
        when(borrowMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(borrowMapper.insert(any(AdminAssetBorrow.class)))
                .thenAnswer(
                        invocation -> {
                            AdminAssetBorrow record = invocation.getArgument(0);
                            record.setId(BORROW_ID);
                            return 1;
                        });
        when(bpmInstanceService.start(any(InstanceStartRequest.class))).thenReturn("9001");

        String id = assetService.borrow(ASSET_ID, borrowRequest());

        assertThat(id).isEqualTo(String.valueOf(BORROW_ID));
        ArgumentCaptor<InstanceStartRequest> startCaptor =
                ArgumentCaptor.forClass(InstanceStartRequest.class);
        verify(bpmInstanceService).start(startCaptor.capture());
        assertThat(startCaptor.getValue().getTemplateCode()).isEqualTo("ADMIN_ASSET_BORROW");
        assertThat(startCaptor.getValue().getBizId()).isEqualTo(BORROW_ID);

        ArgumentCaptor<AdminAssetBorrow> insertCaptor =
                ArgumentCaptor.forClass(AdminAssetBorrow.class);
        verify(borrowMapper).insert(insertCaptor.capture());
        assertThat(insertCaptor.getValue().getStatus()).isEqualTo(AssetBorrowStatus.APPROVING);
        assertThat(insertCaptor.getValue().getBorrowerId()).isEqualTo(0L);

        ArgumentCaptor<AdminAssetBorrow> updateCaptor =
                ArgumentCaptor.forClass(AdminAssetBorrow.class);
        verify(borrowMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getBpmInstanceId()).isEqualTo(9001L);
    }

    @Test
    void returnShouldRejectWhenNoBorrowingRecord() {
        when(assetMapper.selectById(ASSET_ID)).thenReturn(asset(AssetStatus.BORROWED));
        when(borrowMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> assetService.returnAsset(ASSET_ID, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("没有待归还");
        verify(assetMapper, never()).update(any(AdminAsset.class), any(Wrapper.class));
    }

    @Test
    void returnShouldWriteBackActualDateAndRestoreStock() {
        when(assetMapper.selectById(ASSET_ID)).thenReturn(asset(AssetStatus.BORROWED));
        AdminAssetBorrow record = new AdminAssetBorrow();
        record.setId(BORROW_ID);
        record.setAssetId(ASSET_ID);
        record.setStatus(AssetBorrowStatus.BORROWING);
        when(borrowMapper.selectOne(any(Wrapper.class))).thenReturn(record);
        when(borrowMapper.update(any(AdminAssetBorrow.class), any(Wrapper.class))).thenReturn(1);
        when(assetMapper.update(any(AdminAsset.class), any(Wrapper.class))).thenReturn(1);

        assetService.returnAsset(ASSET_ID, "正常归还");

        ArgumentCaptor<AdminAssetBorrow> borrowCaptor =
                ArgumentCaptor.forClass(AdminAssetBorrow.class);
        verify(borrowMapper).update(borrowCaptor.capture(), any(Wrapper.class));
        assertThat(borrowCaptor.getValue().getStatus()).isEqualTo(AssetBorrowStatus.RETURNED);
        assertThat(borrowCaptor.getValue().getActualReturnDate()).isEqualTo(LocalDate.now());
        assertThat(borrowCaptor.getValue().getRemark()).isEqualTo("正常归还");

        ArgumentCaptor<AdminAsset> assetCaptor = ArgumentCaptor.forClass(AdminAsset.class);
        verify(assetMapper).update(assetCaptor.capture(), any(Wrapper.class));
        assertThat(assetCaptor.getValue().getStatus()).isEqualTo(AssetStatus.IN_STOCK);
    }

    @Test
    void scrapShouldRejectWhenBorrowingExists() {
        when(assetMapper.selectById(ASSET_ID)).thenReturn(asset(AssetStatus.IN_STOCK));
        when(borrowMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> assetService.scrap(ASSET_ID, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能报废");
        verify(assetMapper, never()).update(any(AdminAsset.class), any(Wrapper.class));
    }

    @Test
    void inventoryShouldSkipBorrowedAssetAndReturnChangedCount() {
        AdminAsset inStock = asset(AssetStatus.IN_STOCK);
        AdminAsset borrowed = asset(AssetStatus.BORROWED);
        borrowed.setId(ASSET_ID + 1);
        when(assetMapper.selectById(ASSET_ID)).thenReturn(inStock);
        when(assetMapper.selectById(ASSET_ID + 1)).thenReturn(borrowed);
        when(assetMapper.update(any(AdminAsset.class), any(Wrapper.class))).thenReturn(1);

        AssetInventoryRequest request = new AssetInventoryRequest();
        request.setItems(
                List.of(
                        inventoryItem(ASSET_ID, AssetStatus.IN_STOCK, "A 库"),
                        inventoryItem(ASSET_ID + 1, AssetStatus.IN_STOCK, "B 库")));

        int changed = assetService.inventory(request);

        assertThat(changed).isEqualTo(1);
        verify(assetMapper, times(1)).update(any(AdminAsset.class), any(Wrapper.class));
        ArgumentCaptor<AdminAsset> captor = ArgumentCaptor.forClass(AdminAsset.class);
        verify(assetMapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getLocation()).isEqualTo("A 库");
    }

    private AdminAsset asset(AssetStatus status) {
        AdminAsset asset = new AdminAsset();
        asset.setId(ASSET_ID);
        asset.setAssetNo("AST-202611-0001");
        asset.setName("笔记本电脑");
        asset.setStatus(status);
        return asset;
    }

    private AssetBorrowRequest borrowRequest() {
        AssetBorrowRequest request = new AssetBorrowRequest();
        request.setBorrowDate(LocalDate.of(2026, 11, 13));
        request.setExpectedReturnDate(LocalDate.of(2026, 12, 13));
        request.setRemark("项目使用");
        return request;
    }

    private AssetInventoryItem inventoryItem(Long assetId, AssetStatus status, String location) {
        AssetInventoryItem item = new AssetInventoryItem();
        item.setAssetId(assetId);
        item.setStatus(status);
        item.setLocation(location);
        return item;
    }
}
