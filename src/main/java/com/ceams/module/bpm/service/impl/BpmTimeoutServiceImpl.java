package com.ceams.module.bpm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.bpm.config.BpmProperties;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.entity.BpmTask;
import com.ceams.module.bpm.enums.BpmTaskStatus;
import com.ceams.module.bpm.mapper.BpmInstanceMapper;
import com.ceams.module.bpm.mapper.BpmTaskMapper;
import com.ceams.module.bpm.service.BpmTimeoutService;
import com.ceams.module.bpm.support.BpmNotificationPublisher;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 超时扫描提醒实现。
 *
 * <p>查询只走 {@code status + due_at + reminded_at} 索引，不改变任务状态、不自动流转；
 * 提醒通过条件更新抢占 {@code reminded_at}，抢到才发送，保证同一任务仅提醒一次。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpmTimeoutServiceImpl implements BpmTimeoutService {

    private final BpmTaskMapper taskMapper;
    private final BpmInstanceMapper instanceMapper;
    private final BpmNotificationPublisher notificationPublisher;
    private final BpmProperties bpmProperties;

    @Override
    public List<BpmTask> listOverdueTasks(int shardIndex, int shardTotal, Integer limit) {
        int total = Math.max(shardTotal, 1);
        if (shardIndex < 0 || shardIndex >= total) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "分片参数非法：shardIndex/shardTotal");
        }
        int batchSize =
                limit == null || limit <= 0
                        ? Math.max(bpmProperties.getTimeout().getBatchSize(), 1)
                        : Math.min(limit, Math.max(bpmProperties.getTimeout().getBatchSize(), 1));
        LambdaQueryWrapper<BpmTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BpmTask::getStatus, BpmTaskStatus.PENDING)
                .lt(BpmTask::getDueAt, LocalDateTime.now())
                // 已经提醒过的任务不再返回，保证分片扫描不重复命中
                .isNull(BpmTask::getRemindedAt)
                .apply(total > 1, "MOD(id, {0}) = {1}", total, shardIndex)
                .orderByAsc(BpmTask::getDueAt);
        Page<BpmTask> page = new Page<>(1, batchSize, false);
        return taskMapper.selectPage(page, wrapper).getRecords();
    }

    @Override
    public int scanAndRemind() {
        BpmProperties.Timeout timeout = bpmProperties.getTimeout();
        if (!timeout.isEnabled()) {
            log.info("BPM 超时提醒已关闭（ceams.bpm.timeout.enabled=false），跳过本轮扫描");
            return 0;
        }
        int shardTotal = Math.max(timeout.getShardTotal(), 1);
        int batchSize = Math.max(timeout.getBatchSize(), 1);
        int reminded = 0;
        for (int shardIndex = 0; shardIndex < shardTotal; shardIndex++) {
            reminded += remindShard(shardIndex, shardTotal, batchSize);
        }
        if (reminded > 0) {
            log.info("BPM 超时提醒完成：本次共提醒 {} 条任务", reminded);
        }
        return reminded;
    }

    /** 扫描单个分片：批量载入实例（避免逐条查询），逐条抢占提醒权后发送通知。 */
    private int remindShard(int shardIndex, int shardTotal, int batchSize) {
        List<BpmTask> tasks = listOverdueTasks(shardIndex, shardTotal, batchSize);
        if (tasks.isEmpty()) {
            return 0;
        }
        Map<Long, BpmInstance> instanceMap = loadInstances(tasks);
        int reminded = 0;
        for (BpmTask task : tasks) {
            if (!claimReminder(task)) {
                // 其他实例/线程已提醒，跳过
                continue;
            }
            reminded++;
            notificationPublisher.onTaskOverdue(instanceMap.get(task.getInstanceId()), task);
        }
        return reminded;
    }

    /**
     * 抢占提醒权：条件更新 {@code reminded_at IS NULL} 才置为当前时间。
     *
     * <p>更新成功返回 true（抢到提醒权），失败返回 false（已被并发请求提醒）。
     */
    private boolean claimReminder(BpmTask task) {
        LocalDateTime now = LocalDateTime.now();
        BpmTask patch = new BpmTask();
        patch.setRemindedAt(now);
        int updated =
                taskMapper.update(
                        patch,
                        new LambdaUpdateWrapper<BpmTask>()
                                .eq(BpmTask::getId, task.getId())
                                .eq(BpmTask::getStatus, BpmTaskStatus.PENDING)
                                .isNull(BpmTask::getRemindedAt));
        if (updated == 0) {
            return false;
        }
        task.setRemindedAt(now);
        return true;
    }

    /** 批量载入任务所属实例（一次 IN 查询）。 */
    private Map<Long, BpmInstance> loadInstances(List<BpmTask> tasks) {
        List<Long> instanceIds =
                tasks.stream()
                        .map(BpmTask::getInstanceId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        if (instanceIds.isEmpty()) {
            return Map.of();
        }
        List<BpmInstance> instances = instanceMapper.selectBatchIds(instanceIds);
        if (instances == null || instances.isEmpty()) {
            return Map.of();
        }
        return instances.stream()
                .filter(instance -> instance.getId() != null)
                .collect(Collectors.toMap(BpmInstance::getId, instance -> instance, (a, b) -> a));
    }
}
