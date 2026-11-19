package com.ceams.module.notification.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ceams.module.notification.enums.NotificationType;
import com.ceams.module.notification.mapper.UserNotificationMapper;
import com.ceams.module.notification.vo.UserNotificationMineVO;
import com.ceams.module.notification.vo.UserNotificationVO;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** 「我的通知」按类型过滤单元测试：类型筛选下推 Mapper，未传类型保持旧行为。 */
@ExtendWith(MockitoExtension.class)
class UserNotificationTypeFilterTest {

    private static final Long USER_ID = 21L;

    @Mock private UserNotificationMapper userNotificationMapper;

    private UserNotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserNotificationServiceImpl(userNotificationMapper);
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(USER_ID, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void mineWithTypeShouldPassEnumToMapper() {
        when(userNotificationMapper.selectMinePage(any(), eq(USER_ID), eq(NotificationType.HR)))
                .thenAnswer(
                        invocation -> {
                            IPage<UserNotificationVO> page = invocation.getArgument(0);
                            page.setRecords(List.of(record()));
                            page.setTotal(1L);
                            return page;
                        });
        when(userNotificationMapper.selectCount(any())).thenReturn(2L);

        UserNotificationMineVO result = service.mine(1, 20, NotificationType.HR);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getUnreadCount()).isEqualTo(2L);
        verify(userNotificationMapper)
                .selectMinePage(any(IPage.class), eq(USER_ID), eq(NotificationType.HR));
    }

    @Test
    void mineWithoutTypeShouldKeepLegacyBehavior() {
        when(userNotificationMapper.selectMinePage(any(), eq(USER_ID), isNull()))
                .thenAnswer(
                        invocation -> {
                            IPage<UserNotificationVO> page = invocation.getArgument(0);
                            page.setRecords(List.of());
                            page.setTotal(0L);
                            return page;
                        });
        when(userNotificationMapper.selectCount(any())).thenReturn(0L);

        UserNotificationMineVO result = service.mine(1, 20);

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getTotal()).isZero();
        verify(userNotificationMapper).selectMinePage(any(IPage.class), eq(USER_ID), isNull());
    }

    private UserNotificationVO record() {
        UserNotificationVO vo = new UserNotificationVO();
        vo.setId(1L);
        vo.setNotificationId(100L);
        vo.setUserId(USER_ID);
        vo.setIsRead(0);
        vo.setNotificationType(NotificationType.HR);
        vo.setTitle("工资条已发布（2026-11）");
        return vo;
    }
}
