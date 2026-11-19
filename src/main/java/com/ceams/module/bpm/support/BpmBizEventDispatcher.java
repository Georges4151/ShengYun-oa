package com.ceams.module.bpm.support;

import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 业务回调分发器：按 {@code biz_type} 把流程事件路由到对应业务回调。
 *
 * <p>未注册回调的业务类型只记录调试日志，不影响流程推进（避免新增业务类型时必须同步改内核）。
 */
@Slf4j
@Component
public class BpmBizEventDispatcher {

    private final Map<BpmBizType, List<BpmBizCallback>> callbacks = new EnumMap<>(BpmBizType.class);

    public BpmBizEventDispatcher(List<BpmBizCallback> callbackList) {
        if (callbackList == null) {
            return;
        }
        for (BpmBizCallback callback : callbackList) {
            if (callback == null || callback.bizType() == null) {
                continue;
            }
            callbacks.computeIfAbsent(callback.bizType(), key -> new ArrayList<>()).add(callback);
        }
    }

    /** 节点完成事件。 */
    public void onNodeCompleted(BpmInstance instance, String nodeKey) {
        for (BpmBizCallback callback : resolve(instance)) {
            callback.onNodeCompleted(instance, nodeKey);
        }
    }

    /** 实例终态事件。 */
    public void onInstanceFinished(BpmInstance instance, BpmInstanceStatus status) {
        for (BpmBizCallback callback : resolve(instance)) {
            callback.onInstanceFinished(instance, status);
        }
    }

    private List<BpmBizCallback> resolve(BpmInstance instance) {
        if (instance == null) {
            return List.of();
        }
        BpmBizType bizType = BpmBizType.from(instance.getBizType());
        if (bizType == null) {
            log.debug("BPM 业务回调跳过：未知业务类型 bizType={}", instance.getBizType());
            return List.of();
        }
        return callbacks.getOrDefault(bizType, List.of());
    }
}
