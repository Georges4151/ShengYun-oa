package com.ceams.module.ai.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ceams.module.ai.client.OllamaClient;
import com.ceams.module.ai.config.AiProperties;
import com.ceams.module.ai.vo.AiStatusVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** {@link AiStatusServiceImpl} 状态查询与 60 秒探活缓存单元测试。 */
@ExtendWith(MockitoExtension.class)
class AiStatusServiceImplTest {

    @Mock private OllamaClient ollamaClient;

    @Test
    void 未启用_返回不可用且不调用客户端() {
        AiStatusServiceImpl service = new AiStatusServiceImpl(properties(false), ollamaClient);

        AiStatusVO vo = service.getStatus();

        assertFalse(vo.isEnabled());
        assertEquals("qwen2.5:7b", vo.getModel());
        assertFalse(vo.isReachable());
        assertEquals(0L, vo.getLatencyMs());
        verifyNoInteractions(ollamaClient);
    }

    @Test
    void 已启用_客户端可达_返回可达且记录耗时() {
        when(ollamaClient.isReachable()).thenReturn(true);
        AiStatusServiceImpl service = new AiStatusServiceImpl(properties(true), ollamaClient);

        AiStatusVO vo = service.getStatus();

        assertTrue(vo.isEnabled());
        assertTrue(vo.isReachable());
        assertTrue(vo.getLatencyMs() >= 0L);
    }

    @Test
    void 已启用_客户端不可达_返回不可达且不抛异常() {
        when(ollamaClient.isReachable()).thenReturn(false);
        AiStatusServiceImpl service = new AiStatusServiceImpl(properties(true), ollamaClient);

        AiStatusVO vo = service.getStatus();

        assertTrue(vo.isEnabled());
        assertFalse(vo.isReachable());
    }

    @Test
    void 缓存命中_60秒内只探活一次() {
        when(ollamaClient.isReachable()).thenReturn(true);
        AiStatusServiceImpl service = new AiStatusServiceImpl(properties(true), ollamaClient);

        service.getStatus();
        service.getStatus();

        verify(ollamaClient, times(1)).isReachable();
    }

    @Test
    void 缓存命中_失败结果同样缓存60秒() {
        when(ollamaClient.isReachable()).thenReturn(false);
        AiStatusServiceImpl service = new AiStatusServiceImpl(properties(true), ollamaClient);

        assertFalse(service.getStatus().isReachable());
        assertFalse(service.getStatus().isReachable());

        verify(ollamaClient, times(1)).isReachable();
    }

    private AiProperties properties(boolean enabled) {
        AiProperties properties = new AiProperties();
        properties.setEnabled(enabled);
        properties.setModel("qwen2.5:7b");
        return properties;
    }
}
