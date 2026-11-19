package com.ceams.module.notification.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ceams.module.notification.converter.NotificationConverter;
import com.ceams.module.notification.dto.NotificationCreateRequest;
import com.ceams.module.notification.entity.Notification;
import com.ceams.module.notification.entity.UserNotification;
import com.ceams.module.notification.enums.NotificationStatus;
import com.ceams.module.notification.enums.NotificationType;
import com.ceams.module.notification.mapper.NotificationMapper;
import com.ceams.module.notification.mapper.UserNotificationMapper;
import com.ceams.module.notification.mapper.UserNotificationPreferenceMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** {@link NotificationServiceImpl} 定向发布单元测试。 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private NotificationMapper notificationMapper;
    @Mock private NotificationConverter notificationConverter;
    @Mock private UserNotificationPreferenceMapper userNotificationPreferenceMapper;
    @Mock private UserNotificationMapper userNotificationMapper;

    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new NotificationServiceImpl(
                        notificationMapper,
                        notificationConverter,
                        userNotificationPreferenceMapper,
                        userNotificationMapper);
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
}
