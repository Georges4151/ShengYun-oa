package com.ceams.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.admin.config.AdminProperties;
import com.ceams.module.admin.dto.VisitorCheckinRequest;
import com.ceams.module.admin.dto.VisitorCreateRequest;
import com.ceams.module.admin.entity.AdminVisitor;
import com.ceams.module.admin.enums.VisitorStatus;
import com.ceams.module.admin.mapper.AdminVisitorMapper;
import com.ceams.module.admin.service.AdminVisitorService;
import com.ceams.module.admin.support.AdminConverter;
import com.ceams.module.admin.vo.VisitorQrVO;
import com.ceams.module.admin.vo.VisitorVO;
import com.ceams.module.hr.util.HrPageUtil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 行政访客服务实现。
 *
 * <p>二维码 token 一次性有效：核销成功后置 NULL，重复核销、跨日、过期统一按
 * {@code BIZ_STATE_CONFLICT} 返回「二维码无效或已使用」；手机号入库加密、出参脱敏。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminVisitorServiceImpl implements AdminVisitorService {

    /** 核销失败统一提示，避免暴露 token 状态细节。 */
    private static final String INVALID_TOKEN_MESSAGE = "二维码无效或已使用";

    private final AdminVisitorMapper visitorMapper;
    private final AdminProperties adminProperties;

    @Override
    @Transactional
    public VisitorQrVO create(VisitorCreateRequest request) {
        Long hostUserId =
                request.getHostUserId() == null
                        ? SecurityUtils.getCurrentUserIdOrDefault()
                        : request.getHostUserId();

        AdminVisitor visitor = new AdminVisitor();
        visitor.setVisitorName(request.getVisitorName().trim());
        visitor.setPhoneEnc(request.getPhone().trim());
        visitor.setCompany(request.getCompany());
        visitor.setHostUserId(hostUserId);
        visitor.setVisitDate(request.getVisitDate());
        visitor.setPurpose(request.getPurpose());
        visitor.setStatus(VisitorStatus.PENDING);
        visitor.setQrToken(newToken());
        visitorMapper.insert(visitor);
        log.info("访客登记成功：{}，来访日期 {}", visitor.getId(), visitor.getVisitDate());
        return buildQrVO(visitor);
    }

    @Override
    public PageResult<VisitorVO> list(
            LocalDate date, VisitorStatus status, long page, long size) {
        Page<AdminVisitor> p = HrPageUtil.resolve(page, size);
        LambdaQueryWrapper<AdminVisitor> wrapper = new LambdaQueryWrapper<>();
        if (date != null) {
            wrapper.eq(AdminVisitor::getVisitDate, date);
        }
        if (status != null) {
            wrapper.eq(AdminVisitor::getStatus, status);
        }
        wrapper.orderByDesc(AdminVisitor::getCreateTime);
        IPage<AdminVisitor> result = visitorMapper.selectPage(p, wrapper);
        List<VisitorVO> records =
                result.getRecords().stream().map(AdminConverter::toVisitorVO).toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public VisitorQrVO qr(Long id) {
        AdminVisitor visitor = requireVisitor(id);
        requireHostOrAdmin(visitor, "仅被访人或管理员可重新签发二维码");
        if (visitor.getStatus() != VisitorStatus.PENDING
                || !LocalDate.now().equals(visitor.getVisitDate())) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅当天待核销的访客可重新签发二维码");
        }

        String token = newToken();
        AdminVisitor patch = new AdminVisitor();
        // 条件更新已保证并发安全，version 置空避免叠加乐观锁导致条件更新失效
        patch.setVersion(null);
        patch.setQrToken(token);
        int rows =
                visitorMapper.update(
                        patch,
                        new LambdaUpdateWrapper<AdminVisitor>()
                                .eq(AdminVisitor::getId, id)
                                .eq(AdminVisitor::getStatus, VisitorStatus.PENDING));
        if (rows == 0) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "访客状态已变更，请刷新后重试");
        }
        visitor.setQrToken(token);
        log.info("访客 {} 重新签发二维码", id);
        return buildQrVO(visitor);
    }

    @Override
    @Transactional
    public void checkin(VisitorCheckinRequest request) {
        String token = request == null ? null : request.getToken();
        if (StringUtils.isBlank(token)) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "核销令牌不能为空");
        }
        String trimmed = token.trim();
        AdminVisitor visitor =
                visitorMapper.selectOne(
                        new LambdaQueryWrapper<AdminVisitor>()
                                .eq(AdminVisitor::getQrToken, trimmed));
        if (visitor == null
                || visitor.getStatus() != VisitorStatus.PENDING
                || !LocalDate.now().equals(visitor.getVisitDate())
                || expired(visitor)) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, INVALID_TOKEN_MESSAGE);
        }

        LocalDateTime now = LocalDateTime.now();
        AdminVisitor patch = new AdminVisitor();
        // 条件更新已保证并发安全，version 置空避免叠加乐观锁导致条件更新失效
        patch.setVersion(null);
        patch.setStatus(VisitorStatus.CHECKED_IN);
        patch.setCheckinTime(now);
        int rows =
                visitorMapper.update(
                        patch,
                        new LambdaUpdateWrapper<AdminVisitor>()
                                .eq(AdminVisitor::getId, visitor.getId())
                                .eq(AdminVisitor::getStatus, VisitorStatus.PENDING)
                                .eq(AdminVisitor::getQrToken, trimmed)
                                // token 一次性：核销成功即清空（实体 patch 无法写入 null，改用 setSql）
                                .setSql("qr_token = NULL"));
        if (rows == 0) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, INVALID_TOKEN_MESSAGE);
        }
        log.info("访客 {} 核销入场", visitor.getId());
    }

    @Override
    @Transactional
    public void checkout(Long id) {
        AdminVisitor visitor = requireVisitor(id);
        requireHostOrAdmin(visitor, "仅被访人或管理员可登记离场");
        if (visitor.getStatus() != VisitorStatus.CHECKED_IN) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅已入场的访客可登记离场");
        }

        AdminVisitor patch = new AdminVisitor();
        patch.setVersion(null);
        patch.setStatus(VisitorStatus.CHECKED_OUT);
        patch.setCheckoutTime(LocalDateTime.now());
        int rows =
                visitorMapper.update(
                        patch,
                        new LambdaUpdateWrapper<AdminVisitor>()
                                .eq(AdminVisitor::getId, id)
                                .eq(AdminVisitor::getStatus, VisitorStatus.CHECKED_IN));
        if (rows == 0) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "访客状态已变更，请刷新后重试");
        }
        log.info("访客 {} 登记离场", id);
    }

    @Override
    @Transactional
    public void cancel(Long id) {
        AdminVisitor visitor = requireVisitor(id);
        requireHostOrAdmin(visitor, "仅被访人或管理员可取消访客登记");
        if (visitor.getStatus() != VisitorStatus.PENDING) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅待核销的访客登记可取消");
        }

        AdminVisitor patch = new AdminVisitor();
        patch.setVersion(null);
        patch.setStatus(VisitorStatus.CANCELLED);
        int rows =
                visitorMapper.update(
                        patch,
                        new LambdaUpdateWrapper<AdminVisitor>()
                                .eq(AdminVisitor::getId, id)
                                .eq(AdminVisitor::getStatus, VisitorStatus.PENDING));
        if (rows == 0) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "访客状态已变更，请刷新后重试");
        }
        log.info("访客 {} 登记已取消", id);
    }

    private AdminVisitor requireVisitor(Long id) {
        AdminVisitor visitor = id == null ? null : visitorMapper.selectById(id);
        if (visitor == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "访客登记不存在");
        }
        return visitor;
    }

    /** 被访人本人或管理员才可操作访客登记。 */
    private void requireHostOrAdmin(AdminVisitor visitor, String message) {
        Long currentUserId = SecurityUtils.getCurrentUserIdOrDefault();
        if (!Objects.equals(visitor.getHostUserId(), currentUserId) && !isAdmin()) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, message);
        }
    }

    /** 校验二维码是否超过登记时间 + 核销窗口。 */
    private boolean expired(AdminVisitor visitor) {
        LocalDateTime createTime = visitor.getCreateTime();
        if (createTime == null) {
            return false;
        }
        int windowHours = Math.max(adminProperties.getQrCheckinWindowHours(), 0);
        return LocalDateTime.now().isAfter(createTime.plusHours(windowHours));
    }

    private VisitorQrVO buildQrVO(AdminVisitor visitor) {
        VisitorQrVO vo = new VisitorQrVO();
        vo.setVisitorId(visitor.getId());
        vo.setToken(visitor.getQrToken());
        LocalDateTime base =
                visitor.getCreateTime() == null ? LocalDateTime.now() : visitor.getCreateTime();
        vo.setExpireAt(base.plusHours(Math.max(adminProperties.getQrCheckinWindowHours(), 0)));
        vo.setVisitDate(visitor.getVisitDate());
        vo.setVisitorName(visitor.getVisitorName());
        return vo;
    }

    /** 32 位无横线 UUID。 */
    private String newToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** 是否系统管理员（ROLE_SYS_ADMIN）；与行政其他服务保持一致的判定口径。 */
    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_SYS_ADMIN"::equals);
    }
}
