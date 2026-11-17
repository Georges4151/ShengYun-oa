package com.ceams.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.admin.dto.AssetBorrowRequest;
import com.ceams.module.admin.dto.AssetCreateRequest;
import com.ceams.module.admin.dto.AssetInventoryItem;
import com.ceams.module.admin.dto.AssetInventoryRequest;
import com.ceams.module.admin.dto.AssetScrapRequest;
import com.ceams.module.admin.dto.AssetUpdateRequest;
import com.ceams.module.admin.entity.AdminAsset;
import com.ceams.module.admin.entity.AdminAssetBorrow;
import com.ceams.module.admin.enums.AssetBorrowStatus;
import com.ceams.module.admin.enums.AssetCategory;
import com.ceams.module.admin.enums.AssetStatus;
import com.ceams.module.admin.mapper.AdminAssetBorrowMapper;
import com.ceams.module.admin.mapper.AdminAssetMapper;
import com.ceams.module.admin.service.AdminAssetService;
import com.ceams.module.admin.support.AdminConverter;
import com.ceams.module.admin.support.AdminNoGenerator;
import com.ceams.module.admin.vo.AssetBorrowVO;
import com.ceams.module.admin.vo.AssetInventorySummaryVO;
import com.ceams.module.admin.vo.AssetVO;
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.enums.AuditAction;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.hr.util.HrPageUtil;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.mapper.UserMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 行政资产服务实现。
 *
 * <p>资产状态只由 BPM 回调、归还、报废与盘点写入，且统一使用「当前状态 + 期望状态」条件更新，
 * 影响行数为 0 即抛 {@code BIZ_STATE_CONFLICT}，避免并发下状态被覆盖。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAssetServiceImpl implements AdminAssetService {

    /** 审计目标类型。 */
    private static final String AUDIT_TARGET = "ADMIN_ASSET";

    /** 资产编号生成冲突最大重试次数。 */
    private static final int ASSET_NO_MAX_RETRY = 3;

    /** 金额保留小数位。 */
    private static final int MONEY_SCALE = 2;

    private final AdminAssetMapper assetMapper;
    private final AdminAssetBorrowMapper borrowMapper;
    private final AdminNoGenerator noGenerator;
    private final BpmInstanceService bpmInstanceService;
    private final BpmAuditService bpmAuditService;
    private final UserMapper userMapper;

    @Override
    public PageResult<AssetVO> page(
            String keyword, AssetStatus status, Long deptId, long page, long size) {
        Page<AdminAsset> p = HrPageUtil.resolve(page, size);
        LambdaQueryWrapper<AdminAsset> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(AdminAsset::getAssetNo, kw).or().like(AdminAsset::getName, kw));
        }
        if (status != null) {
            wrapper.eq(AdminAsset::getStatus, status);
        }
        if (deptId != null) {
            wrapper.eq(AdminAsset::getDeptId, deptId);
        }
        wrapper.orderByDesc(AdminAsset::getCreateTime);
        IPage<AdminAsset> result = assetMapper.selectPage(p, wrapper);
        List<AssetVO> records =
                result.getRecords().stream().map(AdminConverter::toAssetVO).toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public AssetVO detail(Long id) {
        return AdminConverter.toAssetVO(requireAsset(id));
    }

    @Override
    @Transactional
    public String create(AssetCreateRequest request) {
        BigDecimal price = normalizeMoney(request.getPurchasePrice(), "购置金额");
        requireNonNegative(request.getDepreciationMonths(), "折旧月数");

        AdminAsset asset = new AdminAsset();
        asset.setName(request.getName().trim());
        asset.setCategory(request.getCategory() == null ? AssetCategory.OTHER : request.getCategory());
        asset.setSpecification(request.getSpecification());
        asset.setPurchaseDate(request.getPurchaseDate());
        asset.setPurchasePrice(price);
        asset.setDeptId(request.getDeptId());
        asset.setKeeperId(request.getKeeperId());
        asset.setLocation(request.getLocation());
        asset.setStatus(AssetStatus.IN_STOCK);
        asset.setDepreciationMonths(request.getDepreciationMonths());
        asset.setRemark(request.getRemark());

        for (int attempt = 1; attempt <= ASSET_NO_MAX_RETRY; attempt++) {
            asset.setAssetNo(noGenerator.nextAssetNo());
            try {
                assetMapper.insert(asset);
                log.info("资产建档成功：{}，编号 {}", asset.getId(), asset.getAssetNo());
                return String.valueOf(asset.getId());
            } catch (DuplicateKeyException e) {
                log.warn("资产编号 {} 已存在，第 {} 次重新取号", asset.getAssetNo(), attempt);
            }
        }
        throw BusinessException.of(ErrorCode.BIZ_DUPLICATE_CODE, "资产编号生成冲突，请稍后重试");
    }

    @Override
    public void update(Long id, AssetUpdateRequest request) {
        AdminAsset asset = requireAsset(id);
        if (StringUtils.isNotBlank(request.getName())) {
            asset.setName(request.getName().trim());
        }
        if (request.getCategory() != null) {
            asset.setCategory(request.getCategory());
        }
        if (request.getSpecification() != null) {
            asset.setSpecification(request.getSpecification());
        }
        if (request.getPurchaseDate() != null) {
            asset.setPurchaseDate(request.getPurchaseDate());
        }
        if (request.getPurchasePrice() != null) {
            asset.setPurchasePrice(normalizeMoney(request.getPurchasePrice(), "购置金额"));
        }
        if (request.getDeptId() != null) {
            asset.setDeptId(request.getDeptId());
        }
        if (request.getKeeperId() != null) {
            asset.setKeeperId(request.getKeeperId());
        }
        if (request.getLocation() != null) {
            asset.setLocation(request.getLocation());
        }
        if (request.getDepreciationMonths() != null) {
            requireNonNegative(request.getDepreciationMonths(), "折旧月数");
            asset.setDepreciationMonths(request.getDepreciationMonths());
        }
        if (request.getRemark() != null) {
            asset.setRemark(request.getRemark());
        }
        assetMapper.updateById(asset);
        log.info("资产 {} 基础信息已更新", id);
    }

    @Override
    @Transactional
    public String borrow(Long id, AssetBorrowRequest request) {
        AdminAsset asset = requireAsset(id);
        if (asset.getStatus() != AssetStatus.IN_STOCK) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅库存状态的在库资产可领用");
        }
        Long activeCount =
                borrowMapper.selectCount(
                        new LambdaQueryWrapper<AdminAssetBorrow>()
                                .eq(AdminAssetBorrow::getAssetId, id)
                                .in(
                                        AdminAssetBorrow::getStatus,
                                        AssetBorrowStatus.APPROVING,
                                        AssetBorrowStatus.BORROWING));
        if (activeCount != null && activeCount > 0) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "该资产已有领用或在途领用记录");
        }

        AdminAssetBorrow record = new AdminAssetBorrow();
        record.setAssetId(id);
        record.setBorrowerId(SecurityUtils.getCurrentUserIdOrDefault());
        record.setBorrowDate(request.getBorrowDate());
        record.setExpectedReturnDate(request.getExpectedReturnDate());
        record.setStatus(AssetBorrowStatus.APPROVING);
        record.setRemark(request.getRemark());
        borrowMapper.insert(record);

        InstanceStartRequest start = new InstanceStartRequest();
        start.setTemplateCode(BpmBizType.ADMIN_ASSET_BORROW.name());
        start.setBizType(BpmBizType.ADMIN_ASSET_BORROW.name());
        start.setBizId(record.getId());
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("assetId", String.valueOf(id));
        variables.put("assetNo", asset.getAssetNo());
        variables.put("borrowerId", String.valueOf(record.getBorrowerId()));
        variables.put("borrowDate", request.getBorrowDate().toString());
        start.setVariables(variables);
        String instanceId = bpmInstanceService.start(start);

        record.setBpmInstanceId(parseInstanceId(instanceId));
        borrowMapper.updateById(record);
        log.info("资产 {} 发起领用，记录 {}，BPM 实例 {}", id, record.getId(), instanceId);
        return String.valueOf(record.getId());
    }

    @Override
    public List<AssetBorrowVO> myBorrows() {
        Long borrowerId = SecurityUtils.getCurrentUserIdOrDefault();
        List<AdminAssetBorrow> records =
                borrowMapper.selectList(
                        new LambdaQueryWrapper<AdminAssetBorrow>()
                                .eq(AdminAssetBorrow::getBorrowerId, borrowerId)
                                .orderByDesc(AdminAssetBorrow::getCreateTime));
        if (records.isEmpty()) {
            return List.of();
        }
        Map<Long, AdminAsset> assets = loadAssets(records);
        String borrowerName = currentUserName(borrowerId);
        return records.stream()
                .map(
                        record ->
                                AdminConverter.toBorrowVO(
                                        record, assets.get(record.getAssetId()), borrowerName))
                .toList();
    }

    @Override
    @Transactional
    public void returnAsset(Long id, String remark) {
        requireAsset(id);
        AdminAssetBorrow record =
                borrowMapper.selectOne(
                        new LambdaQueryWrapper<AdminAssetBorrow>()
                                .eq(AdminAssetBorrow::getAssetId, id)
                                .eq(AdminAssetBorrow::getStatus, AssetBorrowStatus.BORROWING)
                                .orderByDesc(AdminAssetBorrow::getBorrowDate)
                                .last("LIMIT 1"));
        if (record == null) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "该资产当前没有待归还的领用记录");
        }

        AdminAssetBorrow borrowPatch = new AdminAssetBorrow();
        // 条件更新已保证并发安全，version 置空避免叠加乐观锁导致条件更新失效
        borrowPatch.setVersion(null);
        borrowPatch.setStatus(AssetBorrowStatus.RETURNED);
        borrowPatch.setActualReturnDate(LocalDate.now());
        if (StringUtils.isNotBlank(remark)) {
            borrowPatch.setRemark(remark);
        }
        int borrowRows =
                borrowMapper.update(
                        borrowPatch,
                        new LambdaUpdateWrapper<AdminAssetBorrow>()
                                .eq(AdminAssetBorrow::getId, record.getId())
                                .eq(AdminAssetBorrow::getStatus, AssetBorrowStatus.BORROWING));
        if (borrowRows == 0) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "领用记录状态已变更，请刷新后重试");
        }

        AdminAsset assetPatch = new AdminAsset();
        assetPatch.setVersion(null);
        assetPatch.setStatus(AssetStatus.IN_STOCK);
        int assetRows =
                assetMapper.update(
                        assetPatch,
                        new LambdaUpdateWrapper<AdminAsset>()
                                .eq(AdminAsset::getId, id)
                                .eq(AdminAsset::getStatus, AssetStatus.BORROWED));
        if (assetRows == 0) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "资产状态已变更，请刷新后重试");
        }
        log.info("资产 {} 归还完成，领用记录 {}", id, record.getId());
    }

    @Override
    @Transactional
    public void scrap(Long id, AssetScrapRequest request) {
        AdminAsset asset = requireAsset(id);
        if (asset.getStatus() != AssetStatus.IN_STOCK) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅库存状态的在库资产可报废");
        }
        Long activeCount =
                borrowMapper.selectCount(
                        new LambdaQueryWrapper<AdminAssetBorrow>()
                                .eq(AdminAssetBorrow::getAssetId, id)
                                .in(
                                        AdminAssetBorrow::getStatus,
                                        AssetBorrowStatus.APPROVING,
                                        AssetBorrowStatus.BORROWING));
        if (activeCount != null && activeCount > 0) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "该资产存在借出或在途领用记录，不能报废");
        }

        AdminAsset patch = new AdminAsset();
        patch.setVersion(null);
        patch.setStatus(AssetStatus.SCRAPPED);
        if (request != null && StringUtils.isNotBlank(request.getRemark())) {
            patch.setRemark(request.getRemark());
        }
        int rows =
                assetMapper.update(
                        patch,
                        new LambdaUpdateWrapper<AdminAsset>()
                                .eq(AdminAsset::getId, id)
                                .eq(AdminAsset::getStatus, AssetStatus.IN_STOCK));
        if (rows == 0) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "资产状态已变更，请刷新后重试");
        }
        recordAudit(
                id,
                AuditAction.SCRAP,
                Map.of("status", asset.getStatus().name()),
                Map.of("status", AssetStatus.SCRAPPED.name()));
        log.info("资产 {} 已报废", id);
    }

    @Override
    @Transactional
    public int inventory(AssetInventoryRequest request) {
        List<AssetInventoryItem> items = request.getItems();
        for (AssetInventoryItem item : items) {
            AssetStatus target = item.getStatus();
            if (target != null
                    && target != AssetStatus.IN_STOCK
                    && target != AssetStatus.REPAIRING) {
                throw BusinessException.of(
                        ErrorCode.VALID_ILLEGAL_ARGUMENT, "盘点状态仅允许 IN_STOCK 或 REPAIRING");
            }
        }

        int changed = 0;
        for (AssetInventoryItem item : items) {
            AdminAsset asset = assetMapper.selectById(item.getAssetId());
            if (asset == null) {
                continue;
            }
            // 借出 / 报废资产不允许被盘点覆盖状态
            if (asset.getStatus() != AssetStatus.IN_STOCK
                    && asset.getStatus() != AssetStatus.REPAIRING) {
                log.info("资产 {} 状态 {}，跳过盘点", asset.getId(), asset.getStatus());
                continue;
            }
            AdminAsset patch = new AdminAsset();
            patch.setVersion(null);
            patch.setLocation(item.getLocation());
            patch.setKeeperId(item.getKeeperId());
            patch.setStatus(item.getStatus());
            int rows =
                    assetMapper.update(
                            patch,
                            new LambdaUpdateWrapper<AdminAsset>()
                                    .eq(AdminAsset::getId, item.getAssetId())
                                    .in(
                                            AdminAsset::getStatus,
                                            AssetStatus.IN_STOCK,
                                            AssetStatus.REPAIRING));
            if (rows > 0) {
                changed++;
            }
        }
        if (changed > 0) {
            recordAudit(null, AuditAction.INVENTORY, null, Map.of("changed", changed));
        }
        log.info("资产盘点完成，提交 {} 条，实际变更 {} 条", items.size(), changed);
        return changed;
    }

    @Override
    public AssetInventorySummaryVO inventorySummary(String category) {
        AssetInventorySummaryVO summary = new AssetInventorySummaryVO();
        LambdaQueryWrapper<AdminAsset> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(category)) {
            AssetCategory filter = AssetCategory.from(category);
            if (filter == null) {
                throw BusinessException.of(
                        ErrorCode.VALID_ILLEGAL_ARGUMENT, "资产分类非法：" + category);
            }
            wrapper.eq(AdminAsset::getCategory, filter);
        }
        List<AdminAsset> assets = assetMapper.selectList(wrapper);

        BigDecimal totalValue = BigDecimal.ZERO;
        List<Long> assetIds = new ArrayList<>(assets.size());
        for (AdminAsset asset : assets) {
            if (asset.getId() != null) {
                assetIds.add(asset.getId());
            }
            if (asset.getPurchasePrice() != null) {
                totalValue = totalValue.add(asset.getPurchasePrice());
            }
            AssetStatus status = asset.getStatus();
            if (status == null) {
                continue;
            }
            switch (status) {
                case IN_STOCK -> summary.setInStockCount(summary.getInStockCount() + 1);
                case BORROWED -> summary.setBorrowedCount(summary.getBorrowedCount() + 1);
                case REPAIRING -> summary.setRepairingCount(summary.getRepairingCount() + 1);
                case SCRAPPED -> summary.setScrappedCount(summary.getScrappedCount() + 1);
                default -> {
                    // 枚举已穷举，保留分支防御未来扩展
                }
            }
        }
        summary.setTotalCount(assets.size());
        summary.setTotalValue(totalValue.setScale(MONEY_SCALE, RoundingMode.HALF_UP));

        // 逾期借用：仅统计落在本次分类过滤范围内的资产
        long overdueCount = 0L;
        if (!assetIds.isEmpty()) {
            Long count =
                    borrowMapper.selectCount(
                            new LambdaQueryWrapper<AdminAssetBorrow>()
                                    .in(AdminAssetBorrow::getAssetId, assetIds)
                                    .eq(AdminAssetBorrow::getStatus, AssetBorrowStatus.BORROWING)
                                    .isNotNull(AdminAssetBorrow::getExpectedReturnDate)
                                    .lt(AdminAssetBorrow::getExpectedReturnDate, LocalDate.now()));
            overdueCount = count == null ? 0L : count;
        }
        summary.setOverdueCount(overdueCount);
        log.info("资产汇总：分类 {}，总数 {}，逾期借用 {} 条", category, summary.getTotalCount(), overdueCount);
        return summary;
    }

    @Override
    public List<AdminAssetBorrow> listOverdueBorrows(LocalDate baseDate) {
        LocalDate date = baseDate == null ? LocalDate.now() : baseDate;
        return borrowMapper.selectList(
                new LambdaQueryWrapper<AdminAssetBorrow>()
                        .eq(AdminAssetBorrow::getStatus, AssetBorrowStatus.BORROWING)
                        .isNotNull(AdminAssetBorrow::getExpectedReturnDate)
                        .lt(AdminAssetBorrow::getExpectedReturnDate, date)
                        .orderByAsc(AdminAssetBorrow::getExpectedReturnDate));
    }

    private AdminAsset requireAsset(Long id) {
        AdminAsset asset = id == null ? null : assetMapper.selectById(id);
        if (asset == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "资产不存在");
        }
        return asset;
    }

    private Map<Long, AdminAsset> loadAssets(List<AdminAssetBorrow> records) {
        List<Long> assetIds =
                records.stream()
                        .map(AdminAssetBorrow::getAssetId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        if (assetIds.isEmpty()) {
            return Map.of();
        }
        return assetMapper.selectBatchIds(assetIds).stream()
                .collect(Collectors.toMap(AdminAsset::getId, Function.identity(), (a, b) -> a));
    }

    private String currentUserName(Long userId) {
        if (userId == null || userId <= 0L) {
            return null;
        }
        SysUser user = userMapper.selectById(userId);
        return user == null ? null : user.getName();
    }

    /** 金额归一化：非空时校验非负并保留 2 位小数。 */
    private BigDecimal normalizeMoney(BigDecimal value, String label) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, label + "不能为负数");
        }
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private void requireNonNegative(Integer value, String label) {
        if (value != null && value < 0) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, label + "不能为负数");
        }
    }

    private void recordAudit(Long targetId, AuditAction action, Object before, Object after) {
        bpmAuditService.record(
                AUDIT_TARGET,
                targetId,
                action,
                before,
                after,
                null,
                SecurityUtils.getCurrentUserIdOrDefault());
    }

    private Long parseInstanceId(String instanceId) {
        if (StringUtils.isBlank(instanceId)) {
            return null;
        }
        try {
            return Long.valueOf(instanceId.trim());
        } catch (NumberFormatException e) {
            log.warn("BPM 实例 ID 非数字，无法回写领用记录：{}", instanceId);
            return null;
        }
    }
}
