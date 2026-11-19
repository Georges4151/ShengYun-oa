package com.ceams.module.notification.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.notification.dto.NotificationCreateRequest;
import com.ceams.module.notification.dto.NotificationPublishRequest;
import com.ceams.module.notification.dto.NotificationRecipientRoleVO;
import com.ceams.module.notification.dto.NotificationRecipientUserVO;
import com.ceams.module.notification.dto.NotificationUpdateRequest;
import com.ceams.module.notification.enums.NotificationPriority;
import com.ceams.module.notification.enums.NotificationStatus;
import com.ceams.module.notification.enums.NotificationType;
import com.ceams.module.notification.vo.NotificationVO;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/** 通知服务。 */
public interface NotificationService {

    PageResult<NotificationVO> page(
            long page,
            long size,
            NotificationType type,
            NotificationStatus status,
            NotificationPriority priority,
            LocalDateTime startTime,
            LocalDateTime endTime);

    NotificationVO detail(Long id);

    String create(NotificationCreateRequest request);

    /**
     * 创建并直接发布通知，定向投递给指定用户（站内信渠道，幂等跳过已存在记录）。
     *
     * @param request 通知内容
     * @param targetUserIds 目标用户 ID 集合（去重、忽略空值）
     * @return 通知 ID 字符串
     */
    String createAndPublishToUsers(
            NotificationCreateRequest request, Collection<Long> targetUserIds);

    /**
     * 同一类型 + 同一深链在指定时间之后是否已存在通知（业务事件提醒的 24 小时去重判断）。
     *
     * @param type 通知类型
     * @param linkUrl 通知深链（内含业务单据 ID，天然按单据去重）
     * @param since 统计起点（含），为空表示不限时间
     * @return 存在则返回 true
     */
    boolean existsRecent(NotificationType type, String linkUrl, LocalDateTime since);

    void update(Long id, NotificationUpdateRequest request);

    void delete(Long id);

    /**
     * 按用户偏好分发并发布（兼容旧调用，未指定接收范围时使用）。
     *
     * @param id 通知 ID
     */
    void publish(Long id);

    /**
     * 按指定接收范围发布通知，仅生成系统站内信（不受用户偏好限制）。
     *
     * <p>{@code request} 为 {@code null} 时保持旧的偏好分发行为，便于兼容未改造的调用方。
     *
     * @param id 通知 ID
     * @param request 发布请求（接收范围）；为空表示按用户偏好分发
     * @return 实际送达（写入系统站内信）的人数
     */
    int publish(Long id, NotificationPublishRequest request);

    /** 查询启用角色列表（发布范围选择用）。 */
    List<NotificationRecipientRoleVO> listRecipientRoles();

    /**
     * 按姓名或账号模糊查询启用用户（发布范围选择用）。
     *
     * @param keyword 关键字，为空时返回前 50 条启用用户
     * @return 最多 50 条启用用户
     */
    List<NotificationRecipientUserVO> searchRecipientUsers(String keyword);

    void revoke(Long id);
}
