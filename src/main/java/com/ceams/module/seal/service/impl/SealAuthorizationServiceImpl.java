package com.ceams.module.seal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.enums.UserStatus;
import com.ceams.module.iam.mapper.UserMapper;
import com.ceams.module.seal.dto.SealAuthorizationCreateRequest;
import com.ceams.module.seal.entity.ElectronicSeal;
import com.ceams.module.seal.entity.SealAuthorization;
import com.ceams.module.seal.enums.SealStatus;
import com.ceams.module.seal.mapper.ElectronicSealMapper;
import com.ceams.module.seal.mapper.SealAuthorizationMapper;
import com.ceams.module.seal.service.SealAuthorizationService;
import com.ceams.module.seal.vo.SealAuthorizationVO;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 印章授权服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SealAuthorizationServiceImpl implements SealAuthorizationService {

    private final SealAuthorizationMapper sealAuthorizationMapper;
    private final ElectronicSealMapper electronicSealMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(SealAuthorizationCreateRequest request) {
        ElectronicSeal seal = requireActiveSeal(request.getSealId());
        SysUser user = requireActiveUser(request.getUserId());

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "授权结束时间必须晚于开始时间");
        }

        // 同一印章同一用户不允许存在时间重叠的启用中授权，避免有效期口径冲突
        Long overlapCount =
                sealAuthorizationMapper.selectCount(
                        new LambdaQueryWrapper<SealAuthorization>()
                                .eq(SealAuthorization::getSealId, seal.getId())
                                .eq(SealAuthorization::getUserId, user.getId())
                                .eq(SealAuthorization::getEnabled, Boolean.TRUE)
                                .lt(SealAuthorization::getStartTime, request.getEndTime())
                                .gt(SealAuthorization::getEndTime, request.getStartTime()));
        if (overlapCount != null && overlapCount > 0) {
            throw BusinessException.of(
                    ErrorCode.BIZ_STATE_CONFLICT, "该用户在此时间段内已存在该印章的有效授权");
        }

        SealAuthorization authorization = new SealAuthorization();
        authorization.setSealId(seal.getId());
        authorization.setUserId(user.getId());
        authorization.setStartTime(request.getStartTime());
        authorization.setEndTime(request.getEndTime());
        authorization.setEnabled(Boolean.TRUE);
        authorization.setRemark(request.getRemark());
        sealAuthorizationMapper.insert(authorization);
        return String.valueOf(authorization.getId());
    }

    @Override
    public List<SealAuthorizationVO> listBySeal(Long sealId) {
        if (sealId == null) {
            return Collections.emptyList();
        }
        List<SealAuthorization> authorizations =
                sealAuthorizationMapper.selectList(
                        new LambdaQueryWrapper<SealAuthorization>()
                                .eq(SealAuthorization::getSealId, sealId)
                                .orderByDesc(SealAuthorization::getCreateTime));
        if (authorizations.isEmpty()) {
            return Collections.emptyList();
        }

        ElectronicSeal seal = electronicSealMapper.selectById(sealId);
        String sealName = seal == null ? null : seal.getSealName();

        List<Long> userIds =
                authorizations.stream()
                        .map(SealAuthorization::getUserId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        Map<Long, SysUser> userMap =
                userIds.isEmpty()
                        ? Collections.emptyMap()
                        : userMapper.selectBatchIds(userIds).stream()
                                .collect(Collectors.toMap(SysUser::getId, Function.identity()));

        return authorizations.stream()
                .map(
                        authorization ->
                                toVO(
                                        authorization,
                                        sealName,
                                        userMap.get(authorization.getUserId())))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long id) {
        SealAuthorization authorization =
                id == null ? null : sealAuthorizationMapper.selectById(id);
        if (authorization == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "印章授权记录不存在");
        }
        // 与文件授权撤销保持一致：逻辑删除，保留历史记录供审计
        sealAuthorizationMapper.deleteById(id);
        log.info("撤销印章授权：id={}，sealId={}，userId={}", id, authorization.getSealId(), authorization.getUserId());
    }

    @Override
    public boolean hasActiveAuthorization(Long sealId, Long userId) {
        if (sealId == null || userId == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        Long count =
                sealAuthorizationMapper.selectCount(
                        new LambdaQueryWrapper<SealAuthorization>()
                                .eq(SealAuthorization::getSealId, sealId)
                                .eq(SealAuthorization::getUserId, userId)
                                .eq(SealAuthorization::getEnabled, Boolean.TRUE)
                                .le(SealAuthorization::getStartTime, now)
                                .ge(SealAuthorization::getEndTime, now));
        return count != null && count > 0;
    }

    private ElectronicSeal requireActiveSeal(Long sealId) {
        ElectronicSeal seal = sealId == null ? null : electronicSealMapper.selectById(sealId);
        if (seal == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "电子印章不存在");
        }
        if (seal.getStatus() != SealStatus.ACTIVE) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "印章已停用或归档，不可授权使用");
        }
        return seal;
    }

    private SysUser requireActiveUser(Long userId) {
        SysUser user = userId == null ? null : userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "被授权用户不存在");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "被授权用户已停用，不可授权");
        }
        return user;
    }

    private SealAuthorizationVO toVO(
            SealAuthorization authorization, String sealName, SysUser user) {
        SealAuthorizationVO vo = new SealAuthorizationVO();
        vo.setId(authorization.getId());
        vo.setSealId(authorization.getSealId());
        vo.setSealName(sealName);
        vo.setUserId(authorization.getUserId());
        // 用户姓名优先，缺失时回退登录账号，避免前端展示空白
        vo.setUserName(user == null ? null : displayName(user));
        vo.setStartTime(authorization.getStartTime());
        vo.setEndTime(authorization.getEndTime());
        vo.setEnabled(authorization.getEnabled());
        vo.setRemark(authorization.getRemark());
        vo.setCreatedAt(authorization.getCreateTime());
        return vo;
    }

    private String displayName(SysUser user) {
        return StringUtils.isNotBlank(user.getName()) ? user.getName() : user.getUsername();
    }
}
