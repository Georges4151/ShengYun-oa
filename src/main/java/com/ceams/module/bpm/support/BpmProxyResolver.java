package com.ceams.module.bpm.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.module.bpm.entity.BpmDelegate;
import com.ceams.module.bpm.enums.DelegateStatus;
import com.ceams.module.bpm.mapper.BpmDelegateMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 代理解析器：任务生成时判断审批人是否处于代理时段。
 *
 * <p>命中规则：状态 ENABLED、当前时间落在 [start_time, end_time]、适用范围为全部
 * 或与实例业务类型一致；同一委托人存在多条命中时取最新创建的一条。
 * 代理人为本人或已被停用时视为未命中，避免产生自审待办。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BpmProxyResolver {

    private final BpmDelegateMapper delegateMapper;

    /**
     * 解析生效代理人。
     *
     * @param userId 原审批人
     * @param bizType 业务类型（可为空）
     * @param at 判定时间（为空取当前时间）
     * @return 代理人 ID；无生效代理返回 null
     */
    public Long resolve(Long userId, String bizType, LocalDateTime at) {
        if (userId == null) {
            return null;
        }
        LocalDateTime now = at == null ? LocalDateTime.now() : at;
        LambdaQueryWrapper<BpmDelegate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BpmDelegate::getUserId, userId)
                .eq(BpmDelegate::getStatus, DelegateStatus.ENABLED)
                .le(BpmDelegate::getStartTime, now)
                .ge(BpmDelegate::getEndTime, now);
        wrapper.and(
                w -> {
                    w.isNull(BpmDelegate::getBizType);
                    if (StringUtils.hasText(bizType)) {
                        w.or().eq(BpmDelegate::getBizType, bizType);
                    }
                });
        wrapper.orderByDesc(BpmDelegate::getCreateTime);
        List<BpmDelegate> list = delegateMapper.selectList(wrapper);
        for (BpmDelegate delegate : list) {
            if (delegate.getDelegateId() != null && !delegate.getDelegateId().equals(userId)) {
                return delegate.getDelegateId();
            }
        }
        return null;
    }
}
