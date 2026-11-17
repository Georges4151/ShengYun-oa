package com.ceams.module.integration.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.mapper.UserMapper;
import com.ceams.module.integration.config.WeComAppProperties;
import com.ceams.module.integration.entity.IntegrationWecomMessage;
import com.ceams.module.integration.mapper.IntegrationWecomMessageMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

/** 企业微信应用消息发件箱单元测试：仅对有 wecom_userid 的用户入队、唯一键冲突静默跳过、关闭/非法参数安全返回。 */
@ExtendWith(MockitoExtension.class)
class WeComMessageOutboxTest {

    @Mock private IntegrationWecomMessageMapper messageMapper;

    @Mock private UserMapper userMapper;

    private WeComMessageOutbox outbox;

    @BeforeEach
    void setUp() {
        WeComAppProperties properties = new WeComAppProperties();
        properties.setEnabled(true);
        properties.setCorpId("corp-id");
        properties.setSecret("corp-secret");
        outbox =
                new WeComMessageOutbox(
                        messageMapper, userMapper, properties, new ObjectMapper());
    }

    @Test
    void shouldEnqueueOnlyBoundUsersAndSkipDuplicateKey() {
        when(userMapper.selectList(any()))
                .thenReturn(List.of(user(11L, "wx11"), user(12L, null), user(13L, "wx13")));
        when(messageMapper.insert(any(IntegrationWecomMessage.class)))
                .thenAnswer(
                        invocation -> {
                            IntegrationWecomMessage message = invocation.getArgument(0);
                            if (message.getTargetUserId() == 13L) {
                                throw new DuplicateKeyException("uk_wecom_msg_notification_user");
                            }
                            return 1;
                        });

        outbox.enqueue("1001", List.of(11L, 12L, 13L), "审批待办提醒", "您有一条新的审批待办", "ceams://todo?tab=pending");

        ArgumentCaptor<IntegrationWecomMessage> captor =
                ArgumentCaptor.forClass(IntegrationWecomMessage.class);
        verify(messageMapper, times(2)).insert(captor.capture());
        List<IntegrationWecomMessage> inserted = captor.getAllValues();
        assertThat(inserted)
                .extracting(IntegrationWecomMessage::getTargetUserId)
                .containsExactly(11L, 13L);
        IntegrationWecomMessage first = inserted.get(0);
        assertThat(first.getNotificationId()).isEqualTo(1001L);
        assertThat(first.getWecomUserid()).isEqualTo("wx11");
        assertThat(first.getMsgType()).isEqualTo("TEXT");
        assertThat(first.getStatus()).isEqualTo("PENDING");
        assertThat(first.getRetryCount()).isZero();
        assertThat(first.getNextRetryAt()).isNotNull();
        assertThat(first.getPayloadJson())
                .contains("审批待办提醒")
                .contains("您有一条新的审批待办")
                .contains("ceams://todo?tab=pending");
    }

    @Test
    void shouldReturnSafelyWhenDisabledOrNotificationIdInvalid() {
        WeComAppProperties disabled = new WeComAppProperties();
        WeComMessageOutbox disabledOutbox =
                new WeComMessageOutbox(messageMapper, userMapper, disabled, new ObjectMapper());

        assertThatCode(
                        () -> {
                            disabledOutbox.enqueue("1001", List.of(11L), "标题", "正文", null);
                            outbox.enqueue("not-a-number", List.of(11L), "标题", "正文", null);
                            outbox.enqueue("1001", List.of(), "标题", "正文", null);
                        })
                .doesNotThrowAnyException();
        verifyNoInteractions(userMapper, messageMapper);
    }

    private SysUser user(Long id, String wecomUserid) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setWecomUserid(wecomUserid);
        return user;
    }
}
