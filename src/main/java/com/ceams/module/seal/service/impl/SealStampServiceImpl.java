package com.ceams.module.seal.service.impl;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.pdf.PdfStampService;
import com.ceams.common.pdf.StampRequest;
import com.ceams.common.security.SecurityUtils;
import com.ceams.common.web.ClientIpResolver;
import com.ceams.module.seal.dto.SealStampRequest;
import com.ceams.module.seal.entity.ElectronicSeal;
import com.ceams.module.seal.entity.ElectronicSealApplication;
import com.ceams.module.seal.entity.ElectronicSealStamp;
import com.ceams.module.seal.entity.ElectronicSealUsage;
import com.ceams.module.seal.enums.ApplicationStatus;
import com.ceams.module.seal.enums.SealStatus;
import com.ceams.module.seal.mapper.ElectronicSealApplicationMapper;
import com.ceams.module.seal.mapper.ElectronicSealMapper;
import com.ceams.module.seal.mapper.ElectronicSealStampMapper;
import com.ceams.module.seal.mapper.ElectronicSealUsageMapper;
import com.ceams.module.seal.service.SealStampService;
import com.ceams.module.seal.vo.SealStampResultVO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PDF 盖章服务实现（HMAC 高危接口）。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SealStampServiceImpl implements SealStampService {

    private final PdfStampService pdfStampService;
    private final ElectronicSealMapper electronicSealMapper;
    private final ElectronicSealApplicationMapper electronicSealApplicationMapper;
    private final ElectronicSealStampMapper electronicSealStampMapper;
    private final ElectronicSealUsageMapper electronicSealUsageMapper;

    @Value("${ceams.file.stamp-dir:./data/stamps}")
    private String stampDir;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SealStampResultVO stamp(SealStampRequest request) {
        ElectronicSeal seal = requireActiveSeal(request.getSealId());
        ElectronicSealApplication application =
                requireApprovedApplication(request.getApplicationId(), seal.getId());

        byte[] sourcePdf = decodeBase64(request.getSourcePdfBase64(), "sourcePdfBase64");
        byte[] sealImage = decodeBase64(request.getSealImageBase64(), "sealImageBase64");

        byte[] stampedPdf =
                pdfStampService.stamp(
                        sourcePdf, buildStampRequest(sealImage, request.getStampPosition()));

        // 审计 IP 仅解析一次；非 Web 线程（如定时任务）解析为 null，允许为空
        String ipAddress = ClientIpResolver.resolveCurrentRequest();

        ElectronicSealStamp stamp = new ElectronicSealStamp();
        stamp.setSealId(seal.getId());
        stamp.setSealName(seal.getSealName());
        stamp.setProjectId(application.getProjectId());
        stamp.setProjectName(application.getProjectName());
        stamp.setContractName(application.getContractName());
        stamp.setContractPath(application.getContractPath());
        stamp.setStampPosition(
                StringUtils.isNotBlank(request.getStampPosition())
                        ? request.getStampPosition()
                        : application.getStampPosition());
        stamp.setStampImage(seal.getSealImage());
        stamp.setStampTime(LocalDateTime.now());
        stamp.setUserId(SecurityUtils.getCurrentUserId());
        stamp.setIpAddress(ipAddress);
        electronicSealStampMapper.insert(stamp);

        String downloadPath = persistPdf(stampedPdf, stamp.getId());

        ElectronicSealUsage usage = new ElectronicSealUsage();
        usage.setSealId(seal.getId());
        usage.setSealName(seal.getSealName());
        usage.setUserId(SecurityUtils.getCurrentUserId());
        usage.setPurpose(application.getPurpose());
        usage.setDocumentType("PDF");
        usage.setDocumentPath(application.getContractPath());
        usage.setUsageTime(LocalDateTime.now());
        usage.setIpAddress(ipAddress);
        electronicSealUsageMapper.insert(usage);

        seal.setUsageCount((seal.getUsageCount() == null ? 0 : seal.getUsageCount()) + 1);
        seal.setLastUsedTime(LocalDateTime.now());
        electronicSealMapper.updateById(seal);

        SealStampResultVO result = new SealStampResultVO();
        result.setStampId(stamp.getId());
        result.setDownloadPath(downloadPath);
        return result;
    }

    private ElectronicSeal requireActiveSeal(Long sealId) {
        ElectronicSeal seal = electronicSealMapper.selectById(sealId);
        if (seal == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "电子印章不存在");
        }
        if (seal.getStatus() != SealStatus.ACTIVE) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "印章已停用或归档，不可盖章");
        }
        return seal;
    }

    private ElectronicSealApplication requireApprovedApplication(Long applicationId, Long sealId) {
        ElectronicSealApplication application =
                electronicSealApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "用印申请不存在");
        }
        if (application.getStatus() != ApplicationStatus.APPROVED) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "用印申请未通过审批，不可盖章");
        }
        if (!sealId.equals(application.getSealId())) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "用印申请与印章不匹配");
        }
        return application;
    }

    private StampRequest buildStampRequest(byte[] sealImage, String stampPosition) {
        Integer x = null;
        Integer y = null;
        if (StringUtils.isNotBlank(stampPosition)) {
            String[] parts = stampPosition.split(",");
            if (parts.length == 2) {
                try {
                    x = Integer.parseInt(parts[0].trim());
                    y = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "盖章位置格式非法，应为 x,y");
                }
            }
        }
        return new StampRequest(sealImage, x, y, 0, 1.0f);
    }

    private byte[] decodeBase64(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, fieldName + " 不能为空");
        }
        String data = value;
        int comma = data.indexOf(',');
        if (comma >= 0) {
            data = data.substring(comma + 1);
        }
        try {
            return Base64.getDecoder().decode(data.trim());
        } catch (IllegalArgumentException e) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, fieldName + " 不是合法的 Base64");
        }
    }

    private String persistPdf(byte[] stampedPdf, Long stampId) {
        try {
            Path dir = Path.of(stampDir);
            Files.createDirectories(dir);
            Path target = dir.resolve(stampId + ".pdf");
            Files.write(target, stampedPdf);
            return "/api/v1/seal/stamps/" + stampId + "/download";
        } catch (IOException e) {
            log.error("盖章文件落盘失败", e);
            throw new BusinessException(ErrorCode.BIZ_UNPROCESSABLE, "盖章文件落盘失败", e);
        }
    }
}
