package com.ceams.module.masterdata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.masterdata.converter.EquipmentLeaseConverter;
import com.ceams.module.masterdata.dto.lease.EquipmentLeaseCreateRequest;
import com.ceams.module.masterdata.dto.lease.EquipmentLeaseUpdateRequest;
import com.ceams.module.masterdata.entity.EquipmentLease;
import com.ceams.module.masterdata.mapper.EquipmentLeaseMapper;
import com.ceams.module.masterdata.mapper.EquipmentMapper;
import com.ceams.module.masterdata.service.EquipmentLeaseService;
import com.ceams.module.masterdata.util.PageUtil;
import com.ceams.module.masterdata.vo.EquipmentLeaseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 设备租赁服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentLeaseServiceImpl implements EquipmentLeaseService {

    private final EquipmentLeaseMapper equipmentLeaseMapper;
    private final EquipmentMapper equipmentMapper;
    private final EquipmentLeaseConverter equipmentLeaseConverter;

    @Override
    public PageResult<EquipmentLeaseVO> page(
            long page, long size, Long equipmentId, Long projectId) {
        Page<EquipmentLease> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<EquipmentLease> wrapper = new LambdaQueryWrapper<>();
        if (equipmentId != null) {
            wrapper.eq(EquipmentLease::getEquipmentId, equipmentId);
        }
        if (projectId != null) {
            wrapper.eq(EquipmentLease::getProjectId, projectId);
        }
        wrapper.orderByDesc(EquipmentLease::getCreateTime);
        IPage<EquipmentLease> result = equipmentLeaseMapper.selectPage(p, wrapper);
        return new PageResult<>(
                equipmentLeaseConverter.toVOList(result.getRecords()),
                result.getTotal(),
                result.getCurrent(),
                result.getSize());
    }

    @Override
    public EquipmentLeaseVO detail(Long id) {
        return equipmentLeaseConverter.toVO(requireLease(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(EquipmentLeaseCreateRequest request) {
        if (equipmentMapper.selectById(request.getEquipmentId()) == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "设备不存在");
        }
        if (request.getLeaseEndDate().isBefore(request.getLeaseStartDate())) {
            throw BusinessException.of(ErrorCode.BIZ_UNPROCESSABLE, "租赁结束日期不能早于开始日期");
        }
        EquipmentLease lease = new EquipmentLease();
        lease.setEquipmentId(request.getEquipmentId());
        lease.setProjectId(request.getProjectId());
        lease.setLessorId(request.getLessorId());
        lease.setLeaseStartDate(request.getLeaseStartDate().atStartOfDay());
        lease.setLeaseEndDate(request.getLeaseEndDate().atStartOfDay());
        lease.setDailyRent(request.getDailyRent());
        lease.setTotalRent(request.getTotalRent());
        lease.setPaymentStatus(request.getPaymentStatus());
        equipmentLeaseMapper.insert(lease);
        return String.valueOf(lease.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, EquipmentLeaseUpdateRequest request) {
        EquipmentLease lease = requireLease(id);
        if (request.getProjectId() != null) {
            lease.setProjectId(request.getProjectId());
        }
        if (request.getLessorId() != null) {
            lease.setLessorId(request.getLessorId());
        }
        if (request.getLeaseStartDate() != null) {
            lease.setLeaseStartDate(request.getLeaseStartDate().atStartOfDay());
        }
        if (request.getLeaseEndDate() != null) {
            lease.setLeaseEndDate(request.getLeaseEndDate().atStartOfDay());
        }
        if (request.getDailyRent() != null) {
            lease.setDailyRent(request.getDailyRent());
        }
        if (request.getTotalRent() != null) {
            lease.setTotalRent(request.getTotalRent());
        }
        if (request.getPaymentStatus() != null) {
            lease.setPaymentStatus(request.getPaymentStatus());
        }
        if (lease.getLeaseStartDate() != null
                && lease.getLeaseEndDate() != null
                && lease.getLeaseEndDate().isBefore(lease.getLeaseStartDate())) {
            throw BusinessException.of(ErrorCode.BIZ_UNPROCESSABLE, "租赁结束日期不能早于开始日期");
        }
        equipmentLeaseMapper.updateById(lease);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireLease(id);
        equipmentLeaseMapper.deleteById(id);
    }

    private EquipmentLease requireLease(Long id) {
        EquipmentLease lease = equipmentLeaseMapper.selectById(id);
        if (lease == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "设备租赁记录不存在");
        }
        return lease;
    }
}
