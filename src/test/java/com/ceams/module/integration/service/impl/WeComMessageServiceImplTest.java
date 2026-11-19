package com.ceams.module.integration.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.bpm.enums.AuditAction;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.bpm.support.BpmSecurityGuard;
import com.ceams.module.integration.entity.IntegrationWecomMessage;
import com.ceams.module.integration.mapper.IntegrationWecomMessageMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 企业微信应用消息服务单元测试：仅 FAILED 可重投、重投复位 PENDING 并写审计。 */
@ExtendWith(MockitoExtension.class)
class WeComMessageServiceImplTest {

    /** 解析 UpdateWrapper.set 生成的 SET 片段：列名 -> 参数占位符。 */
    private static final Pattern SET_PATTERN =
            Pattern.compile("([a-z_]+)=#\\{ew\\.paramNameValuePairs\\.(\\w+)}");

    @Mock private IntegrationWecomMessageMapper messageMapper;

    @Mock private BpmSecurityGuard securityGuard;

    @Mock private BpmAuditService auditService;

    private WeComMessageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new WeComMessageServiceImpl(messageMapper, securityGuard, auditService);
    }

    @Test
    void shouldRejectRetryWhenMessageIsNotFailed() {
        IntegrationWecomMessage message = message(700L, "SENT");
        when(messageMapper.selectById(700L)).thenReturn(message);

        assertThatThrownBy(() -> service.retry(700L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅失败消息可重投")
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALID_ILLEGAL_ARGUMENT);
        verify(messageMapper, never()).update(any(), any());
        verify(auditService, never())
                .record(anyString(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldResetFailedMessageToPendingAndWriteAudit() {
        IntegrationWecomMessage message = message(701L, "FAILED");
        when(messageMapper.selectById(701L)).thenReturn(message);
        when(securityGuard.currentUserId()).thenReturn(7L);

        service.retry(701L);

        Map<String, Object> sets = captureUpdateSets();
        assertThat(sets.get("status")).isEqualTo("PENDING");
        assertThat(sets.get("retry_count")).isEqualTo(0);
        assertThat(sets.get("next_retry_at")).isInstanceOf(LocalDateTime.class);
        assertThat(sets).containsKey("error_msg");
        assertThat(sets.get("error_msg")).isNull();

        verify(auditService)
                .record(
                        eq("INTEGRATION_WECOM_MESSAGE"),
                        eq(701L),
                        eq(AuditAction.WECOM_MESSAGE_RETRY),
                        eq("FAILED"),
                        eq("PENDING"),
                        isNull(),
                        eq(7L));
    }

    // ------------------------------------------------------------------
    // 测试工具
    // ------------------------------------------------------------------

    private IntegrationWecomMessage message(Long id, String status) {
        IntegrationWecomMessage message = new IntegrationWecomMessage();
        message.setId(id);
        message.setNotificationId(1001L);
        message.setTargetUserId(11L);
        message.setWecomUserid("wx11");
        message.setMsgType("TEXT");
        message.setStatus(status);
        message.setRetryCount(5);
        message.setErrorMsg("errcode=93000");
        return message;
    }

    private Map<String, Object> captureUpdateSets() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<IntegrationWecomMessage>> captor =
                ArgumentCaptor.forClass(Wrapper.class);
        verify(messageMapper).update(isNull(), captor.capture());
        @SuppressWarnings("unchecked")
        UpdateWrapper<IntegrationWecomMessage> wrapper =
                (UpdateWrapper<IntegrationWecomMessage>) captor.getValue();
        Map<String, Object> sets = new LinkedHashMap<>();
        Matcher matcher = SET_PATTERN.matcher(wrapper.getSqlSet());
        while (matcher.find()) {
            sets.put(matcher.group(1), wrapper.getParamNameValuePairs().get(matcher.group(2)));
        }
        return sets;
    }
}
