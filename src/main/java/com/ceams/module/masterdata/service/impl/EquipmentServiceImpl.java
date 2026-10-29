package com.ceams.module.masterdata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.masterdata.converter.EquipmentConverter;
import com.ceams.module.masterdata.dto.equipment.EquipmentCreateRequest;
import com.ceams.module.masterdata.dto.equipment.EquipmentUpdateRequest;
import com.ceams.module.masterdata.entity.Equipment;
import com.ceams.module.masterdata.entity.EquipmentLease;
import com.ceams.module.masterdata.enums.EquipmentStatus;
import com.ceams.module.masterdata.mapper.EquipmentLeaseMapper;
import com.ceams.module.masterdata.mapper.EquipmentMapper;
import com.ceams.module.masterdata.service.EquipmentService;
import com.ceams.module.masterdata.util.PageUtil;
import com.ceams.module.masterdata.vo.EquipmentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 设备台账服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentServiceImpl implements EquipmentService {

    private final EquipmentMapper equipmentMapper;
    private final EquipmentLeaseMapper equipmentLeaseMapper;
    private final EquipmentConverter equipmentConverter;

    @Override
    public PageResult<EquipmentVO> page(
            long page, long size, String keyword, EquipmentStatus status) {
        Page<Equipment> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<Equipment> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(
                    w ->
                            w.like(Equipment::getName, keyword)
                                    .or()
                                    .like(Equipment::getModel, keyword));
        }
        if (status != null) {
            wrapper.eq(Equipment::getStatus, status);
        }
        wrapper.orderByDesc(Equipment::getCreateTime);
        IPage<Equipment> result = equipmentMapper.selectPage(p, wrapper);
        return new PageResult<>(
                equipmentConverter.toVOList(result.getRecords()),
                result.getTotal(),
                result.getCurrent(),
                result.getSize());
    }

    @Override
    public EquipmentVO detail(Long id) {
        return equipmentConverter.toVO(requireEquipment(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(EquipmentCreateRequest request) {
        Equipment equipment = new Equipment();
        equipment.setName(request.getName());
        equipment.setType(request.getType());
        equipment.setModel(request.getModel());
        equipment.setStatus(request.getStatus());
        equipment.setPurchaseDate(
                request.getPurchaseDate() == null
                        ? null
                        : request.getPurchaseDate().atStartOfDay());
        equipment.setPurchasePrice(request.getPurchasePrice());
        equipmentMapper.insert(equipment);
        return String.valueOf(equipment.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, EquipmentUpdateRequest request) {
        Equipment equipment = requireEquipment(id);
        if (StringUtils.isNotBlank(request.getName())) {
            equipment.setName(request.getName());
        }
        if (StringUtils.isNotBlank(request.getType())) {
            equipment.setType(request.getType());
        }
        if (request.getModel() != null) {
            equipment.setModel(request.getModel());
        }
        if (request.getStatus() != null) {
            equipment.setStatus(request.getStatus());
        }
        if (request.getPurchaseDate() != null) {
            equipment.setPurchaseDate(request.getPurchaseDate().atStartOfDay());
        }
        if (request.getPurchasePrice() != null) {
            equipment.setPurchasePrice(request.getPurchasePrice());
        }
        equipmentMapper.updateById(equipment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireEquipment(id);
        Long leaseRefs =
                equipmentLeaseMapper.selectCount(
                        new LambdaQueryWrapper<EquipmentLease>()
                                .eq(EquipmentLease::getEquipmentId, id));
        if (leaseRefs != null && leaseRefs > 0) {
            throw BusinessException.of(ErrorCode.BIZ_REFERENCED, "设备已被租赁记录引用，无法删除");
        }
        equipmentMapper.deleteById(id);
    }

    private Equipment requireEquipment(Long id) {
        Equipment equipment = equipmentMapper.selectById(id);
        if (equipment == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "设备不存在");
        }
        return equipment;
    }
}
