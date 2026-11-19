package com.ceams.module.masterdata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.masterdata.converter.EquipmentLeaseConverter;
import com.ceams.module.masterdata.dto.lease.EquipmentLeaseCreateRequest;
import com.ceams.module.masterdata.dto.lease.EquipmentLeaseUpdateRequest;
import com.ceams.module.masterdata.entity.Contact;
import com.ceams.module.masterdata.entity.Equipment;
import com.ceams.module.masterdata.entity.EquipmentLease;
import com.ceams.module.masterdata.mapper.ContactMapper;
import com.ceams.module.masterdata.mapper.EquipmentLeaseMapper;
import com.ceams.module.masterdata.mapper.EquipmentMapper;
import com.ceams.module.masterdata.service.EquipmentLeaseService;
import com.ceams.module.masterdata.util.PageUtil;
import com.ceams.module.masterdata.vo.EquipmentLeaseVO;
import com.ceams.module.project.entity.Project;
import com.ceams.module.project.mapper.ProjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    private final ContactMapper contactMapper;
    private final ProjectMapper projectMapper;
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
        List<EquipmentLeaseVO> vos = equipmentLeaseConverter.toVOList(result.getRecords());
        fillNames(vos);
        return new PageResult<>(vos, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public EquipmentLeaseVO detail(Long id) {
        EquipmentLeaseVO vo = equipmentLeaseConverter.toVO(requireLease(id));
        fillNames(Collections.singletonList(vo));
        return vo;
    }

    /** 回填设备 / 项目 / 出租方名称，避免前端只展示 ID。 */
    private void fillNames(List<EquipmentLeaseVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        Map<Long, String> equipmentNames =
                nameMap(
                        equipmentMapper,
                        vos.stream()
                                .map(EquipmentLeaseVO::getEquipmentId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet()),
                        Equipment::getId,
                        Equipment::getName);
        Map<Long, String> projectNames =
                nameMap(
                        projectMapper,
                        vos.stream()
                                .map(EquipmentLeaseVO::getProjectId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet()),
                        Project::getId,
                        Project::getProjectName);
        Map<Long, String> lessorNames =
                nameMap(
                        contactMapper,
                        vos.stream()
                                .map(EquipmentLeaseVO::getLessorId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet()),
                        Contact::getId,
                        Contact::getName);
        for (EquipmentLeaseVO vo : vos) {
            vo.setEquipmentName(equipmentNames.get(vo.getEquipmentId()));
            vo.setProjectName(projectNames.get(vo.getProjectId()));
            vo.setLessorName(vo.getLessorId() == null ? null : lessorNames.get(vo.getLessorId()));
        }
    }

    /** 批量查询后构建 id -> 名称 映射；空集合直接返回空 Map，避免 MyBatis 生成 IN ()。 */
    private <T> Map<Long, String> nameMap(
            BaseMapper<T> mapper,
            Set<Long> ids,
            Function<T, Long> idGetter,
            Function<T, String> nameGetter) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<T> rows = mapper.selectBatchIds(ids);
        return rows.stream()
                .filter(item -> idGetter.apply(item) != null)
                .collect(Collectors.toMap(idGetter, nameGetter, (first, second) -> first));
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
