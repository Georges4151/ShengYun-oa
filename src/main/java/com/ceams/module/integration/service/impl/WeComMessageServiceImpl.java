package com.ceams.module.integration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.bpm.enums.AuditAction;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.bpm.support.BpmSecurityGuard;
import com.ceams.module.integration.entity.IntegrationWecomMessage;
import com.ceams.module.integration.enums.WeComMessageStatus;
import com.ceams.module.integration.mapper.IntegrationWecomMessageMapper;
import com.ceams.module.integration.service.WeComMessageService;
import com.ceams.module.integration.vo.WeComMessageVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 企业微信应用消息（发件箱）服务实现。
 *
 * <p>口径：
 *
 * <ul>
 *   <li>分页要求 {@code integration:wecom:view}，手工重投要求 {@code integration:wecom:sync}；
 *   <li>仅 {@code FAILED} 消息可重投，重投后复位为 {@code PENDING}（{@code retryCount=0}、
 *       {@code next_retry_at=now}），由调度器下一轮消费；
 *   <li>重投写审计 {@code WECOM_MESSAGE_RETRY}（目标类型 {@code INTEGRATION_WECOM_MESSAGE}），
 *       审计失败只记日志，不影响重投结果。
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeComMessageServiceImpl implements WeComMessageService {

    /** 查看发件箱权限码。 */
    private static final String PERMISSION_VIEW = "integration:wecom:view";

    /** 手工重投权限码。 */
    private static final String PERMISSION_SYNC = "integration:wecom:sync";

    /** 单页最大条数。 */
    private static final int MAX_PAGE_SIZE = 100;

    private final IntegrationWecomMessageMapper messageMapper;
    private final BpmSecurityGuard securityGuard;
    private final BpmAuditService auditService;

    @Override
    public PageResult<WeComMessageVO> page(int page, int size, String status) {
        securityGuard.requirePermission(PERMISSION_VIEW);
        if (page < 1) {
            throw BusinessException.of(ErrorCode.VALID_PAGE_BOUNDS, "页码必须不小于 1");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw BusinessException.of(
                    ErrorCode.VALID_PAGE_BOUNDS, "每页条数必须在 1~" + MAX_PAGE_SIZE + " 之间");
        }
        WeComMessageStatus statusFilter = parseStatus(status);
        QueryWrapper<IntegrationWecomMessage> wrapper = new QueryWrapper<>();
        if (statusFilter != null) {
            wrapper.eq("status", statusFilter.name());
        }
        wrapper.orderByDesc("id");
        IPage<IntegrationWecomMessage> result =
                messageMapper.selectPage(new Page<>(page, size), wrapper);
        List<WeComMessageVO> records = result.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public void retry(Long id) {
        securityGuard.requirePermission(PERMISSION_SYNC);
        if (id == null) {
            throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "消息 ID 不能为空");
        }
        IntegrationWecomMessage message = messageMapper.selectById(id);
        if (message == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "应用消息不存在");
        }
        if (!WeComMessageStatus.FAILED.name().equals(message.getStatus())) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "仅失败消息可重投");
        }
        UpdateWrapper<IntegrationWecomMessage> wrapper =
                new UpdateWrapper<IntegrationWecomMessage>()
                        .eq("id", id)
                        .set("status", WeComMessageStatus.PENDING.name())
                        .set("retry_count", 0)
                        .set("next_retry_at", LocalDateTime.now())
                        .set("error_msg", null);
        messageMapper.update(null, wrapper);
        recordAudit(id, message.getStatus());
        log.info("企业微信应用消息已手工重投：messageId={}", id);
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    /** 重投审计：before 为原状态，after 为 PENDING；审计失败不影响重投。 */
    private void recordAudit(Long id, String beforeStatus) {
        try {
            auditService.record(
                    TARGET_WECOM_MESSAGE,
                    id,
                    AuditAction.WECOM_MESSAGE_RETRY,
                    beforeStatus,
                    WeComMessageStatus.PENDING.name(),
                    null,
                    securityGuard.currentUserId());
        } catch (Exception e) {
            log.warn("写入企业微信应用消息重投审计失败：messageId={}，原因={}", id, e.getMessage());
        }
    }

    private WeComMessageStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return WeComMessageStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "消息状态非法：" + status);
        }
    }

    private WeComMessageVO toVO(IntegrationWecomMessage message) {
        WeComMessageVO vo = new WeComMessageVO();
        vo.setId(message.getId());
        vo.setNotificationId(message.getNotificationId());
        vo.setTargetUserId(message.getTargetUserId());
        vo.setWecomUserid(message.getWecomUserid());
        vo.setMsgType(message.getMsgType());
        vo.setStatus(message.getStatus());
        vo.setRetryCount(message.getRetryCount());
        vo.setNextRetryAt(message.getNextRetryAt());
        vo.setSentAt(message.getSentAt());
        vo.setErrorMsg(message.getErrorMsg());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }
}
