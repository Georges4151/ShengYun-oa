package com.ceams.module.bpm.service;

import com.ceams.module.bpm.entity.BpmTask;
import java.util.List;

/**
 * BPM 超时服务：查询已超时任务，并按「每任务仅提醒一次」执行扫描提醒。
 */
public interface BpmTimeoutService {

    /**
     * 按分片查询已超时的待处理任务（尚未提醒）。
     *
     * <p>查询条件固定为 {@code status=PENDING AND due_at < now() AND reminded_at IS NULL}，
     * 命中 {@code idx_bpm_task_status_due_reminded(status, due_at, reminded_at)}；
     * 分片用于避免一次性全表扫描。
     *
     * @param shardIndex 分片序号，从 0 开始
     * @param shardTotal 分片总数（>=1；等于 1 表示不分片）
     * @param limit 单批返回上限（为空或非正数时取配置 batchSize）
     * @return 超时任务列表（按 due_at 升序）
     */
    List<BpmTask> listOverdueTasks(int shardIndex, int shardTotal, Integer limit);

    /**
     * 扫描全部超时任务并发送站内提醒。
     *
     * <p>每条任务通过条件更新（{@code WHERE id = ? AND reminded_at IS NULL}）抢占提醒权，
     * 更新成功才发送通知并回写 remindedAt，保证并发/多实例部署下同一任务只提醒一次。
     *
     * @return 本次实际提醒的任务条数
     */
    int scanAndRemind();
}
