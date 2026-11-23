package com.ceams.module.ai.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.module.ai.client.AiChatClient;
import com.ceams.module.ai.vo.AiStatusVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** {@link AiStatusServiceImpl} 状态查询与 60 秒探活缓存单元测试。 */
@ExtendWith(MockitoExtension.class)
class AiStatusServiceImplTest {

    @Mock private AiChatClient aiChatClient;

    @Test
    void 未启用_返回不可用且不发起网络请求() {
        when(aiChatClient.isEnabled()).thenReturn(false);
        when(aiChatClient.textModel()).thenReturn("qwen2.5:7b");
        AiStatusServiceImpl service = new AiStatusServiceImpl(aiChatClient);

        AiStatusVO vo = service.getStatus();

        assertFalse(vo.isEnabled());
        assertEquals("qwen2.5:7b", vo.getModel());
        assertFalse(vo.isReachable());
        assertEquals(0L, vo.getLatencyMs());
        verify(aiChatClient, times(0)).isReachable();
    }

    @Test
    void 已启用_客户端可达_返回可达且记录耗时() {
        when(aiChatClient.isEnabled()).thenReturn(true);
        when(aiChatClient.textModel()).thenReturn("qwen2.5:7b");
        when(aiChatClient.isReachable()).thenReturn(true);
        AiStatusServiceImpl service = new AiStatusServiceImpl(aiChatClient);

        AiStatusVO vo = service.getStatus();

        assertTrue(vo.isEnabled());
        assertTrue(vo.isReachable());
        assertTrue(vo.getLatencyMs() >= 0L);
    }

    @Test
    void 已启用_客户端不可达_返回不可达且不抛异常() {
        when(aiChatClient.isEnabled()).thenReturn(true);
        when(aiChatClient.isReachable()).thenReturn(false);
        AiStatusServiceImpl service = new AiStatusServiceImpl(aiChatClient);

        AiStatusVO vo = service.getStatus();

        assertTrue(vo.isEnabled());
        assertFalse(vo.isReachable());
    }

    @Test
    void 缓存命中_60秒内只探活一次() {
        when(aiChatClient.isEnabled()).thenReturn(true);
        when(aiChatClient.isReachable()).thenReturn(true);
        AiStatusServiceImpl service = new AiStatusServiceImpl(aiChatClient);

        service.getStatus();
        service.getStatus();

        verify(aiChatClient, times(1)).isReachable();
    }

    @Test
    void 缓存命中_失败结果同样缓存60秒() {
        when(aiChatClient.isEnabled()).thenReturn(true);
        when(aiChatClient.isReachable()).thenReturn(false);
        AiStatusServiceImpl service = new AiStatusServiceImpl(aiChatClient);

        assertFalse(service.getStatus().isReachable());
        assertFalse(service.getStatus().isReachable());

        verify(aiChatClient, times(1)).isReachable();
    }

    @Test
    void 配置修订号变化_缓存立即失效并重新探活() {
        when(aiChatClient.isEnabled()).thenReturn(true);
        when(aiChatClient.isReachable()).thenReturn(true);
        when(aiChatClient.configRevision()).thenReturn(1L, 2L);
        AiStatusServiceImpl service = new AiStatusServiceImpl(aiChatClient);

        service.getStatus();
        service.getStatus();

        verify(aiChatClient, times(2)).isReachable();
    }
}
