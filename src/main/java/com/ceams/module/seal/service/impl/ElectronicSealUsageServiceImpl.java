package com.ceams.module.seal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.seal.converter.ElectronicSealUsageConverter;
import com.ceams.module.seal.entity.ElectronicSealUsage;
import com.ceams.module.seal.mapper.ElectronicSealUsageMapper;
import com.ceams.module.seal.service.ElectronicSealUsageService;
import com.ceams.module.seal.util.PageUtil;
import com.ceams.module.seal.vo.ElectronicSealUsageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 印章使用审计服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElectronicSealUsageServiceImpl implements ElectronicSealUsageService {

    private final ElectronicSealUsageMapper electronicSealUsageMapper;
    private final ElectronicSealUsageConverter electronicSealUsageConverter;

    @Override
    public PageResult<ElectronicSealUsageVO> page(long page, long size, Long sealId, Long userId) {
        Page<ElectronicSealUsage> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<ElectronicSealUsage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(sealId != null, ElectronicSealUsage::getSealId, sealId);
        wrapper.eq(userId != null, ElectronicSealUsage::getUserId, userId);
        wrapper.orderByDesc(ElectronicSealUsage::getUsageTime);
        IPage<ElectronicSealUsage> result = electronicSealUsageMapper.selectPage(p, wrapper);
        return new PageResult<>(
                electronicSealUsageConverter.toVOList(result.getRecords()),
                result.getTotal(),
                result.getCurrent(),
                result.getSize());
    }

    @Override
    public ElectronicSealUsageVO detail(Long id) {
        return electronicSealUsageConverter.toVO(requireUsage(id));
    }

    private ElectronicSealUsage requireUsage(Long id) {
        ElectronicSealUsage usage = electronicSealUsageMapper.selectById(id);
        if (usage == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "使用审计记录不存在");
        }
        return usage;
    }
}
