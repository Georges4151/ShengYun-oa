package com.ceams.module.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.iam.converter.DeviceConverter;
import com.ceams.module.iam.dto.device.DeviceBindRequest;
import com.ceams.module.iam.entity.SysUserDevice;
import com.ceams.module.iam.enums.DeviceStatus;
import com.ceams.module.iam.mapper.UserDeviceMapper;
import com.ceams.module.iam.service.DeviceService;
import com.ceams.module.iam.util.PageUtil;
import com.ceams.module.iam.vo.UserDeviceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 设备绑定服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final UserDeviceMapper userDeviceMapper;
    private final DeviceConverter deviceConverter;

    @Override
    public PageResult<UserDeviceVO> page(long page, long size) {
        Long userId = SecurityUtils.getCurrentUserIdOrDefault();
        Page<SysUserDevice> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<SysUserDevice> wrapper = new LambdaQueryWrapper<SysUserDevice>()
                .eq(SysUserDevice::getUserId, userId)
                .orderByDesc(SysUserDevice::getCreateTime);
        IPage<SysUserDevice> result = userDeviceMapper.selectPage(p, wrapper);
        return new PageResult<>(deviceConverter.toVOList(result.getRecords()),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String bind(DeviceBindRequest request) {
        Long userId = SecurityUtils.getCurrentUserIdOrDefault();
        LocalDateTime now = LocalDateTime.now();
        SysUserDevice device = userDeviceMapper.selectOne(
                new LambdaQueryWrapper<SysUserDevice>()
                        .eq(SysUserDevice::getUserId, userId)
                        .eq(SysUserDevice::getDeviceId, request.getDeviceId())
                        .last("LIMIT 1"));
        if (device == null) {
            device = new SysUserDevice();
            device.setUserId(userId);
            device.setDeviceId(request.getDeviceId());
            device.setBindTime(now);
            device.setCreateBy(userId);
        }
        device.setDeviceName(request.getDeviceName());
        device.setPlatform(request.getPlatform());
        device.setStatus(DeviceStatus.ACTIVE);
        device.setLastLoginTime(now);
        if (device.getId() == null) {
            userDeviceMapper.insert(device);
        } else {
            userDeviceMapper.updateById(device);
        }
        return String.valueOf(device.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbind(Long id) {
        Long userId = SecurityUtils.getCurrentUserIdOrDefault();
        SysUserDevice device = userDeviceMapper.selectById(id);
        if (device == null || !device.getUserId().equals(userId)) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "设备不存在");
        }
        device.setStatus(DeviceStatus.UNBOUND);
        userDeviceMapper.updateById(device);
    }
}