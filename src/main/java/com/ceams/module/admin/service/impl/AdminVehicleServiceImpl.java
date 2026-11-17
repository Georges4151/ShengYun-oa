package com.ceams.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.admin.config.AdminProperties;
import com.ceams.module.admin.dto.VehicleApplyRequest;
import com.ceams.module.admin.dto.VehicleCreateRequest;
import com.ceams.module.admin.dto.VehicleExpenseRequest;
import com.ceams.module.admin.dto.VehicleReturnRequest;
import com.ceams.module.admin.dto.VehicleUpdateRequest;
import com.ceams.module.admin.entity.AdminVehicle;
import com.ceams.module.admin.entity.AdminVehicleApply;
import com.ceams.module.admin.entity.AdminVehicleExpense;
import com.ceams.module.admin.enums.VehicleApplyStatus;
import com.ceams.module.admin.enums.VehicleStatus;
import com.ceams.module.admin.mapper.AdminVehicleApplyMapper;
import com.ceams.module.admin.mapper.AdminVehicleExpenseMapper;
import com.ceams.module.admin.mapper.AdminVehicleMapper;
import com.ceams.module.admin.service.AdminVehicleService;
import com.ceams.module.admin.support.AdminConflictGuard;
import com.ceams.module.admin.support.AdminConverter;
import com.ceams.module.admin.vo.VehicleApplyVO;
import com.ceams.module.admin.vo.VehicleExpenseVO;
import com.ceams.module.admin.vo.VehicleUsageItemVO;
import com.ceams.module.admin.vo.VehicleUsageSummaryVO;
import com.ceams.module.admin.vo.VehicleVO;
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.hr.util.HrPageUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 行政车辆服务实现。
 *
 * <p>车辆状态只由审批回调与还车写入，统一使用「当前状态 + 期望状态」条件更新，
 * 影响行数为 0 即抛 {@code BIZ_STATE_CONFLICT}，避免并发下状态被覆盖。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminVehicleServiceImpl implements AdminVehicleService {

    /** 金额 / 里程统一保留 2 位小数。 */
    private static final int MONEY_SCALE = 2;

    private final AdminVehicleMapper vehicleMapper;
    private final AdminVehicleApplyMapper applyMapper;
    private final AdminVehicleExpenseMapper expenseMapper;
    private final AdminConflictGuard conflictGuard;
    private final BpmInstanceService bpmInstanceService;
    private final AdminProperties adminProperties;

    @Override
    public PageResult<VehicleVO> page(
            String keyword, VehicleStatus status, long page, long size) {
        Page<AdminVehicle> p = HrPageUtil.resolve(page, size);
        LambdaQueryWrapper<AdminVehicle> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.and(
                    w -> w.like(AdminVehicle::getPlateNo, kw).or().like(AdminVehicle::getBrandModel, kw));
        }
        if (status != null) {
            wrapper.eq(AdminVehicle::getStatus, status);
        }
        wrapper.orderByDesc(AdminVehicle::getCreateTime);
        IPage<AdminVehicle> result = vehicleMapper.selectPage(p, wrapper);
        List<VehicleVO> records =
                result.getRecords().stream().map(AdminConverter::toVehicleVO).toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public String create(VehicleCreateRequest request) {
        String plateNo = request.getPlateNo().trim();
        if (existsPlate(plateNo, null)) {
            throw BusinessException.of(ErrorCode.BIZ_DUPLICATE_CODE, "车牌号已存在");
        }
        requireNonNegative(request.getSeats(), "座位数");

        AdminVehicle vehicle = new AdminVehicle();
        vehicle.setPlateNo(plateNo);
        vehicle.setBrandModel(request.getBrandModel());
        vehicle.setSeats(request.getSeats());
        vehicle.setPurchaseDate(request.getPurchaseDate());
        vehicle.setInsuranceExpire(request.getInsuranceExpire());
        vehicle.setInspectionExpire(request.getInspectionExpire());
        vehicle.setStatus(VehicleStatus.IDLE);
        vehicle.setRemark(request.getRemark());
        try {
            vehicleMapper.insert(vehicle);
        } catch (DuplicateKeyException e) {
            throw BusinessException.of(ErrorCode.BIZ_DUPLICATE_CODE, "车牌号已存在");
        }
        log.info("车辆建档成功：{}，车牌 {}", vehicle.getId(), plateNo);
        return String.valueOf(vehicle.getId());
    }

    @Override
    public void update(Long id, VehicleUpdateRequest request) {
        AdminVehicle vehicle = requireVehicle(id);
        if (request.getBrandModel() != null) {
            vehicle.setBrandModel(request.getBrandModel());
        }
        if (request.getSeats() != null) {
            requireNonNegative(request.getSeats(), "座位数");
            vehicle.setSeats(request.getSeats());
        }
        if (request.getPurchaseDate() != null) {
            vehicle.setPurchaseDate(request.getPurchaseDate());
        }
        if (request.getInsuranceExpire() != null) {
            vehicle.setInsuranceExpire(request.getInsuranceExpire());
        }
        if (request.getInspectionExpire() != null) {
            vehicle.setInspectionExpire(request.getInspectionExpire());
        }
        if (request.getRemark() != null) {
            vehicle.setRemark(request.getRemark());
        }
        vehicleMapper.updateById(vehicle);
        log.info("车辆 {} 基础信息已更新", id);
    }

    @Override
    @Transactional
    public String apply(VehicleApplyRequest request) {
        AdminVehicle vehicle = requireVehicle(request.getVehicleId());
        validateTimeRange(request.getStartTime(), request.getEndTime());
        if (conflictGuard.hasVehicleConflict(
                request.getVehicleId(), request.getStartTime(), request.getEndTime(), null)) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "该车辆在所选时段已被占用，请调整时间");
        }

        AdminVehicleApply apply = new AdminVehicleApply();
        apply.setVehicleId(request.getVehicleId());
        apply.setApplicantId(SecurityUtils.getCurrentUserIdOrDefault());
        apply.setPurpose(request.getPurpose());
        apply.setDestination(request.getDestination());
        apply.setStartTime(request.getStartTime());
        apply.setEndTime(request.getEndTime());
        apply.setStatus(VehicleApplyStatus.APPROVING);
        applyMapper.insert(apply);

        InstanceStartRequest start = new InstanceStartRequest();
        start.setTemplateCode(BpmBizType.ADMIN_VEHICLE_APPLY.name());
        start.setBizType(BpmBizType.ADMIN_VEHICLE_APPLY.name());
        start.setBizId(apply.getId());
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("vehicleId", String.valueOf(request.getVehicleId()));
        variables.put("plateNo", vehicle.getPlateNo());
        variables.put("applicantId", String.valueOf(apply.getApplicantId()));
        variables.put("startTime", request.getStartTime().toString());
        variables.put("endTime", request.getEndTime().toString());
        start.setVariables(variables);
        String instanceId = bpmInstanceService.start(start);

        apply.setBpmInstanceId(parseInstanceId(instanceId));
        applyMapper.updateById(apply);
        log.info(
                "车辆 {} 发起用车申请，申请单 {}，BPM 实例 {}",
                request.getVehicleId(),
                apply.getId(),
                instanceId);
        return String.valueOf(apply.getId());
    }

    @Override
    public List<VehicleApplyVO> myApplies() {
        Long applicantId = SecurityUtils.getCurrentUserIdOrDefault();
        List<AdminVehicleApply> records =
                applyMapper.selectList(
                        new LambdaQueryWrapper<AdminVehicleApply>()
                                .eq(AdminVehicleApply::getApplicantId, applicantId)
                                .orderByDesc(AdminVehicleApply::getCreateTime));
        if (records.isEmpty()) {
            return List.of();
        }
        Map<Long, AdminVehicle> vehicles = loadVehicles(records);
        return records.stream()
                .map(apply -> AdminConverter.toVehicleApplyVO(apply, vehicles.get(apply.getVehicleId())))
                .toList();
    }

    @Override
    @Transactional
    public void returnVehicle(Long applyId, VehicleReturnRequest request) {
        AdminVehicleApply apply = applyId == null ? null : applyMapper.selectById(applyId);
        if (apply == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "用车申请不存在");
        }
        Long currentUserId = SecurityUtils.getCurrentUserIdOrDefault();
        boolean owner = Objects.equals(apply.getApplicantId(), currentUserId);
        if (!owner && !isAdmin()) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "仅申请人本人或管理员可登记还车");
        }
        if (apply.getStatus() != VehicleApplyStatus.APPROVED
                && apply.getStatus() != VehicleApplyStatus.USING) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅审批通过或使用中的用车单可还车");
        }
        BigDecimal mileage = request == null ? null : request.getMileage();
        if (mileage == null || mileage.compareTo(BigDecimal.ZERO) < 0) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "还车里程不能为空且不能为负数");
        }
        LocalDateTime returnTime =
                request.getReturnTime() == null ? LocalDateTime.now() : request.getReturnTime();

        AdminVehicleApply patch = new AdminVehicleApply();
        // 条件更新已保证并发安全，version 置空避免叠加乐观锁导致条件更新失效
        patch.setVersion(null);
        patch.setStatus(VehicleApplyStatus.RETURNED);
        patch.setReturnTime(returnTime);
        patch.setMileage(mileage);
        int applyRows =
                applyMapper.update(
                        patch,
                        new LambdaUpdateWrapper<AdminVehicleApply>()
                                .eq(AdminVehicleApply::getId, applyId)
                                .in(
                                        AdminVehicleApply::getStatus,
                                        VehicleApplyStatus.APPROVED,
                                        VehicleApplyStatus.USING));
        if (applyRows == 0) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "用车单状态已变更，请刷新后重试");
        }

        AdminVehicle vehiclePatch = new AdminVehicle();
        vehiclePatch.setVersion(null);
        vehiclePatch.setStatus(VehicleStatus.IDLE);
        int vehicleRows =
                vehicleMapper.update(
                        vehiclePatch,
                        new LambdaUpdateWrapper<AdminVehicle>()
                                .eq(AdminVehicle::getId, apply.getVehicleId())
                                .eq(AdminVehicle::getStatus, VehicleStatus.USING));
        if (vehicleRows == 0) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "车辆状态已变更，请刷新后重试");
        }
        log.info(
                "用车单 {} 还车完成，车辆 {}，里程 {}，备注 {}",
                applyId,
                apply.getVehicleId(),
                mileage,
                request.getRemark());
    }

    @Override
    @Transactional
    public String addExpense(Long vehicleId, VehicleExpenseRequest request) {
        requireVehicle(vehicleId);
        if (request.getExpenseType() == null) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "费用类型不能为空");
        }
        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "金额必须大于 0");
        }
        if (request.getOccurDate() == null) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "发生日期不能为空");
        }

        AdminVehicleExpense expense = new AdminVehicleExpense();
        expense.setVehicleId(vehicleId);
        expense.setExpenseType(request.getExpenseType());
        expense.setAmount(amount);
        expense.setOccurDate(request.getOccurDate());
        expense.setAttachmentName(request.getAttachmentName());
        expense.setRemark(request.getRemark());
        expenseMapper.insert(expense);
        log.info("车辆 {} 登记费用 {}，金额 {}", vehicleId, expense.getId(), amount);
        return String.valueOf(expense.getId());
    }

    @Override
    public List<VehicleExpenseVO> expenses(Long vehicleId) {
        requireVehicle(vehicleId);
        return expenseMapper
                .selectList(
                        new LambdaQueryWrapper<AdminVehicleExpense>()
                                .eq(AdminVehicleExpense::getVehicleId, vehicleId)
                                .orderByDesc(AdminVehicleExpense::getOccurDate)
                                .orderByDesc(AdminVehicleExpense::getCreateTime))
                .stream()
                .map(AdminConverter::toVehicleExpenseVO)
                .toList();
    }

    @Override
    public List<VehicleVO> alerts() {
        LocalDate today = LocalDate.now();
        LocalDate limit = today.plusDays(Math.max(adminProperties.getAlertDays(), 0));
        LambdaQueryWrapper<AdminVehicle> wrapper = new LambdaQueryWrapper<>();
        // 到期日不为空且不晚于窗口上限（含已过期车辆），命中任一即预警
        wrapper.and(
                w ->
                        w.isNotNull(AdminVehicle::getInsuranceExpire)
                                .le(AdminVehicle::getInsuranceExpire, limit)
                                .or()
                                .isNotNull(AdminVehicle::getInspectionExpire)
                                .le(AdminVehicle::getInspectionExpire, limit));
        List<VehicleVO> result =
                vehicleMapper.selectList(wrapper).stream()
                        .map(vehicle -> AdminConverter.toVehicleVO(vehicle, today))
                        .sorted(Comparator.comparingLong(this::nearestRemainDays))
                        .toList();
        log.info("车辆到期预警 {} 条，窗口至 {}", result.size(), limit);
        return result;
    }

    @Override
    public VehicleUsageSummaryVO usageSummary(LocalDate start, LocalDate end, Long vehicleId) {
        VehicleUsageSummaryVO summary = new VehicleUsageSummaryVO();

        // 申请：按 startTime 的日期落在 [start, end]
        LambdaQueryWrapper<AdminVehicleApply> applyWrapper = new LambdaQueryWrapper<>();
        if (vehicleId != null) {
            applyWrapper.eq(AdminVehicleApply::getVehicleId, vehicleId);
        }
        if (start != null) {
            applyWrapper.ge(AdminVehicleApply::getStartTime, start.atStartOfDay());
        }
        if (end != null) {
            applyWrapper.lt(AdminVehicleApply::getStartTime, end.plusDays(1).atStartOfDay());
        }
        List<AdminVehicleApply> applies = applyMapper.selectList(applyWrapper);

        // 费用：按 occurDate 落在 [start, end]
        LambdaQueryWrapper<AdminVehicleExpense> expenseWrapper = new LambdaQueryWrapper<>();
        if (vehicleId != null) {
            expenseWrapper.eq(AdminVehicleExpense::getVehicleId, vehicleId);
        }
        if (start != null) {
            expenseWrapper.ge(AdminVehicleExpense::getOccurDate, start);
        }
        if (end != null) {
            expenseWrapper.le(AdminVehicleExpense::getOccurDate, end);
        }
        List<AdminVehicleExpense> expenses = expenseMapper.selectList(expenseWrapper);

        Map<Long, BigDecimal> expenseByVehicle = new HashMap<>();
        BigDecimal totalExpense = BigDecimal.ZERO;
        for (AdminVehicleExpense expense : expenses) {
            BigDecimal amount =
                    expense.getAmount() == null ? BigDecimal.ZERO : expense.getAmount();
            totalExpense = totalExpense.add(amount);
            if (expense.getVehicleId() != null) {
                expenseByVehicle.merge(expense.getVehicleId(), amount, BigDecimal::add);
            }
        }

        // 按车辆分组：TreeMap 保证输出按 vehicleId 升序，便于前端稳定渲染
        Map<Long, VehicleUsageItemVO> itemMap = new TreeMap<>();
        long approvedCount = 0L;
        BigDecimal totalMileage = BigDecimal.ZERO;
        for (AdminVehicleApply apply : applies) {
            Long key = apply.getVehicleId();
            if (key == null) {
                continue;
            }
            VehicleUsageItemVO item =
                    itemMap.computeIfAbsent(
                            key,
                            id -> {
                                VehicleUsageItemVO created = new VehicleUsageItemVO();
                                created.setVehicleId(id);
                                created.setPlateNo("");
                                created.setTotalMileage(BigDecimal.ZERO);
                                created.setTotalExpense(BigDecimal.ZERO);
                                return created;
                            });
            item.setApplyCount(item.getApplyCount() + 1);
            if (apply.getMileage() != null) {
                item.setTotalMileage(item.getTotalMileage().add(apply.getMileage()));
                totalMileage = totalMileage.add(apply.getMileage());
            }
            VehicleApplyStatus status = apply.getStatus();
            if (status == VehicleApplyStatus.APPROVED
                    || status == VehicleApplyStatus.USING
                    || status == VehicleApplyStatus.RETURNED) {
                approvedCount++;
            }
        }

        Map<Long, String> plateNos = loadPlateNos(itemMap.keySet());
        List<VehicleUsageItemVO> items = new ArrayList<>(itemMap.size());
        for (VehicleUsageItemVO item : itemMap.values()) {
            item.setPlateNo(plateNos.getOrDefault(item.getVehicleId(), ""));
            item.setTotalMileage(scaleMoney(item.getTotalMileage()));
            item.setTotalExpense(
                    scaleMoney(expenseByVehicle.getOrDefault(item.getVehicleId(), BigDecimal.ZERO)));
            items.add(item);
        }

        summary.setApplyCount(applies.size());
        summary.setApprovedCount(approvedCount);
        summary.setTotalMileage(scaleMoney(totalMileage));
        summary.setTotalExpense(scaleMoney(totalExpense));
        summary.setItems(items);
        log.info(
                "用车汇总：车辆 {}，区间 {} ~ {}，申请 {} 条，费用 {}",
                vehicleId,
                start,
                end,
                applies.size(),
                summary.getTotalExpense());
        return summary;
    }

    /** 批量查车牌号，车辆不存在或车牌为空由调用方回落空串。 */
    private Map<Long, String> loadPlateNos(Collection<Long> vehicleIds) {
        if (vehicleIds == null || vehicleIds.isEmpty()) {
            return Map.of();
        }
        return vehicleMapper.selectBatchIds(vehicleIds).stream()
                .filter(vehicle -> vehicle.getId() != null && StringUtils.isNotBlank(vehicle.getPlateNo()))
                .collect(Collectors.toMap(AdminVehicle::getId, AdminVehicle::getPlateNo, (a, b) -> a));
    }

    /** 金额 / 里程归一化：null 按 0，保留 2 位小数。 */
    private BigDecimal scaleMoney(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private long nearestRemainDays(VehicleVO vo) {
        Long insurance = vo.getInsuranceRemainDays();
        Long inspection = vo.getInspectionRemainDays();
        if (insurance == null && inspection == null) {
            return Long.MAX_VALUE;
        }
        if (insurance == null) {
            return inspection;
        }
        if (inspection == null) {
            return insurance;
        }
        return Math.min(insurance, inspection);
    }

    private AdminVehicle requireVehicle(Long id) {
        AdminVehicle vehicle = id == null ? null : vehicleMapper.selectById(id);
        if (vehicle == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "车辆不存在");
        }
        return vehicle;
    }

    private boolean existsPlate(String plateNo, Long excludeId) {
        LambdaQueryWrapper<AdminVehicle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminVehicle::getPlateNo, plateNo);
        if (excludeId != null) {
            wrapper.ne(AdminVehicle::getId, excludeId);
        }
        Long count = vehicleMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private Map<Long, AdminVehicle> loadVehicles(List<AdminVehicleApply> records) {
        List<Long> vehicleIds =
                records.stream()
                        .map(AdminVehicleApply::getVehicleId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        if (vehicleIds.isEmpty()) {
            return Map.of();
        }
        return vehicleMapper.selectBatchIds(vehicleIds).stream()
                .collect(Collectors.toMap(AdminVehicle::getId, Function.identity(), (a, b) -> a));
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "用车开始与结束时间不能为空");
        }
        if (!start.isBefore(end)) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "用车开始时间必须早于结束时间");
        }
    }

    private void requireNonNegative(Integer value, String label) {
        if (value != null && value < 0) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, label + "不能为负数");
        }
    }

    /** 是否系统管理员（ROLE_SYS_ADMIN）；与 BPM 权限守卫保持一致的角色判定口径。 */
    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_SYS_ADMIN"::equals);
    }

    private Long parseInstanceId(String instanceId) {
        if (StringUtils.isBlank(instanceId)) {
            return null;
        }
        try {
            return Long.valueOf(instanceId.trim());
        } catch (NumberFormatException e) {
            log.warn("BPM 实例 ID 非数字，无法回写用车申请：{}", instanceId);
            return null;
        }
    }
}
