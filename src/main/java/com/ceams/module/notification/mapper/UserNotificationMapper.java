package com.ceams.module.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ceams.module.notification.entity.UserNotification;
import com.ceams.module.notification.enums.NotificationType;
import com.ceams.module.notification.vo.UserNotificationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 用户通知 Mapper。 */
@Mapper
public interface UserNotificationMapper extends BaseMapper<UserNotification> {

    /**
     * 「我的通知」联表分页：用户通知表 join 通知表，可按通知类型过滤。
     *
     * <p>只返回未删除的用户通知与未删除的通知主记录；结果按用户通知创建时间倒序，
     * 由分页插件生成 count 语句，避免先查全部再内存过滤。
     *
     * @param page 分页参数（MyBatis-Plus 分页插件要求首参为 IPage）
     * @param userId 当前用户 ID
     * @param type 通知类型，为空表示不过滤
     * @return 可直接映射为 {@link UserNotificationVO} 的分页结果
     */
    @Select(
            """
            <script>
            SELECT un.id,
                   un.notification_id,
                   un.user_id,
                   un.is_read,
                   un.read_time,
                   un.channel,
                   un.push_status,
                   un.push_time,
                   un.create_time,
                   n.title,
                   n.content,
                   n.link_url,
                   n.notification_type,
                   n.priority,
                   n.publish_time
            FROM biz_user_notification un
            INNER JOIN biz_notification n
                    ON n.id = un.notification_id
                   AND n.deleted = 0
            WHERE un.user_id = #{userId}
              AND un.deleted = 0
            <if test="type != null">
              AND n.notification_type = #{type}
            </if>
            ORDER BY un.create_time DESC, un.id DESC
            </script>
            """)
    IPage<UserNotificationVO> selectMinePage(
            IPage<UserNotificationVO> page,
            @Param("userId") Long userId,
            @Param("type") NotificationType type);
}
