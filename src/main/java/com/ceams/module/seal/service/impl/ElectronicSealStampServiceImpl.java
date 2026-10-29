package com.ceams.module.seal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.common.security.SecurityUtils;
import com.ceams.common.sign.HmacSignUtil;
import com.ceams.module.iam.entity.OperationLog;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.enums.OperationType;
import com.ceams.module.iam.mapper.OperationLogMapper;
import com.ceams.module.iam.mapper.UserMapper;
import com.ceams.module.seal.converter.ElectronicSealStampConverter;
import com.ceams.module.seal.entity.ElectronicSealStamp;
import com.ceams.module.seal.mapper.ElectronicSealStampMapper;
import com.ceams.module.seal.service.ElectronicSealStampService;
import com.ceams.module.seal.util.PageUtil;
import com.ceams.module.seal.vo.DownloadUrlVO;
import com.ceams.module.seal.vo.ElectronicSealStampVO;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** 用印盖章记录服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElectronicSealStampServiceImpl implements ElectronicSealStampService {

    private final ElectronicSealStampMapper electronicSealStampMapper;
    private final ElectronicSealStampConverter electronicSealStampConverter;
    private final OperationLogMapper operationLogMapper;
    private final UserMapper userMapper;

    @Value("${ceams.file.stamp-dir:./data/stamps}")
    private String stampDir;

    /** 下载签名 URL 专用密钥（仅服务端生成/校验下载链接，与请求 HMAC 的 AppSecret 无关）。 */
    @Value("${ceams.export.secret}")
    private String exportSecret;

    /** 下载签名 URL 有效期（毫秒）。 */
    @Value("${ceams.export.url-ttl-millis:300000}")
    private long urlTtlMillis;

    @Override
    public PageResult<ElectronicSealStampVO> page(
            long page, long size, Long sealId, Long projectId) {
        Page<ElectronicSealStamp> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<ElectronicSealStamp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(sealId != null, ElectronicSealStamp::getSealId, sealId);
        wrapper.eq(projectId != null, ElectronicSealStamp::getProjectId, projectId);
        wrapper.orderByDesc(ElectronicSealStamp::getStampTime);
        IPage<ElectronicSealStamp> result = electronicSealStampMapper.selectPage(p, wrapper);
        return new PageResult<>(
                electronicSealStampConverter.toVOList(result.getRecords()),
                result.getTotal(),
                result.getCurrent(),
                result.getSize());
    }

    @Override
    public ElectronicSealStampVO detail(Long id) {
        return electronicSealStampConverter.toVO(requireStamp(id));
    }

    @Override
    public DownloadUrlVO downloadUrl(Long id) {
        // 记录必须存在才允许生成下载链接
        requireStamp(id);
        long expires = System.currentTimeMillis() + urlTtlMillis;
        String sign = downloadSign(id, expires);
        String url = "/api/v1/seal/stamps/" + id + "/download?expires=" + expires + "&sign=" + sign;
        return new DownloadUrlVO(url, expires);
    }

    @Override
    public byte[] download(Long id, Long expires, String sign, HttpServletRequest request) {
        if (expires == null || StringUtils.isBlank(sign)) {
            throw BusinessException.of(ErrorCode.SIGN_INVALID, "下载链接缺少签名参数");
        }
        if (System.currentTimeMillis() > expires) {
            throw BusinessException.of(ErrorCode.SIGN_TIMESTAMP_EXPIRED, "下载链接已过期");
        }
        String expected = downloadSign(id, expires);
        if (!HmacSignUtil.constantTimeEquals(expected, sign)) {
            throw BusinessException.of(ErrorCode.SIGN_INVALID, "下载链接签名无效");
        }

        requireStamp(id);
        byte[] bytes = readFile(id);
        logDownload(id, request);
        return bytes;
    }

    /**
     * 计算下载 URL 签名：{@code sign = HMAC-SHA256(exportSecret, id + ":" + expires)}。
     *
     * <p>复用 {@link HmacSignUtil#sign(String, String, String, String)}，将 {@code (id, ":", expires)}
     * 依次代入三个 payload 片段，等价于对 {@code id + ":" + expires} 做 HMAC-SHA256，校验端按同样参数复算即可。
     */
    private String downloadSign(Long id, long expires) {
        return HmacSignUtil.sign(String.valueOf(id), ":", String.valueOf(expires), exportSecret);
    }

    private byte[] readFile(Long id) {
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

    /** 下载留痕：写入 sys_operation_log（operation_type=EXPORT）。 */
    private void logDownload(Long id, HttpServletRequest request) {
        Long userId = SecurityUtils.getCurrentUserIdOrDefault();
        OperationLog op = new OperationLog();
        op.setUserId(userId);
        op.setUsername(resolveUsername(userId));
        op.setOperationType(OperationType.EXPORT);
        op.setOperationDescription("下载盖章 PDF");
        op.setTargetType("STAMP");
        op.setTargetId(id);
        op.setIpAddress(resolveIp(request));
        op.setUserAgent(request == null ? null : request.getHeader("User-Agent"));
        op.setRequestMethod(request == null ? null : request.getMethod());
        op.setRequestUri(request == null ? null : request.getRequestURI());
        op.setStatus("SUCCESS");
        operationLogMapper.insert(op);
    }

    private String resolveUsername(Long userId) {
        if (userId == null || userId == 0L) {
            return null;
        }
        SysUser user = userMapper.selectById(userId);
        return user == null ? null : user.getUsername();
    }

    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
