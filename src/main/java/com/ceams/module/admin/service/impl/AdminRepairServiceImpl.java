package com.ceams.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.admin.dto.RepairAssignRequest;
import com.ceams.module.admin.dto.RepairCreateRequest;
import com.ceams.module.admin.dto.RepairFinishRequest;
import com.ceams.module.admin.dto.RepairProcessRequest;
import com.ceams.module.admin.entity.AdminRepair;
import com.ceams.module.admin.enums.RepairCategory;
import com.ceams.module.admin.enums.RepairStatus;
import com.ceams.module.admin.mapper.AdminRepairMapper;
import com.ceams.module.admin.service.AdminRepairService;
import com.ceams.module.admin.support.AdminConverter;
import com.ceams.module.admin.support.AdminNoGenerator;
import com.ceams.module.admin.vo.RepairStatsVO;
import com.ceams.module.admin.vo.RepairVO;
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.hr.util.HrPageUtil;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.mapper.UserMapper;
import com.ceams.module.notification.enums.NotificationType;
import com.ceams.module.notification.service.NotificationService;
import com.ceams.module.notification.support.NotificationDispatchSupport;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
 * 行政报修服务实现。
 *
 * <p>状态机严格按 SUBMITTED → ASSIGNED → PROCESSING → DONE → ACCEPTED 流转，
 * REJECTED 仅由审批回调写入；写操作统一使用「当前状态 + 期望状态」条件更新，
 * 影响行数为 0 即抛 {@code BIZ_STATE_CONFLICT}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRepairServiceImpl implements AdminRepairService {

    /** 工单号生成冲突最大重试次数。 */
    private static final int REPAIR_NO_MAX_RETRY = 3;

    /** 单张工单最多图片数。 */
    private static final int MAX_IMAGES = 6;

    /** 完成率保留 4 位小数。 */
    private static final int RATE_SCALE = 4;

    /** 平均时长保留 2 位小数。 */
    private static final int HOURS_SCALE = 2;

    /** 同一工单派单提醒去重窗口（小时）。 */
    private static final long DEDUP_HOURS = 24L;

    private final AdminRepairMapper repairMapper;
    private final AdminNoGenerator noGenerator;
    private final BpmInstanceService bpmInstanceService;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final NotificationDispatchSupport notificationDispatchSupport;

    @Override
    @Transactional
    public String create(RepairCreateRequest request) {
        int imageCount = request.getImages() == null ? 0 : request.getImages().size();
        if (imageCount > MAX_IMAGES) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "图片最多上传 " + MAX_IMAGES + " 张");
        }

        Long reporterId = SecurityUtils.getCurrentUserIdOrDefault();
        AdminRepair repair = new AdminRepair();
        repair.setReporterId(reporterId);
        repair.setDeptId(currentDeptId(reporterId));
        repair.setLocation(request.getLocation().trim());
        repair.setCategory(
                request.getCategory() == null ? RepairCategory.OTHER : request.getCategory());
        repair.setDescription(request.getDescription());
        repair.setImagesJson(AdminConverter.joinImages(request.getImages()));
        repair.setStatus(RepairStatus.SUBMITTED);

        boolean inserted = false;
        for (int attempt = 1; attempt <= REPAIR_NO_MAX_RETRY; attempt++) {
            repair.setRepairNo(noGenerator.nextRepairNo());
            try {
                repairMapper.insert(repair);
                inserted = true;
                break;
            } catch (DuplicateKeyException e) {
                log.warn("报修工单号 {} 已存在，第 {} 次重新取号", repair.getRepairNo(), attempt);
            }
        }
        if (!inserted) {
            throw BusinessException.of(ErrorCode.BIZ_DUPLICATE_CODE, "工单号生成冲突，请稍后重试");
        }

        InstanceStartRequest start = new InstanceStartRequest();
        start.setTemplateCode(BpmBizType.ADMIN_REPAIR.name());
        start.setBizType(BpmBizType.ADMIN_REPAIR.name());
        start.setBizId(repair.getId());
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("repairNo", repair.getRepairNo());
        variables.put("reporterId", String.valueOf(reporterId));
        variables.put("location", repair.getLocation());
        variables.put("category", repair.getCategory().name());
        start.setVariables(variables);
        String instanceId = bpmInstanceService.start(start);

        repair.setBpmInstanceId(parseInstanceId(instanceId));
        repairMapper.updateById(repair);
        log.info(
                "报修工单 {}（{}）已提交，BPM 实例 {}",
                repair.getId(),
                repair.getRepairNo(),
                instanceId);
        return String.valueOf(repair.getId());
    }

    @Override
    public PageResult<RepairVO> page(
            RepairStatus status, Long assigneeId, long page, long size) {
        Page<AdminRepair> p = HrPageUtil.resolve(page, size);
        LambdaQueryWrapper<AdminRepair> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(AdminRepair::getStatus, status);
        }
        if (assigneeId != null) {
            wrapper.eq(AdminRepair::getAssigneeId, assigneeId);
        }
        wrapper.orderByDesc(AdminRepair::getCreateTime);
        IPage<AdminRepair> result = repairMapper.selectPage(p, wrapper);
        List<RepairVO> records =
                result.getRecords().stream().map(AdminConverter::toRepairVO).toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public List<RepairVO> my() {
        Long reporterId = SecurityUtils.getCurrentUserIdOrDefault();
        return repairMapper
                .selectList(
                        new LambdaQueryWrapper<AdminRepair>()
                                .eq(AdminRepair::getReporterId, reporterId)
                                .orderByDesc(AdminRepair::getCreateTime))
                .stream()
                .map(AdminConverter::toRepairVO)
                .toList();
    }

    @Override
    @Transactional
    public void assign(Long id, RepairAssignRequest request) {
        AdminRepair repair = requireRepair(id);
        if (repair.getStatus() != RepairStatus.ASSIGNED) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅审批通过的工单可派单");
        }
        if (request == null || request.getAssigneeId() == null) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "维修人不能为空");
        }
        // 审批回调会先写入 assignedAt，因此首次派单只能以「更新前 assigneeId 为空」判定
        boolean firstAssign = repair.getAssigneeId() == null;

        AdminRepair patch = new AdminRepair();
        // 条件更新已保证并发安全，version 置空避免叠加乐观锁导致条件更新失效
        patch.setVersion(null);
        patch.setAssigneeId(request.getAssigneeId());
        patch.setAssignedAt(LocalDateTime.now());
        if (StringUtils.isNotBlank(request.getRemark())) {
            patch.setRemark(request.getRemark());
        }
        int rows =
                repairMapper.update(
                        patch,
                        new LambdaUpdateWrapper<AdminRepair>()
                                .eq(AdminRepair::getId, id)
                                .eq(AdminRepair::getStatus, RepairStatus.ASSIGNED));
        if (rows == 0) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "工单状态已变更，请刷新后重试");
        }
        log.info("报修工单 {} 派单给 {}", id, request.getAssigneeId());
        if (firstAssign) {
            notifyAssignee(repair, request.getAssigneeId());
        }
    }

    /** 首次派单通知维修人：24 小时内同一工单已提醒过则跳过，通知失败不影响派单结果。 */
    private void notifyAssignee(AdminRepair repair, Long assigneeId) {
        if (assigneeId == null) {
            return;
        }
        String linkUrl = "ceams://admin/repair/" + repair.getId();
        try {
            if (notificationService.existsRecent(
                    NotificationType.ADMIN, linkUrl, LocalDateTime.now().minusHours(DEDUP_HOURS))) {
                return;
            }
            String content =
                    "报修工单 "
                            + (repair.getRepairNo() == null ? repair.getId() : repair.getRepairNo())
                            + " 已派给您处理，请及时响应。";
            notificationDispatchSupport.send(
                    NotificationType.ADMIN, "新报修工单已派给您", content, linkUrl, List.of(assigneeId));
        } catch (Exception e) {
            log.warn(
                    "报修派单通知发送失败（已忽略，不影响派单结果）：repairId={}，assigneeId={}，原因={}",
                    repair.getId(),
                    assigneeId,
                    e.getMessage(),
                    e);
        }
    }

    @Override
    @Transactional
    public void process(Long id, RepairProcessRequest request) {
        AdminRepair repair = requireRepair(id);
        requireAssigneeOrAdmin(repair, "仅维修人或管理员可开始处理");
        if (repair.getStatus() != RepairStatus.ASSIGNED) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅已受理的工单可开始处理");
        }

        AdminRepair patch = new AdminRepair();
        patch.setVersion(null);
        patch.setStatus(RepairStatus.PROCESSING);
        patch.setProcessedAt(LocalDateTime.now());
        if (request != null && StringUtils.isNotBlank(request.getRemark())) {
            patch.setRemark(request.getRemark());
        }
        int rows =
                repairMapper.update(
                        patch,
                        new LambdaUpdateWrapper<AdminRepair>()
                                .eq(AdminRepair::getId, id)
                                .eq(AdminRepair::getStatus, RepairStatus.ASSIGNED));
        if (rows == 0) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "工单状态已变更，请刷新后重试");
        }
        log.info("报修工单 {} 开始处理", id);
    }

    @Override
    @Transactional
    public void finish(Long id, RepairFinishRequest request) {
        AdminRepair repair = requireRepair(id);
        requireAssigneeOrAdmin(repair, "仅维修人或管理员可登记完工");
        if (repair.getStatus() != RepairStatus.PROCESSING) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅处理中的工单可登记完工");
        }

        AdminRepair patch = new AdminRepair();
        patch.setVersion(null);
        patch.setStatus(RepairStatus.DONE);
        patch.setFinishedAt(LocalDateTime.now());
        if (request != null && StringUtils.isNotBlank(request.getRemark())) {
            patch.setRemark(request.getRemark());
        }
        int rows =
                repairMapper.update(
                        patch,
                        new LambdaUpdateWrapper<AdminRepair>()
                                .eq(AdminRepair::getId, id)
                                .eq(AdminRepair::getStatus, RepairStatus.PROCESSING));
        if (rows == 0) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "工单状态已变更，请刷新后重试");
        }
        log.info("报修工单 {} 已完工", id);
    }

    @Override
    @Transactional
    public void accept(Long id) {
        AdminRepair repair = requireRepair(id);
        requireReporterOrAdmin(repair, "仅报修人或管理员可验收");
        if (repair.getStatus() != RepairStatus.DONE) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅已完工的工单可验收");
        }

        AdminRepair patch = new AdminRepair();
        patch.setVersion(null);
        patch.setStatus(RepairStatus.ACCEPTED);
        patch.setAcceptedAt(LocalDateTime.now());
        int rows =
                repairMapper.update(
                        patch,
                        new LambdaUpdateWrapper<AdminRepair>()
                                .eq(AdminRepair::getId, id)
                                .eq(AdminRepair::getStatus, RepairStatus.DONE));
        if (rows == 0) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "工单状态已变更，请刷新后重试");
        }
        log.info("报修工单 {} 已验收", id);
    }

    @Override
    public RepairStatsVO stats(
            LocalDate start, LocalDate end, String category, Long employeeUserId) {
        LambdaQueryWrapper<AdminRepair> wrapper = new LambdaQueryWrapper<>();
        if (start != null) {
            wrapper.ge(AdminRepair::getCreateTime, start.atStartOfDay());
        }
        if (end != null) {
            wrapper.lt(AdminRepair::getCreateTime, end.plusDays(1).atStartOfDay());
        }
        if (StringUtils.isNotBlank(category)) {
            RepairCategory filter = RepairCategory.from(category);
            if (filter == null) {
                throw BusinessException.of(
                        ErrorCode.VALID_ILLEGAL_ARGUMENT, "报修分类非法：" + category);
            }
            wrapper.eq(AdminRepair::getCategory, filter);
        }
        if (employeeUserId != null) {
            wrapper.eq(AdminRepair::getReporterId, employeeUserId);
        }
        List<AdminRepair> repairs = repairMapper.selectList(wrapper);

        long submitted = 0L;
        long processing = 0L;
        long completed = 0L;
        long handledCount = 0L;
        double handledHours = 0D;
        for (AdminRepair repair : repairs) {
            RepairStatus status = repair.getStatus();
            if (status == RepairStatus.SUBMITTED) {
                submitted++;
            } else if (status == RepairStatus.ASSIGNED || status == RepairStatus.PROCESSING) {
                processing++;
            } else if (status == RepairStatus.DONE || status == RepairStatus.ACCEPTED) {
                completed++;
            }
            if (repair.getAssignedAt() != null && repair.getFinishedAt() != null) {
                handledHours +=
                        Duration.between(repair.getAssignedAt(), repair.getFinishedAt()).toMinutes()
                                / 60.0D;
                handledCount++;
            }
        }

        RepairStatsVO stats = new RepairStatsVO();
        stats.setTotalCount(repairs.size());
        stats.setSubmittedCount(submitted);
        stats.setProcessingCount(processing);
        stats.setCompletedCount(completed);
        stats.setCompletionRate(
                repairs.isEmpty()
                        ? BigDecimal.ZERO.setScale(RATE_SCALE, RoundingMode.HALF_UP)
                        : BigDecimal.valueOf(completed)
                                .divide(
                                        BigDecimal.valueOf(repairs.size()),
                                        RATE_SCALE,
                                        RoundingMode.HALF_UP));
        stats.setAvgHandleHours(
                handledCount == 0L
                        ? BigDecimal.ZERO.setScale(HOURS_SCALE, RoundingMode.HALF_UP)
                        : BigDecimal.valueOf(handledHours / handledCount)
                                .setScale(HOURS_SCALE, RoundingMode.HALF_UP));
        log.info(
                "报修统计：区间 {} ~ {}，分类 {}，总数 {}，完成 {}",
                start,
                end,
                category,
                repairs.size(),
                completed);
        return stats;
    }

    private AdminRepair requireRepair(Long id) {
        AdminRepair repair = id == null ? null : repairMapper.selectById(id);
        if (repair == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "报修工单不存在");
        }
        return repair;
    }

    /** 维修人本人或管理员才可推进处理 / 完工。 */
    private void requireAssigneeOrAdmin(AdminRepair repair, String message) {
        Long currentUserId = SecurityUtils.getCurrentUserIdOrDefault();
        if (!Objects.equals(repair.getAssigneeId(), currentUserId) && !isAdmin()) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, message);
        }
    }

    /** 报修人本人或管理员才可验收。 */
    private void requireReporterOrAdmin(AdminRepair repair, String message) {
        Long currentUserId = SecurityUtils.getCurrentUserIdOrDefault();
        if (!Objects.equals(repair.getReporterId(), currentUserId) && !isAdmin()) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, message);
        }
    }

    /** 当前用户部门快照；未登录或查不到用户时留空。 */
    private Long currentDeptId(Long userId) {
        if (userId == null || userId <= 0L) {
            return null;
        }
        SysUser user = userMapper.selectById(userId);
        return user == null ? null : user.getDeptId();
    }

    /** 是否系统管理员（ROLE_SYS_ADMIN）；与行政其他服务保持一致的判定口径。 */
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
            log.warn("BPM 实例 ID 非数字，无法回报修工单：{}", instanceId);
            return null;
        }
    }
}
