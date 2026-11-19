package com.ceams.module.bpm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.bpm.dto.DelegateCreateRequest;
import com.ceams.module.bpm.entity.BpmDelegate;
import com.ceams.module.bpm.enums.DelegateStatus;
import com.ceams.module.bpm.mapper.BpmDelegateMapper;
import com.ceams.module.bpm.service.BpmDelegateService;
import com.ceams.module.bpm.support.BpmSecurityGuard;
import com.ceams.module.bpm.vo.DelegateVO;
import com.ceams.module.bpm.vo.UserOptionVO;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.enums.UserStatus;
import com.ceams.module.iam.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 用户自助代理服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpmDelegateServiceImpl implements BpmDelegateService {

    private final BpmDelegateMapper delegateMapper;
    private final UserMapper userMapper;
    private final BpmSecurityGuard securityGuard;

    @Override
    public PageResult<DelegateVO> pageMine(long page, long size) {
        Long current = requireLogin();
        Page<BpmDelegate> p = new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 100));
        IPage<BpmDelegate> result =
                delegateMapper.selectPage(
                        p,
                        new LambdaQueryWrapper<BpmDelegate>()
                                .eq(BpmDelegate::getUserId, current)
                                .orderByDesc(BpmDelegate::getCreateTime));
        List<BpmDelegate> records = result.getRecords();
        if (records.isEmpty()) {
            return new PageResult<>(List.of(), result.getTotal(), result.getCurrent(), result.getSize());
        }
        Map<Long, SysUser> userMap = loadUsers(records.stream().map(BpmDelegate::getDelegateId).toList());
        LocalDateTime now = LocalDateTime.now();
        List<DelegateVO> vos =
                records.stream().map(r -> toVO(r, userMap, now)).toList();
        return new PageResult<>(vos, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(DelegateCreateRequest request) {
        Long current = requireLogin();
        LocalDateTime start = request.getStartTime();
        LocalDateTime end = request.getEndTime();
        if (!start.isBefore(end)) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "代理结束时间必须晚于开始时间");
        }
        if (Objects.equals(request.getDelegateId(), current)) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "不能将自己设置为代理人");
        }
        SysUser delegate = userMapper.selectById(request.getDelegateId());
        if (delegate == null || delegate.getStatus() != UserStatus.ACTIVE) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "代理人不存在或已停用");
        }
        String bizType = StringUtils.hasText(request.getBizType()) ? request.getBizType().trim() : null;
        requireNoOverlap(current, bizType, start, end);

        BpmDelegate entity = new BpmDelegate();
        entity.setUserId(current);
        entity.setDelegateId(delegate.getId());
        entity.setBizType(bizType);
        entity.setStartTime(start);
        entity.setEndTime(end);
        entity.setStatus(DelegateStatus.ENABLED);
        entity.setReason(StringUtils.hasText(request.getReason()) ? request.getReason().trim() : null);
        delegateMapper.insert(entity);
        log.info(
                "BPM 代理创建：userId={}，delegateId={}，bizType={}，{} ~ {}",
                current,
                delegate.getId(),
                bizType,
                start,
                end);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        Long current = requireLogin();
        BpmDelegate entity = delegateMapper.selectById(id);
        if (entity == null || !Objects.equals(entity.getUserId(), current)) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "代理记录不存在");
        }
        if (entity.getStatus() == DelegateStatus.DISABLED) {
            return;
        }
        entity.setStatus(DelegateStatus.DISABLED);
        delegateMapper.updateById(entity);
        log.info("BPM 代理停用：id={}，userId={}", id, current);
    }

    @Override
    public List<UserOptionVO> candidates(String keyword, int limit) {
        Long current = requireLogin();
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getStatus, UserStatus.ACTIVE).ne(SysUser::getId, current);
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(w -> w.like(SysUser::getName, value).or().like(SysUser::getUsername, value));
        }
        wrapper.orderByAsc(SysUser::getId).last("LIMIT " + Math.min(Math.max(limit, 1), 50));
        return userMapper.selectList(wrapper).stream()
                .map(
                        user -> {
                            UserOptionVO vo = new UserOptionVO();
                            vo.setId(user.getId());
                            vo.setName(user.getName());
                            vo.setUsername(user.getUsername());
                            return vo;
                        })
                .toList();
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    /** 同一委托人在时间重叠区间内只能有一条生效代理（范围为全部或同一业务）。 */
    private void requireNoOverlap(
            Long userId, String bizType, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<BpmDelegate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BpmDelegate::getUserId, userId)
                .eq(BpmDelegate::getStatus, DelegateStatus.ENABLED)
                .lt(BpmDelegate::getStartTime, end)
                .gt(BpmDelegate::getEndTime, start);
        wrapper.and(
                w -> {
                    w.isNull(BpmDelegate::getBizType);
                    if (StringUtils.hasText(bizType)) {
                        w.or().eq(BpmDelegate::getBizType, bizType);
                    }
                });
        if (delegateMapper.selectCount(wrapper) > 0) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "该时间段已存在生效代理，请先停用后再设置");
        }
    }

    private DelegateVO toVO(BpmDelegate entity, Map<Long, SysUser> userMap, LocalDateTime now) {
        DelegateVO vo = new DelegateVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setDelegateId(entity.getDelegateId());
        SysUser delegate = userMap.get(entity.getDelegateId());
        vo.setDelegateName(delegate == null ? null : delegate.getName());
        vo.setBizType(entity.getBizType());
        vo.setStartTime(entity.getStartTime());
        vo.setEndTime(entity.getEndTime());
        vo.setStatus(entity.getStatus());
        vo.setReason(entity.getReason());
        vo.setCreateTime(entity.getCreateTime());
        vo.setEffective(
                entity.getStatus() == DelegateStatus.ENABLED
                        && entity.getStartTime() != null
                        && entity.getEndTime() != null
                        && !now.isBefore(entity.getStartTime())
                        && !now.isAfter(entity.getEndTime()));
        return vo;
    }

    private Map<Long, SysUser> loadUsers(Collection<Long> userIds) {
        List<Long> ids = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(ids).stream()
                .filter(user -> user.getId() != null)
                .collect(Collectors.toMap(SysUser::getId, user -> user, (a, b) -> a));
    }

    private Long requireLogin() {
        Long current = securityGuard.currentUserId();
        if (current == null) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "未登录不可操作代理");
        }
        return current;
    }
}
