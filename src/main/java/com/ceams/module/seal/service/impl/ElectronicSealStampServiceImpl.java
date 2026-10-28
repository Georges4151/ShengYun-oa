package com.ceams.module.seal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.seal.converter.ElectronicSealStampConverter;
import com.ceams.module.seal.entity.ElectronicSealStamp;
import com.ceams.module.seal.mapper.ElectronicSealStampMapper;
import com.ceams.module.seal.service.ElectronicSealStampService;
import com.ceams.module.seal.util.PageUtil;
import com.ceams.module.seal.vo.ElectronicSealStampVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 用印盖章记录服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElectronicSealStampServiceImpl implements ElectronicSealStampService {

    private final ElectronicSealStampMapper electronicSealStampMapper;
    private final ElectronicSealStampConverter electronicSealStampConverter;

    @Value("${ceams.file.stamp-dir:./data/stamps}")
    private String stampDir;

    @Override
    public PageResult<ElectronicSealStampVO> page(long page, long size, Long sealId, Long projectId) {
        Page<ElectronicSealStamp> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<ElectronicSealStamp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(sealId != null, ElectronicSealStamp::getSealId, sealId);
        wrapper.eq(projectId != null, ElectronicSealStamp::getProjectId, projectId);
        wrapper.orderByDesc(ElectronicSealStamp::getStampTime);
        IPage<ElectronicSealStamp> result = electronicSealStampMapper.selectPage(p, wrapper);
        return new PageResult<>(electronicSealStampConverter.toVOList(result.getRecords()),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public ElectronicSealStampVO detail(Long id) {
        return electronicSealStampConverter.toVO(requireStamp(id));
    }

    @Override
    public byte[] download(Long id) {
        requireStamp(id);
        Path file = Path.of(stampDir, id + ".pdf");
        if (!Files.exists(file)) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "盖章文件不存在");
        }
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            log.error("读取盖章文件失败: {}", file, e);
            throw new BusinessException(ErrorCode.BIZ_UNPROCESSABLE, "读取盖章文件失败", e);
        }
    }

    private ElectronicSealStamp requireStamp(Long id) {
        ElectronicSealStamp stamp = electronicSealStampMapper.selectById(id);
        if (stamp == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "盖章记录不存在");
        }
        return stamp;
    }
}
