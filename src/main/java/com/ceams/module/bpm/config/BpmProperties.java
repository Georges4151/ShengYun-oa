package com.ceams.module.bpm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** BPM 内核配置（{@code ceams.bpm.*}）。 */
@Data
@ConfigurationProperties(prefix = "ceams.bpm")
public class BpmProperties {

    /** 节点未配置 dueHours 时的默认超时时长（小时）。 */
    private int defaultDueHours = 24;

    /**
     * 部门主管解析使用的角色编码（已废弃，仅保留配置兼容）。
     *
     * <p>V17 补齐 IAM 组织字段后，DEPT_LEADER 改为按发起人
     * {@code sys_dept.leader_id} → {@code sys_user.leader_id} 两级解析，本配置不再生效。
     */
    @Deprecated
    private String deptLeaderRoleCode = "";

    /** 超时提醒配置（{@code ceams.bpm.timeout.*}）。 */
    private Timeout timeout = new Timeout();

    /** 超时提醒配置（{@code ceams.bpm.timeout.*}）。 */
    @Data
    public static class Timeout {

        /** 是否启用超时提醒定时任务（默认开启）。 */
        private boolean enabled = true;

        /** 定时任务 cron 表达式（默认每 30 分钟一次）。 */
        private String cron = "0 0/30 * * * ?";

        /** 单批查询条数上限。 */
        private int batchSize = 200;

        /** 默认分片总数（用于外部调度按片拉取）。 */
        private int shardTotal = 4;
    }
}
