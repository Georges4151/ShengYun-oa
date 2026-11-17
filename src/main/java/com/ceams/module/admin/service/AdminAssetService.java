package com.ceams.module.admin.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.admin.dto.AssetBorrowRequest;
import com.ceams.module.admin.dto.AssetCreateRequest;
import com.ceams.module.admin.dto.AssetInventoryRequest;
import com.ceams.module.admin.dto.AssetScrapRequest;
import com.ceams.module.admin.dto.AssetUpdateRequest;
import com.ceams.module.admin.enums.AssetStatus;
import com.ceams.module.admin.entity.AdminAssetBorrow;
import com.ceams.module.admin.vo.AssetBorrowVO;
import com.ceams.module.admin.vo.AssetInventorySummaryVO;
import com.ceams.module.admin.vo.AssetVO;
import java.time.LocalDate;
import java.util.List;

/** 行政资产服务。 */
public interface AdminAssetService {

    /** 分页查询资产台账（keyword 命中编号或名称）。 */
    PageResult<AssetVO> page(
            String keyword, AssetStatus status, Long deptId, long page, long size);

    /** 资产详情。 */
    AssetVO detail(Long id);

    /** 建档，返回资产 ID。 */
    String create(AssetCreateRequest request);

    /** 编辑资产基础信息（状态不在此接口修改）。 */
    void update(Long id, AssetUpdateRequest request);

    /** 发起领用（BPM），返回领用记录 ID。 */
    String borrow(Long id, AssetBorrowRequest request);

    /** 本人领用记录。 */
    List<AssetBorrowVO> myBorrows();

    /** 归还登记（仅 BORROWING）。 */
    void returnAsset(Long id, String remark);

    /** 报废（仅 IN_STOCK 且无借出记录）。 */
    void scrap(Long id, AssetScrapRequest request);

    /** 批量盘点，返回实际变更条数。 */
    int inventory(AssetInventoryRequest request);

    /**
     * 资产台账汇总（只读聚合，无事务）。
     *
     * <p>口径：category 非空时按资产分类过滤（非法值抛 BusinessException）；按状态计数，
     * totalValue 在 Java 内累加明文 purchasePrice（null 跳过）；overdueCount 统计借用记录中
     * status=BORROWING 且 expectedReturnDate 早于今天、且资产落在本次过滤范围内的条数。
     *
     * @param category 资产分类编码，可空
     * @return 资产汇总
     */
    AssetInventorySummaryVO inventorySummary(String category);

    /**
     * 逾期未归还的借用记录（只读，P4 通知复用）。
     *
     * <p>口径：status=BORROWING 且 expectedReturnDate 早于基准日，按 expectedReturnDate 升序。
     *
     * @param baseDate 基准日，为空取当天
     * @return 借用记录实体列表
     */
    List<AdminAssetBorrow> listOverdueBorrows(LocalDate baseDate);
}
