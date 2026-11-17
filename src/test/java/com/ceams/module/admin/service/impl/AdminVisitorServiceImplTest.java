package com.ceams.module.admin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.admin.config.AdminProperties;
import com.ceams.module.admin.dto.VisitorCheckinRequest;
import com.ceams.module.admin.dto.VisitorCreateRequest;
import com.ceams.module.admin.entity.AdminVisitor;
import com.ceams.module.admin.enums.VisitorStatus;
import com.ceams.module.admin.mapper.AdminVisitorMapper;
import com.ceams.module.admin.support.AdminConverter;
import com.ceams.module.admin.vo.VisitorQrVO;
import com.ceams.module.admin.vo.VisitorVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** 访客服务单元测试：登记、核销一次性 / 跨日 / 过期、离场、取消、权限与手机号脱敏。 */
@ExtendWith(MockitoExtension.class)
class AdminVisitorServiceImplTest {

    private static final Long VISITOR_ID = 2101L;
    private static final Long HOST_ID = 100L;

    @Mock private AdminVisitorMapper visitorMapper;

    private AdminVisitorServiceImpl visitorService;

    @BeforeEach
    void setUp() {
        AdminProperties adminProperties = new AdminProperties();
        adminProperties.setQrCheckinWindowHours(24);
        visitorService = new AdminVisitorServiceImpl(visitorMapper, adminProperties);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createShouldGenerateTokenAndPendingStatus() {
        when(visitorMapper.insert(any(AdminVisitor.class)))
                .thenAnswer(
                        invocation -> {
                            AdminVisitor record = invocation.getArgument(0);
                            record.setId(VISITOR_ID);
                            record.setCreateTime(LocalDateTime.now());
                            return 1;
                        });

        VisitorQrVO qr = visitorService.create(request(LocalDate.now()));

        assertThat(qr.getVisitorId()).isEqualTo(VISITOR_ID);
        assertThat(qr.getToken()).hasSize(32).doesNotContain("-");
        assertThat(qr.getExpireAt()).isAfter(LocalDateTime.now());
        assertThat(qr.getVisitDate()).isEqualTo(LocalDate.now());

        ArgumentCaptor<AdminVisitor> captor = ArgumentCaptor.forClass(AdminVisitor.class);
        verify(visitorMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(VisitorStatus.PENDING);
        assertThat(captor.getValue().getQrToken()).hasSize(32);
        assertThat(captor.getValue().getPhoneEnc()).isEqualTo("13800138000");
        // 未传被访人时默认当前登录用户（单元测试无登录态为 0）
        assertThat(captor.getValue().getHostUserId()).isEqualTo(0L);
    }

    @Test
    void checkinShouldRejectCrossDayToken() {
        AdminVisitor visitor =
                visitor(
                        VisitorStatus.PENDING,
                        LocalDate.now().minusDays(1),
                        LocalDateTime.now());
        when(visitorMapper.selectOne(any(Wrapper.class))).thenReturn(visitor);

        assertThatThrownBy(() -> visitorService.checkin(checkinRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("二维码无效或已使用")
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
        verify(visitorMapper, never()).update(any(AdminVisitor.class), any(Wrapper.class));
    }

    @Test
    void checkinShouldRejectExpiredToken() {
        AdminVisitor visitor =
                visitor(
                        VisitorStatus.PENDING,
                        LocalDate.now(),
                        LocalDateTime.now().minusHours(25));
        when(visitorMapper.selectOne(any(Wrapper.class))).thenReturn(visitor);

        assertThatThrownBy(() -> visitorService.checkin(checkinRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("二维码无效或已使用")
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
        verify(visitorMapper, never()).update(any(AdminVisitor.class), any(Wrapper.class));
    }

    @Test
    void checkinShouldMarkCheckedInAndClearToken() {
        AdminVisitor visitor =
                visitor(VisitorStatus.PENDING, LocalDate.now(), LocalDateTime.now());
        when(visitorMapper.selectOne(any(Wrapper.class))).thenReturn(visitor);
        when(visitorMapper.update(any(AdminVisitor.class), any(Wrapper.class))).thenReturn(1);

        visitorService.checkin(checkinRequest());

        ArgumentCaptor<AdminVisitor> captor = ArgumentCaptor.forClass(AdminVisitor.class);
        ArgumentCaptor<Wrapper> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(visitorMapper).update(captor.capture(), wrapperCaptor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(VisitorStatus.CHECKED_IN);
        assertThat(captor.getValue().getCheckinTime()).isNotNull();
        assertThat(captor.getValue().getVersion()).isNull();
        LambdaUpdateWrapper<?> wrapper = (LambdaUpdateWrapper<?>) wrapperCaptor.getValue();
        // 核销成功即清空 token，保证一次性有效
        assertThat(wrapper.getSqlSet()).contains("qr_token");
    }

    @Test
    void checkinShouldRejectAlreadyUsedToken() {
        when(visitorMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> visitorService.checkin(checkinRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("二维码无效或已使用")
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
    }

    @Test
    void checkoutShouldRejectWhenNotCheckedIn() {
        loginAs(HOST_ID);
        when(visitorMapper.selectById(VISITOR_ID))
                .thenReturn(visitor(VisitorStatus.PENDING, LocalDate.now(), LocalDateTime.now()));

        assertThatThrownBy(() -> visitorService.checkout(VISITOR_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
        verify(visitorMapper, never()).update(any(AdminVisitor.class), any(Wrapper.class));
    }

    @Test
    void checkoutShouldMarkCheckedOut() {
        loginAs(HOST_ID);
        when(visitorMapper.selectById(VISITOR_ID))
                .thenReturn(visitor(VisitorStatus.CHECKED_IN, LocalDate.now(), LocalDateTime.now()));
        when(visitorMapper.update(any(AdminVisitor.class), any(Wrapper.class))).thenReturn(1);

        visitorService.checkout(VISITOR_ID);

        ArgumentCaptor<AdminVisitor> captor = ArgumentCaptor.forClass(AdminVisitor.class);
        verify(visitorMapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getStatus()).isEqualTo(VisitorStatus.CHECKED_OUT);
        assertThat(captor.getValue().getCheckoutTime()).isNotNull();
        assertThat(captor.getValue().getVersion()).isNull();
    }

    @Test
    void checkoutShouldRejectNonHostNonAdmin() {
        loginAs(999L);
        when(visitorMapper.selectById(VISITOR_ID))
                .thenReturn(visitor(VisitorStatus.CHECKED_IN, LocalDate.now(), LocalDateTime.now()));

        assertThatThrownBy(() -> visitorService.checkout(VISITOR_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
        verify(visitorMapper, never()).update(any(AdminVisitor.class), any(Wrapper.class));
    }

    @Test
    void cancelShouldRejectNonPending() {
        loginAs(HOST_ID);
        when(visitorMapper.selectById(VISITOR_ID))
                .thenReturn(visitor(VisitorStatus.CHECKED_IN, LocalDate.now(), LocalDateTime.now()));

        assertThatThrownBy(() -> visitorService.cancel(VISITOR_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
        verify(visitorMapper, never()).update(any(AdminVisitor.class), any(Wrapper.class));
    }

    @Test
    void cancelShouldMarkCancelled() {
        loginAs(HOST_ID);
        when(visitorMapper.selectById(VISITOR_ID))
                .thenReturn(visitor(VisitorStatus.PENDING, LocalDate.now(), LocalDateTime.now()));
        when(visitorMapper.update(any(AdminVisitor.class), any(Wrapper.class))).thenReturn(1);

        visitorService.cancel(VISITOR_ID);

        ArgumentCaptor<AdminVisitor> captor = ArgumentCaptor.forClass(AdminVisitor.class);
        verify(visitorMapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getStatus()).isEqualTo(VisitorStatus.CANCELLED);
    }

    @Test
    void converterShouldMaskPhone() {
        AdminVisitor visitor = visitor(VisitorStatus.PENDING, LocalDate.now(), LocalDateTime.now());

        VisitorVO vo = AdminConverter.toVisitorVO(visitor);

        assertThat(vo.getPhone()).isEqualTo("138****8000");
        assertThat(vo.getPhone()).doesNotContain("13800138000");
    }

    private AdminVisitor visitor(
            VisitorStatus status, LocalDate visitDate, LocalDateTime createTime) {
        AdminVisitor visitor = new AdminVisitor();
        visitor.setId(VISITOR_ID);
        visitor.setVisitorName("张三");
        visitor.setPhoneEnc("13800138000");
        visitor.setCompany("示例科技");
        visitor.setHostUserId(HOST_ID);
        visitor.setVisitDate(visitDate);
        visitor.setPurpose("商务洽谈");
        visitor.setQrToken("0123456789abcdef0123456789abcdef");
        visitor.setStatus(status);
        visitor.setCreateTime(createTime);
        return visitor;
    }

    private VisitorCreateRequest request(LocalDate visitDate) {
        VisitorCreateRequest request = new VisitorCreateRequest();
        request.setVisitorName("张三");
        request.setPhone("13800138000");
        request.setCompany("示例科技");
        request.setVisitDate(visitDate);
        request.setPurpose("商务洽谈");
        return request;
    }

    private VisitorCheckinRequest checkinRequest() {
        VisitorCheckinRequest request = new VisitorCheckinRequest();
        request.setToken("0123456789abcdef0123456789abcdef");
        return request;
    }

    private void loginAs(Long userId) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }
}
