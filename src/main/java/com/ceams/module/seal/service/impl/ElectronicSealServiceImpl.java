package com.ceams.module.seal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.seal.converter.ElectronicSealConverter;
import com.ceams.module.seal.dto.ElectronicSealCreateRequest;
import com.ceams.module.seal.dto.ElectronicSealUpdateRequest;
import com.ceams.module.seal.entity.ElectronicSeal;
import com.ceams.module.seal.entity.ElectronicSealApplication;
import com.ceams.module.seal.enums.SealStatus;
import com.ceams.module.seal.enums.StorageType;
import com.ceams.module.seal.mapper.ElectronicSealApplicationMapper;
import com.ceams.module.seal.mapper.ElectronicSealMapper;
import com.ceams.module.seal.service.ElectronicSealService;
import com.ceams.module.seal.util.PageUtil;
import com.ceams.module.seal.vo.ElectronicSealVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 电子印章服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElectronicSealServiceImpl implements ElectronicSealService {

    private final ElectronicSealMapper electronicSealMapper;
    private final ElectronicSealApplicationMapper electronicSealApplicationMapper;
    private final ElectronicSealConverter electronicSealConverter;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<ElectronicSealVO> page(
            long page, long size, String name, SealStatus status, StorageType storageType) {
        Page<ElectronicSeal> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<ElectronicSeal> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(name)) {
            wrapper.like(ElectronicSeal::getSealName, name);
        }
        wrapper.eq(status != null, ElectronicSeal::getStatus, status);
        wrapper.eq(storageType != null, ElectronicSeal::getStorageType, storageType);
        wrapper.orderByDesc(ElectronicSeal::getCreateTime);
        IPage<ElectronicSeal> result = electronicSealMapper.selectPage(p, wrapper);
        return new PageResult<>(
                electronicSealConverter.toVOList(result.getRecords()),
                result.getTotal(),
                result.getCurrent(),
                result.getSize());
    }

    @Override
    public ElectronicSealVO detail(Long id) {
        return electronicSealConverter.toVO(requireSeal(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(ElectronicSealCreateRequest request) {
        ensureCodeUnique(request.getSealCode(), null);
        ElectronicSeal seal = new ElectronicSeal();
        seal.setSealName(request.getSealName());
        seal.setSealType(request.getSealType());
        seal.setSealCode(request.getSealCode());
        seal.setDescription(request.getDescription());
        seal.setStatus(SealStatus.ACTIVE);
        seal.setSealImage(request.getSealImage());
        seal.setStorageType(request.getStorageType());
        seal.setSealFileId(request.getSealFileId());
        seal.setUsbDriveId(request.getUsbDriveId());
        boolean passwordProtected = Boolean.TRUE.equals(request.getPasswordProtected());
        seal.setPasswordProtected(passwordProtected);
        // 仅存哈希；未启用密码保护时清空，避免历史残留
        seal.setPasswordHash(passwordProtected ? encodePassword(request.getPassword()) : null);
        electronicSealMapper.insert(seal);
        return String.valueOf(seal.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ElectronicSealUpdateRequest request) {
        ElectronicSeal seal = requireSeal(id);
        if (request.getSealName() != null) {
            seal.setSealName(request.getSealName());
        }
        if (request.getSealType() != null) {
            seal.setSealType(request.getSealType());
        }
        if (request.getSealCode() != null) {
            ensureCodeUnique(request.getSealCode(), id);
            seal.setSealCode(request.getSealCode());
        }
        if (request.getDescription() != null) {
            seal.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            seal.setStatus(request.getStatus());
        }
        if (request.getSealImage() != null) {
            seal.setSealImage(request.getSealImage());
        }
        if (request.getStorageType() != null) {
            seal.setStorageType(request.getStorageType());
        }
        if (request.getSealFileId() != null) {
            seal.setSealFileId(request.getSealFileId());
        }
        if (request.getUsbDriveId() != null) {
            seal.setUsbDriveId(request.getUsbDriveId());
        }
        if (request.getPasswordProtected() != null) {
            seal.setPasswordProtected(request.getPasswordProtected());
        }
        boolean passwordProtected = Boolean.TRUE.equals(seal.getPasswordProtected());
        if (!passwordProtected) {
            // 关闭密码保护时清空哈希，避免失效密码残留
            seal.setPasswordHash(null);
        } else if (StringUtils.isNotBlank(request.getPassword())) {
            seal.setPasswordHash(encodePassword(request.getPassword()));
        } else if (StringUtils.isBlank(seal.getPasswordHash())) {
            // 已启用保护且历史上没有哈希时，必须同时设置密码
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "启用密码保护时必须设置印章密码");
        }
        electronicSealMapper.updateById(seal);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireSeal(id);
        Long referenceCount =
                electronicSealApplicationMapper.selectCount(
                        new LambdaQueryWrapper<ElectronicSealApplication>()
                                .eq(ElectronicSealApplication::getSealId, id));
        if (referenceCount != null && referenceCount > 0) {
            throw BusinessException.of(ErrorCode.BIZ_REFERENCED, "该印章存在用印申请，无法删除");
        }
        electronicSealMapper.deleteById(id);
    }

    private ElectronicSeal requireSeal(Long id) {
        ElectronicSeal seal = electronicSealMapper.selectById(id);
        if (seal == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "电子印章不存在");
        }
        return seal;
    }

    private void ensureCodeUnique(String sealCode, Long excludeId) {
        if (StringUtils.isBlank(sealCode)) {
            return;
        }
        LambdaQueryWrapper<ElectronicSeal> wrapper =
                new LambdaQueryWrapper<ElectronicSeal>()
                        .eq(ElectronicSeal::getSealCode, sealCode)
                        .ne(excludeId != null, ElectronicSeal::getId, excludeId);
        Long count = electronicSealMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw BusinessException.of(ErrorCode.BIZ_DUPLICATE_CODE, "印章编码已存在");
        }
    }

    /** 校验并加密印章密码：启用密码保护时密码必填且不少于 6 位。 */
    private String encodePassword(String rawPassword) {
        if (StringUtils.isBlank(rawPassword)) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "启用密码保护时必须设置印章密码");
        }
        if (rawPassword.length() < 6) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "印章密码长度不能少于 6 位");
        }
        return passwordEncoder.encode(rawPassword);
    }
}
