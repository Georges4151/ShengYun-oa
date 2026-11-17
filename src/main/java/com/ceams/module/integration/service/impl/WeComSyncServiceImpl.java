package com.ceams.module.integration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.bpm.enums.AuditAction;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.bpm.support.BpmSecurityGuard;
import com.ceams.module.integration.client.WeComApiClient;
import com.ceams.module.integration.client.WeComDept;
import com.ceams.module.integration.client.WeComUser;
import com.ceams.module.integration.config.WeComAppProperties;
import com.ceams.module.integration.entity.IntegrationWecomSync;
import com.ceams.module.integration.enums.WeComSyncStatus;
import com.ceams.module.integration.enums.WeComSyncType;
import com.ceams.module.integration.mapper.IntegrationWecomSyncMapper;
import com.ceams.module.integration.service.WeComSyncService;
import com.ceams.module.integration.support.WeComSyncRunner;
import com.ceams.module.integration.support.WeComTokenCache;
import com.ceams.module.integration.vo.WeComSyncLogVO;
import com.ceams.module.integration.vo.WeComSyncResultVO;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 企业微信组织同步服务实现。
 *
 * <p>口径：
 *
 * <ul>
 *   <li>手动入口校验 {@code integration:wecom:sync}；定时任务仅依赖配置开关，不走权限校验。
 *   <li>Redis {@code wecom:sync:lock}（SET NX EX 600）保证同一时刻只有一个批次执行，finally 释放。
 *   <li>企业微信接口失败时批次置 FAILED 并写入 errorMsg，但仍返回批次号（HTTP 200），便于冒烟验证。
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeComSyncServiceImpl implements WeComSyncService {

    /** 手动同步权限码。 */
    private static final String PERMISSION_SYNC = "integration:wecom:sync";

    /** 查看同步日志权限码。 */
    private static final String PERMISSION_VIEW = "integration:wecom:view";

    /** 同步互斥锁 key。 */
    private static final String SYNC_LOCK_KEY = "wecom:sync:lock";

    /** 同步互斥锁有效期（秒）。 */
    private static final long SYNC_LOCK_SECONDS = 600L;

    /** 单页最大条数。 */
    private static final int MAX_PAGE_SIZE = 100;

    /** 失败原因最大长度（与表字段一致）。 */
    private static final int ERROR_MSG_MAX_LENGTH = 1000;

    /** 批次号时间格式。 */
    private static final DateTimeFormatter BATCH_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final IntegrationWecomSyncMapper syncMapper;
    private final WeComApiClient weComApiClient;
    private final WeComTokenCache tokenCache;
    private final WeComSyncRunner syncRunner;
    private final WeComAppProperties properties;
    private final BpmSecurityGuard securityGuard;
    private final BpmAuditService auditService;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public String sync(WeComSyncType type) {
        securityGuard.requirePermission(PERMISSION_SYNC);
        return executeSync(type == null ? WeComSyncType.MANUAL : type);
    }

    @Override
    public WeComSyncResultVO result(String batchNo) {
        IntegrationWecomSync batch = findByBatchNo(batchNo);
        if (batch == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "同步批次不存在");
        }
        WeComSyncResultVO vo = new WeComSyncResultVO();
        vo.setBatchNo(batch.getSyncBatchNo());
        vo.setStatus(batch.getStatus() == null ? null : batch.getStatus().name());
        vo.setTotal(batch.getTotal());
        vo.setSuccessCount(batch.getSuccessCount());
        vo.setFailedCount(batch.getFailedCount());
        vo.setErrorMsg(batch.getErrorMsg());
        return vo;
    }

    @Override
    public PageResult<WeComSyncLogVO> logs(int page, int size, String status) {
        securityGuard.requirePermission(PERMISSION_VIEW);
        if (page < 1) {
            throw BusinessException.of(ErrorCode.VALID_PAGE_BOUNDS, "页码必须不小于 1");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw BusinessException.of(
                    ErrorCode.VALID_PAGE_BOUNDS, "每页条数必须在 1~" + MAX_PAGE_SIZE + " 之间");
        }
        WeComSyncStatus statusFilter = parseStatus(status);
        LambdaQueryWrapper<IntegrationWecomSync> wrapper = new LambdaQueryWrapper<>();
        if (statusFilter != null) {
            wrapper.eq(IntegrationWecomSync::getStatus, statusFilter);
        }
        wrapper.orderByDesc(IntegrationWecomSync::getStartedAt)
                .orderByDesc(IntegrationWecomSync::getId);
        Page<IntegrationWecomSync> pageRequest = new Page<>(page, size);
        IPage<IntegrationWecomSync> result = syncMapper.selectPage(pageRequest, wrapper);
        List<WeComSyncLogVO> records =
                result.getRecords().stream().map(this::toLogVO).toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 定时组织同步：未启用直接跳过，异常全吞仅记日志。 */
    @Scheduled(cron = "${ceams.wecom.sync-cron:0 0 2 * * ?}")
    public void scheduledSync() {
        if (!properties.isConfigured()) {
            log.debug("企业微信应用集成未启用，跳过定时组织同步");
            return;
        }
        try {
            executeSync(WeComSyncType.SCHEDULED);
        } catch (Exception e) {
            log.warn("定时企业微信组织同步失败：{}", e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // 内部流程
    // ------------------------------------------------------------------

    private String executeSync(WeComSyncType type) {
        if (!properties.isConfigured()) {
            throw BusinessException.of(ErrorCode.SYSTEM_SERVICE_UNAVAILABLE, "企业微信集成未启用");
        }
        String lockToken = UUID.randomUUID().toString();
        Boolean acquired =
                stringRedisTemplate
                        .opsForValue()
                        .setIfAbsent(SYNC_LOCK_KEY, lockToken, Duration.ofSeconds(SYNC_LOCK_SECONDS));
        if (!Boolean.TRUE.equals(acquired)) {
            throw BusinessException.of(
                    ErrorCode.BIZ_STATE_CONFLICT, "组织同步正在进行中，请稍后重试");
        }
        try {
            return doSync(type);
        } finally {
            releaseLock(lockToken);
        }
    }

    private String doSync(WeComSyncType type) {
        String batchNo = nextBatchNo();
        IntegrationWecomSync batch = new IntegrationWecomSync();
        batch.setSyncBatchNo(batchNo);
        batch.setSyncType(type);
        batch.setStatus(WeComSyncStatus.RUNNING);
        batch.setStartedAt(LocalDateTime.now());
        syncMapper.insert(batch);
        try {
            String token = tokenCache.getToken();
            List<WeComDept> depts = weComApiClient.listDept(token);
            Map<Long, List<WeComUser>> usersByDept = new LinkedHashMap<>();
            for (WeComDept dept : depts) {
                if (dept == null || dept.id() == null) {
                    continue;
                }
                usersByDept.put(dept.id(), weComApiClient.listDeptUsers(token, dept.id()));
            }
            WeComSyncRunner.SyncResult result = syncRunner.run(depts, usersByDept);
            batch.setTotal(result.total());
            batch.setSuccessCount(result.success());
            batch.setFailedCount(result.failed());
            batch.setStatus(resolveStatus(result));
            batch.setFinishedAt(LocalDateTime.now());
            syncMapper.updateById(batch);
            recordAudit(batch);
            log.info(
                    "企业微信组织同步批次 {} 完成：status={}，total={}，success={}，failed={}",
                    batchNo,
                    batch.getStatus(),
                    batch.getTotal(),
                    batch.getSuccessCount(),
                    batch.getFailedCount());
            return batchNo;
        } catch (Exception e) {
            markFailed(batch, e);
            return batchNo;
        }
    }

    private void markFailed(IntegrationWecomSync batch, Exception cause) {
        batch.setStatus(WeComSyncStatus.FAILED);
        batch.setErrorMsg(truncate(cause.getMessage(), ERROR_MSG_MAX_LENGTH));
        batch.setFinishedAt(LocalDateTime.now());
        try {
            syncMapper.updateById(batch);
        } catch (Exception e) {
            log.warn("回写企业微信同步批次失败状态异常：batchNo={}，原因={}", batch.getSyncBatchNo(), e.getMessage());
        }
        recordAudit(batch);
        log.warn(
                "企业微信组织同步批次 {} 失败：{}",
                batch.getSyncBatchNo(),
                cause.getMessage());
    }

    private void recordAudit(IntegrationWecomSync batch) {
        try {
            Map<String, Object> after = new LinkedHashMap<>();
            after.put("syncBatchNo", batch.getSyncBatchNo());
            after.put("syncType", batch.getSyncType());
            after.put("status", batch.getStatus());
            after.put("total", batch.getTotal());
            after.put("successCount", batch.getSuccessCount());
            after.put("failedCount", batch.getFailedCount());
            after.put("errorMsg", batch.getErrorMsg());
            auditService.record(
                    TARGET_WECOM_SYNC,
                    batch.getId(),
                    AuditAction.WECOM_SYNC,
                    null,
                    after,
                    null,
                    securityGuard.currentUserId());
        } catch (Exception e) {
            log.warn("写入企业微信组织同步审计失败：batchNo={}，原因={}", batch.getSyncBatchNo(), e.getMessage());
        }
    }

    private void releaseLock(String lockToken) {
        try {
            String holder = stringRedisTemplate.opsForValue().get(SYNC_LOCK_KEY);
            if (lockToken.equals(holder)) {
                stringRedisTemplate.delete(SYNC_LOCK_KEY);
            }
        } catch (Exception e) {
            log.warn("释放企业微信组织同步锁异常：{}", e.getMessage());
        }
    }

    private IntegrationWecomSync findByBatchNo(String batchNo) {
        if (!StringUtils.hasText(batchNo)) {
            return null;
        }
        return syncMapper.selectOne(
                new LambdaQueryWrapper<IntegrationWecomSync>()
                        .eq(IntegrationWecomSync::getSyncBatchNo, batchNo.trim())
                        .last("LIMIT 1"));
    }

    private WeComSyncStatus resolveStatus(WeComSyncRunner.SyncResult result) {
        if (result.failed() == 0) {
            return WeComSyncStatus.SUCCESS;
        }
        return result.success() > 0 ? WeComSyncStatus.PARTIAL : WeComSyncStatus.FAILED;
    }

    private WeComSyncStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return WeComSyncStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "同步状态非法：" + status);
        }
    }

    private String nextBatchNo() {
        String timestamp = LocalDateTime.now().format(BATCH_TIME_FORMAT);
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "WECOM-" + timestamp + "-" + random;
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private WeComSyncLogVO toLogVO(IntegrationWecomSync batch) {
        WeComSyncLogVO vo = new WeComSyncLogVO();
        vo.setId(batch.getId());
        vo.setBatchNo(batch.getSyncBatchNo());
        vo.setSyncType(batch.getSyncType() == null ? null : batch.getSyncType().name());
        vo.setTotal(batch.getTotal());
        vo.setSuccessCount(batch.getSuccessCount());
        vo.setFailedCount(batch.getFailedCount());
        vo.setStatus(batch.getStatus() == null ? null : batch.getStatus().name());
        vo.setErrorMsg(batch.getErrorMsg());
        vo.setStartedAt(batch.getStartedAt());
        vo.setFinishedAt(batch.getFinishedAt());
        return vo;
    }
}
