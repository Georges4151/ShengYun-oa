package com.ceams.module.integration.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.bpm.enums.AuditAction;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.bpm.support.BpmSecurityGuard;
import com.ceams.module.integration.client.WeComApiClient;
import com.ceams.module.integration.client.WeComDept;
import com.ceams.module.integration.client.WeComUser;
import com.ceams.module.integration.config.WeComAppProperties;
import com.ceams.module.integration.entity.IntegrationWecomSync;
import com.ceams.module.integration.enums.WeComSyncStatus;
import com.ceams.module.integration.enums.WeComSyncType;
import com.ceams.module.integration.mapper.IntegrationWecomSyncMapper;
import com.ceams.module.integration.support.WeComSyncRunner;
import com.ceams.module.integration.support.WeComTokenCache;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/** 企业微信组织同步服务单元测试：锁冲突 409、正常批次 SUCCESS 与审计写入。 */
@ExtendWith(MockitoExtension.class)
class WeComSyncServiceImplTest {

    private static final String SYNC_LOCK_KEY = "wecom:sync:lock";

    @Mock private IntegrationWecomSyncMapper syncMapper;

    @Mock private WeComApiClient weComApiClient;

    @Mock private WeComTokenCache tokenCache;

    @Mock private WeComSyncRunner syncRunner;

    @Mock private BpmSecurityGuard securityGuard;

    @Mock private BpmAuditService auditService;

    @Mock private StringRedisTemplate stringRedisTemplate;

    @Mock private ValueOperations<String, String> valueOperations;

    private WeComSyncServiceImpl service;

    @BeforeEach
    void setUp() {
        WeComAppProperties properties = new WeComAppProperties();
        properties.setEnabled(true);
        properties.setCorpId("corp-id");
        properties.setSecret("corp-secret");
        service =
                new WeComSyncServiceImpl(
                        syncMapper,
                        weComApiClient,
                        tokenCache,
                        syncRunner,
                        properties,
                        securityGuard,
                        auditService,
                        stringRedisTemplate);
    }

    @Test
    void shouldRejectWhenSyncLockHeld() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(SYNC_LOCK_KEY), anyString(), any(Duration.class)))
                .thenReturn(false);

        assertThatThrownBy(() -> service.sync(WeComSyncType.MANUAL))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("组织同步正在进行中")
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
        verify(tokenCache, never()).getToken();
        verify(syncMapper, never()).insert(any(IntegrationWecomSync.class));
    }

    @Test
    void shouldPersistSuccessBatchAndAudit() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(SYNC_LOCK_KEY), anyString(), any(Duration.class)))
                .thenReturn(true);
        when(tokenCache.getToken()).thenReturn("tk");
        when(weComApiClient.listDept("tk"))
                .thenReturn(List.of(new WeComDept(10L, "研发部", 1L, 1)));
        when(weComApiClient.listDeptUsers("tk", 10L))
                .thenReturn(
                        List.of(
                                new WeComUser(
                                        "wx1", "张三", "13800000000", "z@x.com", 1, List.of(10L))));
        when(syncRunner.run(anyList(), anyMap()))
                .thenReturn(new WeComSyncRunner.SyncResult(2, 2, 0));
        AtomicReference<WeComSyncStatus> insertedStatus = new AtomicReference<>();
        when(syncMapper.insert(any(IntegrationWecomSync.class)))
                .thenAnswer(
                        invocation -> {
                            IntegrationWecomSync batch = invocation.getArgument(0);
                            insertedStatus.set(batch.getStatus());
                            batch.setId(999L);
                            return 1;
                        });
        when(syncMapper.updateById(any(IntegrationWecomSync.class))).thenReturn(1);
        when(securityGuard.currentUserId()).thenReturn(null);

        String batchNo = service.sync(WeComSyncType.MANUAL);

        assertThat(batchNo).startsWith("WECOM-").hasSizeLessThanOrEqualTo(32);
        assertThat(insertedStatus.get()).isEqualTo(WeComSyncStatus.RUNNING);

        ArgumentCaptor<IntegrationWecomSync> captor =
                ArgumentCaptor.forClass(IntegrationWecomSync.class);
        verify(syncMapper).updateById(captor.capture());
        IntegrationWecomSync updated = captor.getValue();
        assertThat(updated.getSyncBatchNo()).isEqualTo(batchNo);
        assertThat(updated.getSyncType()).isEqualTo(WeComSyncType.MANUAL);
        assertThat(updated.getStatus()).isEqualTo(WeComSyncStatus.SUCCESS);
        assertThat(updated.getTotal()).isEqualTo(2);
        assertThat(updated.getSuccessCount()).isEqualTo(2);
        assertThat(updated.getFailedCount()).isZero();
        assertThat(updated.getFinishedAt()).isNotNull();

        verify(auditService)
                .record(
                        eq("INTEGRATION_WECOM_SYNC"),
                        eq(999L),
                        eq(AuditAction.WECOM_SYNC),
                        isNull(),
                        any(),
                        isNull(),
                        isNull());
    }
}
