package com.ceams.module.notification.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.crypto.SensitiveTypeHandler;
import com.ceams.common.exception.BusinessException;
import com.ceams.module.iam.entity.SysRole;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.entity.UserRole;
import com.ceams.module.iam.mapper.RoleMapper;
import com.ceams.module.iam.mapper.UserMapper;
import com.ceams.module.iam.mapper.UserRoleMapper;
import com.ceams.module.notification.converter.NotificationConverter;
import com.ceams.module.notification.dto.NotificationCreateRequest;
import com.ceams.module.notification.dto.NotificationPublishRequest;
import com.ceams.module.notification.dto.NotificationPublishScope;
import com.ceams.module.notification.dto.NotificationRecipientRoleVO;
import com.ceams.module.notification.dto.NotificationRecipientUserVO;
import com.ceams.module.notification.entity.Notification;
import com.ceams.module.notification.entity.UserNotification;
import com.ceams.module.notification.entity.UserNotificationPreference;
import com.ceams.module.notification.enums.NotificationPriority;
import com.ceams.module.notification.enums.NotificationStatus;
import com.ceams.module.notification.enums.NotificationType;
import com.ceams.module.notification.enums.PushChannel;
import com.ceams.module.notification.enums.PushStatus;
import com.ceams.module.notification.mapper.NotificationMapper;
import com.ceams.module.notification.mapper.UserNotificationMapper;
import com.ceams.module.notification.mapper.UserNotificationPreferenceMapper;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** {@link NotificationServiceImpl} 定向发布 / 接收范围发布单元测试。 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private NotificationMapper notificationMapper;
    @Mock private NotificationConverter notificationConverter;
    @Mock private UserNotificationPreferenceMapper userNotificationPreferenceMapper;
    @Mock private UserNotificationMapper userNotificationMapper;
    @Mock private UserMapper userMapper;
    @Mock private RoleMapper roleMapper;
    @Mock private UserRoleMapper userRoleMapper;

    private NotificationServiceImpl service;

    /** MyBatis-Plus Lambda 条件依赖 TableInfo 缓存，纯单元测试需手工初始化 IAM 实体元数据。 */
    @BeforeAll
    static void initTableInfoCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        // SysUser.phone 使用构造注入的 SensitiveTypeHandler，单元测试无 Spring 容器，注册占位实例即可
        configuration.getTypeHandlerRegistry().register(new SensitiveTypeHandler(null));
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, SysUser.class);
        TableInfoHelper.initTableInfo(assistant, SysRole.class);
        TableInfoHelper.initTableInfo(assistant, UserRole.class);
    }

    @BeforeEach
    void setUp() {
        service =
                new NotificationServiceImpl(
                        notificationMapper,
                        notificationConverter,
                        userNotificationPreferenceMapper,
                        userNotificationMapper,
                        userMapper,
                        roleMapper,
                        userRoleMapper);
    }

    @Test
    void 定向投递_去重后为每个用户写入站内信并置发布状态() {
        stubInsertedNotification(1001L);
        when(userNotificationMapper.selectCount(any())).thenReturn(0L);

        String id = service.createAndPublishToUsers(request(), List.of(11L, 11L, 22L, 22L));

        assertEquals("1001", id);
        ArgumentCaptor<Notification> notificationCaptor =
                ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(notificationCaptor.capture());
        Notification saved = notificationCaptor.getValue();
        assertEquals(NotificationStatus.PUBLISHED, saved.getStatus());
        assertNotNull(saved.getPublishTime());
        verify(userNotificationMapper, times(2)).insert(any(UserNotification.class));
    }

    @Test
    void 定向投递_同一通知用户渠道已存在则跳过() {
        stubInsertedNotification(1002L);
        when(userNotificationMapper.selectCount(any())).thenReturn(1L);

        service.createAndPublishToUsers(request(), List.of(11L));

        verify(userNotificationMapper, never()).insert(any(UserNotification.class));
    }

    @Test
    void 定向投递_空用户列表不写入站内信() {
        stubInsertedNotification(1003L);

        service.createAndPublishToUsers(request(), List.of());

        verifyNoInteractions(userNotificationMapper);
    }

    @Test
    void 范围发布_全员_为所有启用用户写入站内信() {
        when(notificationMapper.selectById(2001L)).thenReturn(draftNotification(2001L));
        when(userMapper.selectList(any())).thenReturn(List.of(user(11L), user(22L), user(33L)));

        int delivered =
                service.publish(2001L, publishRequest(NotificationPublishScope.ALL, null, null));

        assertEquals(3, delivered);
        verify(userNotificationMapper, times(3)).insert(any(UserNotification.class));
    }

    @Test
    void 范围发布_按角色_关联用户去重后写入站内信() {
        when(notificationMapper.selectById(2002L)).thenReturn(draftNotification(2002L));
        when(userRoleMapper.selectList(any()))
                .thenReturn(List.of(userRole(11L, 5L), userRole(22L, 5L)));
        when(userMapper.selectList(any())).thenReturn(List.of(user(11L), user(22L)));

        int delivered =
                service.publish(
                        2002L, publishRequest(NotificationPublishScope.ROLES, List.of(5L), null));

        assertEquals(2, delivered);
        verify(userNotificationMapper, times(2)).insert(any(UserNotification.class));
    }

    @Test
    void 范围发布_按用户_重复用户只写入一次() {
        when(notificationMapper.selectById(2003L)).thenReturn(draftNotification(2003L));
        when(userMapper.selectList(any())).thenReturn(List.of(user(11L)));

        int delivered =
                service.publish(
                        2003L,
                        publishRequest(NotificationPublishScope.USERS, null, List.of(11L, 11L)));

        assertEquals(1, delivered);
        verify(userNotificationMapper, times(1)).insert(any(UserNotification.class));
    }

    @Test
    void 范围发布_角色为空抛出业务异常() {
        when(notificationMapper.selectById(2004L)).thenReturn(draftNotification(2004L));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                service.publish(
                                        2004L,
                                        publishRequest(
                                                NotificationPublishScope.ROLES, List.of(), null)));

        assertEquals("请选择接收角色", exception.getMessage());
        verify(userNotificationMapper, never()).insert(any(UserNotification.class));
    }

    @Test
    void 范围发布_用户为空抛出业务异常() {
        when(notificationMapper.selectById(2005L)).thenReturn(draftNotification(2005L));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                service.publish(
                                        2005L,
                                        publishRequest(
                                                NotificationPublishScope.USERS, null, List.of())));

        assertEquals("请选择接收用户", exception.getMessage());
        verify(userNotificationMapper, never()).insert(any(UserNotification.class));
    }

    @Test
    void 未指定范围_沿用用户偏好分发() {
        when(notificationMapper.selectById(2006L)).thenReturn(draftNotification(2006L));
        UserNotificationPreference preference = new UserNotificationPreference();
        preference.setUserId(11L);
        preference.setReceiveSystem(1);
        preference.setReceiveEmail(0);
        preference.setReceiveSms(0);
        when(userNotificationPreferenceMapper.selectList(any())).thenReturn(List.of(preference));

        int delivered = service.publish(2006L, null);

        assertEquals(1, delivered);
        verify(userNotificationMapper, times(1)).insert(any(UserNotification.class));
    }

    @Test
    void 收件人角色列表_仅映射启用角色字段() {
        SysRole role = new SysRole();
        role.setId(5L);
        role.setName("财务");
        role.setCode("FINANCE");
        when(roleMapper.selectList(any())).thenReturn(List.of(role));

        List<NotificationRecipientRoleVO> roles = service.listRecipientRoles();

        assertEquals(1, roles.size());
        assertEquals(5L, roles.get(0).getId());
        assertEquals("财务", roles.get(0).getName());
        assertEquals("FINANCE", roles.get(0).getCode());
    }

    @Test
    void 收件人用户搜索_返回启用用户最小字段() {
        SysUser user = user(11L);
        user.setName("张三");
        user.setUsername("zhangsan");
        when(userMapper.selectPage(any(), any()))
                .thenAnswer(
                        invocation -> {
                            Page<SysUser> page = invocation.getArgument(0);
                            page.setRecords(List.of(user));
                            page.setTotal(1L);
                            return page;
                        });

        List<NotificationRecipientUserVO> users = service.searchRecipientUsers("张");

        assertEquals(1, users.size());
        assertEquals(11L, users.get(0).getId());
        assertEquals("张三", users.get(0).getName());
        assertEquals("zhangsan", users.get(0).getUsername());
    }

    @Test
    void 范围发布_通知不存在抛出业务异常() {
        // notificationMapper.selectById 默认返回 null，模拟通知不存在
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                service.publish(
                                        2101L,
                                        publishRequest(NotificationPublishScope.ALL, null, null)));

        assertEquals("通知不存在", exception.getMessage());
        verify(notificationMapper, never()).updateById(any(Notification.class));
    }

    @Test
    void 范围发布_非草稿状态不可发布() {
        Notification notification = draftNotification(2102L);
        notification.setStatus(NotificationStatus.PUBLISHED);
        when(notificationMapper.selectById(2102L)).thenReturn(notification);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                service.publish(
                                        2102L,
                                        publishRequest(NotificationPublishScope.ALL, null, null)));

        assertEquals("仅草稿状态可发布", exception.getMessage());
        verify(notificationMapper, never()).updateById(any(Notification.class));
        verify(userNotificationMapper, never()).insert(any(UserNotification.class));
    }

    @Test
    void 范围发布_未指定范围抛出业务异常() {
        when(notificationMapper.selectById(2103L)).thenReturn(draftNotification(2103L));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.publish(2103L, publishRequest(null, null, null)));

        assertEquals("请选择接收范围", exception.getMessage());
        verify(userNotificationMapper, never()).insert(any(UserNotification.class));
    }

    @Test
    void 范围发布_全员无启用用户时送达零人() {
        when(notificationMapper.selectById(2104L)).thenReturn(draftNotification(2104L));
        when(userMapper.selectList(any())).thenReturn(List.of());

        int delivered =
                service.publish(2104L, publishRequest(NotificationPublishScope.ALL, null, null));

        assertEquals(0, delivered);
        verify(userNotificationMapper, never()).insert(any(UserNotification.class));
    }

    @Test
    void 范围发布_多角色交叉用户去重() {
        when(notificationMapper.selectById(2105L)).thenReturn(draftNotification(2105L));
        when(userRoleMapper.selectList(any()))
                .thenReturn(
                        List.of(
                                userRole(11L, 5L),
                                userRole(11L, 6L),
                                userRole(22L, 6L)));
        when(userMapper.selectList(any())).thenReturn(List.of(user(11L), user(22L)));

        int delivered =
                service.publish(
                        2105L,
                        publishRequest(
                                NotificationPublishScope.ROLES, List.of(5L, 6L), null));

        assertEquals(2, delivered);
        verify(userNotificationMapper, times(2)).insert(any(UserNotification.class));
    }

    @Test
    void 范围发布_按用户且无有效用户时不写入() {
        when(notificationMapper.selectById(2106L)).thenReturn(draftNotification(2106L));
        when(userMapper.selectList(any())).thenReturn(List.of());

        int delivered =
                service.publish(
                        2106L,
                        publishRequest(
                                NotificationPublishScope.USERS, null, List.of(11L, 22L)));

        assertEquals(0, delivered);
        verify(userNotificationMapper, never()).insert(any(UserNotification.class));
    }

    @Test
    void 范围发布_成功后通知置为已发布并记录发布时间() {
        when(notificationMapper.selectById(2107L)).thenReturn(draftNotification(2107L));
        when(userMapper.selectList(any())).thenReturn(List.of(user(11L)));

        service.publish(2107L, publishRequest(NotificationPublishScope.ALL, null, null));

        ArgumentCaptor<Notification> notificationCaptor =
                ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).updateById(notificationCaptor.capture());
        Notification updated = notificationCaptor.getValue();
        assertEquals(NotificationStatus.PUBLISHED, updated.getStatus());
        assertNotNull(updated.getPublishTime());
    }

    @Test
    void 范围发布_站内信渠道与状态正确() {
        when(notificationMapper.selectById(2108L)).thenReturn(draftNotification(2108L));
        when(userMapper.selectList(any())).thenReturn(List.of(user(11L), user(22L)));

        service.publish(2108L, publishRequest(NotificationPublishScope.ALL, null, null));

        ArgumentCaptor<UserNotification> userNotificationCaptor =
                ArgumentCaptor.forClass(UserNotification.class);
        verify(userNotificationMapper, times(2)).insert(userNotificationCaptor.capture());
        List<UserNotification> savedNotifications = userNotificationCaptor.getAllValues();
        assertEquals(2, savedNotifications.size());
        for (UserNotification saved : savedNotifications) {
            assertEquals(2108L, saved.getNotificationId());
            assertEquals(PushChannel.SYSTEM, saved.getChannel());
            assertEquals(PushStatus.SENT, saved.getPushStatus());
            assertNotNull(saved.getPushTime());
        }
        Set<Long> savedUserIds =
                Set.copyOf(
                        savedNotifications.stream().map(UserNotification::getUserId).toList());
        assertEquals(Set.of(11L, 22L), savedUserIds);
    }

    @Test
    void 偏好分发_关闭站内信的用户不发送() {
        Notification notification = draftNotification(2109L);
        notification.setPriority(NotificationPriority.NORMAL);
        when(notificationMapper.selectById(2109L)).thenReturn(notification);
        UserNotificationPreference preference = new UserNotificationPreference();
        preference.setUserId(11L);
        preference.setReceiveSystem(0);
        preference.setReceiveEmail(0);
        preference.setReceiveSms(0);
        when(userNotificationPreferenceMapper.selectList(any())).thenReturn(List.of(preference));

        int delivered = service.publish(2109L, null);

        assertEquals(0, delivered);
        verify(userNotificationMapper, never()).insert(any(UserNotification.class));
    }

    @Test
    void 收件人用户搜索_关键字为空时限制50条() {
        when(userMapper.selectPage(any(), any()))
                .thenAnswer(
                        invocation -> {
                            Page<SysUser> page = invocation.getArgument(0);
                            assertEquals(1L, page.getCurrent());
                            assertEquals(50L, page.getSize());
                            page.setRecords(List.of());
                            page.setTotal(0L);
                            return page;
                        });

        List<NotificationRecipientUserVO> users = service.searchRecipientUsers(null);

        assertEquals(0, users.size());
    }

    private void stubInsertedNotification(Long id) {
        doAnswer(
                        invocation -> {
                            Notification notification = invocation.getArgument(0);
                            notification.setId(id);
                            return 1;
                        })
                .when(notificationMapper)
                .insert(any(Notification.class));
    }

    private static NotificationCreateRequest request() {
        NotificationCreateRequest request = new NotificationCreateRequest();
        request.setTitle("经营日报｜2026-11-06");
        request.setContent("CEAMS 经营日报");
        request.setNotificationType(NotificationType.SYSTEM);
        return request;
    }

    private static Notification draftNotification(Long id) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setTitle("系统维护通知");
        notification.setNotificationType(NotificationType.SYSTEM);
        notification.setStatus(NotificationStatus.DRAFT);
        return notification;
    }

    private static NotificationPublishRequest publishRequest(
            NotificationPublishScope scope, List<Long> roleIds, List<Long> userIds) {
        NotificationPublishRequest request = new NotificationPublishRequest();
        request.setScope(scope);
        request.setRoleIds(roleIds);
        request.setUserIds(userIds);
        return request;
    }

    private static SysUser user(Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        return user;
    }

    private static UserRole userRole(Long userId, Long roleId) {
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        return userRole;
    }
}
