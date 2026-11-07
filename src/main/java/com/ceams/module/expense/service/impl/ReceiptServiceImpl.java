package com.ceams.module.expense.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.expense.converter.ApprovalRecordConverter;
import com.ceams.module.expense.converter.ReceiptConverter;
import com.ceams.module.expense.converter.ReceiptVerificationConverter;
import com.ceams.module.expense.dto.ApprovalActionRequest;
import com.ceams.module.expense.dto.ReceiptCreateRequest;
import com.ceams.module.expense.dto.ReceiptUpdateRequest;
import com.ceams.module.expense.dto.ReceiptVerifyRequest;
import com.ceams.module.expense.entity.ApprovalRecord;
import com.ceams.module.expense.entity.Receipt;
import com.ceams.module.expense.entity.ReceiptVerification;
import com.ceams.module.expense.enums.ApprovalRecordStatus;
import com.ceams.module.expense.enums.PaymentStatus;
import com.ceams.module.expense.enums.ReceiptApprovalStatus;
import com.ceams.module.expense.enums.VerificationStatus;
import com.ceams.module.expense.mapper.ApprovalRecordMapper;
import com.ceams.module.expense.mapper.ReceiptMapper;
import com.ceams.module.expense.mapper.ReceiptVerificationMapper;
import com.ceams.module.expense.service.ReceiptService;
import com.ceams.module.expense.util.PageUtil;
import com.ceams.module.expense.vo.ApprovalRecordVO;
import com.ceams.module.expense.vo.ReceiptVO;
import com.ceams.module.expense.vo.ReceiptVerificationVO;
import com.ceams.module.payment.service.PaymentAccountQueryService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 票据（报销）服务实现。
 *
 * <p>三级审批状态机：approval_status + current_approval_level 表达。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {

    /** 三级审批总级数。 */
    private static final int MAX_APPROVAL_LEVEL = 3;

    private final ReceiptMapper receiptMapper;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final ReceiptVerificationMapper receiptVerificationMapper;
    private final ReceiptConverter receiptConverter;
    private final ApprovalRecordConverter approvalRecordConverter;
    private final ReceiptVerificationConverter receiptVerificationConverter;
    private final PaymentAccountQueryService paymentAccountQueryService;

    @Override
    public PageResult<ReceiptVO> page(
            long page,
            long size,
            String receiptNumber,
            Long userId,
            Long projectId,
            VerificationStatus verificationStatus,
            PaymentStatus paymentStatus,
            ReceiptApprovalStatus approvalStatus) {
        Page<Receipt> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<Receipt> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(receiptNumber)) {
            wrapper.like(Receipt::getReceiptNumber, receiptNumber);
        }
        if (userId != null) {
            wrapper.eq(Receipt::getUserId, userId);
        }
        if (projectId != null) {
            wrapper.eq(Receipt::getProjectId, projectId);
        }
        if (verificationStatus != null) {
            wrapper.eq(Receipt::getVerificationStatus, verificationStatus);
        }
        if (paymentStatus != null) {
            wrapper.eq(Receipt::getPaymentStatus, paymentStatus);
        }
        if (approvalStatus != null) {
            wrapper.eq(Receipt::getApprovalStatus, approvalStatus);
        }
        wrapper.orderByDesc(Receipt::getCreateTime);
        IPage<Receipt> result = receiptMapper.selectPage(p, wrapper);
        return new PageResult<>(
                receiptConverter.toVOList(result.getRecords()),
                result.getTotal(),
                result.getCurrent(),
                result.getSize());
    }

    @Override
    public ReceiptVO detail(Long id) {
        return receiptConverter.toVO(requireReceipt(id));
    }

    @Override
    public List<ReceiptVO> findPotentialDuplicates(
            Long excludeId,
            String receiptNumber,
            BigDecimal amount,
            LocalDateTime dayStart,
            LocalDateTime dayEnd) {
        boolean byNumber = StringUtils.isNotBlank(receiptNumber);
        boolean byAmountDay = amount != null && dayStart != null && dayEnd != null;
        if (!byNumber && !byAmountDay) {
            return List.of();
        }
        LambdaQueryWrapper<Receipt> wrapper = new LambdaQueryWrapper<>();
        if (excludeId != null) {
            wrapper.ne(Receipt::getId, excludeId);
        }
        if (byNumber && byAmountDay) {
            wrapper.and(
                    w ->
                            w.eq(Receipt::getReceiptNumber, receiptNumber)
                                    .or(
                                            inner ->
                                                    inner.eq(Receipt::getAmount, amount)
                                                            .ge(Receipt::getReceiptDate, dayStart)
                                                            .le(Receipt::getReceiptDate, dayEnd)));
        } else if (byNumber) {
            wrapper.eq(Receipt::getReceiptNumber, receiptNumber);
        } else {
            wrapper.eq(Receipt::getAmount, amount)
                    .ge(Receipt::getReceiptDate, dayStart)
                    .le(Receipt::getReceiptDate, dayEnd);
        }
        wrapper.orderByDesc(Receipt::getCreateTime).last("LIMIT 5");
        return receiptConverter.toVOList(receiptMapper.selectList(wrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(ReceiptCreateRequest request) {
        if (receiptNumberExists(request.getReceiptNumber())) {
            throw BusinessException.of(ErrorCode.BIZ_DUPLICATE_CODE, "票据编号已存在");
        }
        Receipt receipt = new Receipt();
        receipt.setReceiptNumber(request.getReceiptNumber());
        receipt.setUserId(
                request.getUserId() == null
                        ? SecurityUtils.getCurrentUserId()
                        : request.getUserId());
        receipt.setReporterId(request.getReporterId());
        receipt.setProjectId(request.getProjectId());
        receipt.setReporterName(request.getReporterName());
        receipt.setReceiptDate(request.getReceiptDate());
        receipt.setAmount(request.getAmount());
        receipt.setType(request.getType());
        receipt.setDescription(request.getDescription());
        receipt.setFilePath(request.getFilePath());
        receipt.setFileHash(request.getFileHash());
        receipt.setOcrData(request.getOcrData());
        receipt.setPaymentMethod(request.getPaymentMethod());
        receipt.setVerificationStatus(VerificationStatus.PENDING);
        receipt.setPaymentStatus(PaymentStatus.UNPAID);
        receipt.setPaidAmount(BigDecimal.ZERO);
        receipt.setCurrentApprovalLevel(0);
        receipt.setApprovalStatus(ReceiptApprovalStatus.DRAFT);
        receiptMapper.insert(receipt);
        return String.valueOf(receipt.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ReceiptUpdateRequest request) {
        Receipt receipt = requireReceipt(id);
        requireEditable(receipt);
        if (request.getReporterId() != null) {
            receipt.setReporterId(request.getReporterId());
        }
        if (request.getProjectId() != null) {
            receipt.setProjectId(request.getProjectId());
        }
        if (request.getReporterName() != null) {
            receipt.setReporterName(request.getReporterName());
        }
        if (request.getReceiptDate() != null) {
            receipt.setReceiptDate(request.getReceiptDate());
        }
        if (request.getAmount() != null) {
            receipt.setAmount(request.getAmount());
        }
        if (request.getType() != null) {
            receipt.setType(request.getType());
        }
        if (request.getDescription() != null) {
            receipt.setDescription(request.getDescription());
        }
        if (request.getFilePath() != null) {
            receipt.setFilePath(request.getFilePath());
        }
        if (request.getFileHash() != null) {
            receipt.setFileHash(request.getFileHash());
        }
        if (request.getOcrData() != null) {
            receipt.setOcrData(request.getOcrData());
        }
        if (request.getPaymentMethod() != null) {
            receipt.setPaymentMethod(request.getPaymentMethod());
        }
        receiptMapper.updateById(receipt);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Receipt receipt = requireReceipt(id);
        requireEditable(receipt);
        receiptMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long id) {
        Receipt receipt = requireReceipt(id);
        ReceiptApprovalStatus status = receipt.getApprovalStatus();
        if (status != ReceiptApprovalStatus.DRAFT && status != ReceiptApprovalStatus.REJECTED) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅草稿态或驳回态的票据可上报");
        }
        receipt.setApprovalStatus(ReceiptApprovalStatus.PENDING);
        receipt.setCurrentApprovalLevel(1);
        receiptMapper.updateById(receipt);

        ApprovalRecord record = new ApprovalRecord();
        record.setReceiptId(receipt.getId());
        record.setApprovalLevel(1);
        record.setStatus(ApprovalRecordStatus.PENDING);
        approvalRecordMapper.insert(record);
        log.info("票据 {} 已上报，进入三级审批第 1 级", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, ApprovalActionRequest request) {
        Receipt receipt = requireReceipt(id);
        if (receipt.getApprovalStatus() != ReceiptApprovalStatus.PENDING) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅待审批状态的票据可审批");
        }
        int level = receipt.getCurrentApprovalLevel();
        if (level < 1 || level > MAX_APPROVAL_LEVEL) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "当前审批级别非法");
        }
        ApprovalRecord record = requirePendingApprovalRecord(receipt.getId(), level);
        record.setStatus(ApprovalRecordStatus.APPROVED);
        record.setApprovalTime(LocalDateTime.now());
        record.setApprovalComment(request == null ? null : request.getComment());
        record.setApproverId(SecurityUtils.getCurrentUserId());
        approvalRecordMapper.updateById(record);

        if (level < MAX_APPROVAL_LEVEL) {
            receipt.setCurrentApprovalLevel(level + 1);
            receiptMapper.updateById(receipt);
            ApprovalRecord next = new ApprovalRecord();
            next.setReceiptId(receipt.getId());
            next.setApprovalLevel(level + 1);
            next.setStatus(ApprovalRecordStatus.PENDING);
            approvalRecordMapper.insert(next);
        } else {
            receipt.setApprovalStatus(ReceiptApprovalStatus.APPROVED);
            receiptMapper.updateById(receipt);
        }
        log.info("票据 {} 第 {} 级审批通过", id, level);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, ApprovalActionRequest request) {
        Receipt receipt = requireReceipt(id);
        if (receipt.getApprovalStatus() != ReceiptApprovalStatus.PENDING) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅待审批状态的票据可驳回");
        }
        int level = receipt.getCurrentApprovalLevel();
        ApprovalRecord record = requirePendingApprovalRecord(receipt.getId(), level);
        record.setStatus(ApprovalRecordStatus.REJECTED);
        record.setApprovalTime(LocalDateTime.now());
        record.setApprovalComment(request == null ? null : request.getComment());
        record.setApproverId(SecurityUtils.getCurrentUserId());
        approvalRecordMapper.updateById(record);

        receipt.setApprovalStatus(ReceiptApprovalStatus.REJECTED);
        receipt.setCurrentApprovalLevel(0);
        receiptMapper.updateById(receipt);
        log.info("票据 {} 第 {} 级审批驳回", id, level);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void verify(Long id, ReceiptVerifyRequest request) {
        Receipt receipt = requireReceipt(id);
        if (receipt.getApprovalStatus() != ReceiptApprovalStatus.APPROVED) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅审批通过的票据可进行财务核验");
        }
        if (!paymentAccountQueryService.exists(request.getPaymentAccountId())) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "付款账户不存在");
        }
        if (request.getAmount().compareTo(receipt.getAmount()) > 0) {
            throw BusinessException.of(ErrorCode.BIZ_UNPROCESSABLE, "核验金额不能超过票据金额");
        }
        ReceiptVerification verification = new ReceiptVerification();
        verification.setReceiptId(receipt.getId());
        verification.setPaymentAccountId(request.getPaymentAccountId());
        verification.setAmount(request.getAmount());
        verification.setPaymentMethod(request.getPaymentMethod());
        verification.setVerificationTime(LocalDateTime.now());
        verification.setVerifiedBy(SecurityUtils.getCurrentUserId());
        verification.setRemark(request.getRemark());
        receiptVerificationMapper.insert(verification);

        receipt.setVerificationStatus(VerificationStatus.VERIFIED);
        receipt.setVerifiedBy(SecurityUtils.getCurrentUserId());
        receipt.setVerifiedAt(LocalDateTime.now());
        receiptMapper.updateById(receipt);
        log.info("票据 {} 财务核验通过，核验金额 {}", id, request.getAmount());
    }

    @Override
    public List<ApprovalRecordVO> listApprovals(Long id) {
        requireReceipt(id);
        LambdaQueryWrapper<ApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalRecord::getReceiptId, id).orderByAsc(ApprovalRecord::getApprovalLevel);
        return approvalRecordConverter.toVOList(approvalRecordMapper.selectList(wrapper));
    }

    @Override
    public List<ReceiptVerificationVO> listVerifications(Long id) {
        requireReceipt(id);
        LambdaQueryWrapper<ReceiptVerification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReceiptVerification::getReceiptId, id)
                .orderByDesc(ReceiptVerification::getVerificationTime);
        return receiptVerificationConverter.toVOList(receiptVerificationMapper.selectList(wrapper));
    }

    @Override
    public ExpenseSummary summary(
            LocalDateTime start, LocalDateTime end, Long userId, String keyword) {
        LambdaQueryWrapper<Receipt> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(Receipt::getReceiptDate, start)
                .le(Receipt::getReceiptDate, end)
                .eq(userId != null, Receipt::getUserId, userId);
        applyKeyword(wrapper, keyword);
        List<Receipt> receipts = receiptMapper.selectList(wrapper);

        BigDecimal totalAmount = BigDecimal.ZERO;
        Map<String, Long> approvalStatusDistribution = new LinkedHashMap<>();
        Map<String, Long> paymentStatusDistribution = new LinkedHashMap<>();
        Map<String, Long> verificationStatusDistribution = new LinkedHashMap<>();
        Map<String, Long> typeDistribution = new LinkedHashMap<>();
        for (Receipt receipt : receipts) {
            totalAmount = totalAmount.add(nvl(receipt.getAmount()));
            merge(approvalStatusDistribution, receipt.getApprovalStatus());
            merge(paymentStatusDistribution, receipt.getPaymentStatus());
            merge(verificationStatusDistribution, receipt.getVerificationStatus());
            merge(typeDistribution, receipt.getType());
        }
        return new ExpenseSummary(
                totalAmount,
                receipts.size(),
                approvalStatusDistribution,
                paymentStatusDistribution,
                verificationStatusDistribution,
                typeDistribution);
    }

    @Override
    public PendingApprovalPage pendingApprovals(
            Long userId, String keyword, Long projectId, int limit) {
        LambdaQueryWrapper<Receipt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Receipt::getApprovalStatus, ReceiptApprovalStatus.PENDING)
                .eq(userId != null, Receipt::getUserId, userId)
                .eq(projectId != null, Receipt::getProjectId, projectId);
        applyKeyword(wrapper, keyword);
        wrapper.orderByDesc(Receipt::getCreateTime);
        List<Receipt> receipts = receiptMapper.selectList(wrapper);
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Receipt receipt : receipts) {
            totalAmount = totalAmount.add(nvl(receipt.getAmount()));
        }
        int safeLimit = Math.max(1, limit);
        List<Receipt> limited =
                receipts.size() <= safeLimit ? receipts : receipts.subList(0, safeLimit);
        return new PendingApprovalPage(
                receipts.size(), totalAmount, receiptConverter.toVOList(limited));
    }

    private void applyKeyword(LambdaQueryWrapper<Receipt> wrapper, String keyword) {
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(
                    w ->
                            w.like(Receipt::getReceiptNumber, keyword)
                                    .or()
                                    .like(Receipt::getDescription, keyword));
        }
    }

    private static void merge(Map<String, Long> distribution, Enum<?> value) {
        String key = value == null ? "UNKNOWN" : value.name();
        distribution.merge(key, 1L, Long::sum);
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Receipt requireReceipt(Long id) {
        Receipt receipt = receiptMapper.selectById(id);
        if (receipt == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "票据不存在");
        }
        return receipt;
    }

    private void requireEditable(Receipt receipt) {
        ReceiptApprovalStatus status = receipt.getApprovalStatus();
        if (status != ReceiptApprovalStatus.DRAFT && status != ReceiptApprovalStatus.REJECTED) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅草稿态或驳回态的票据可编辑");
        }
    }

    private boolean receiptNumberExists(String receiptNumber) {
        LambdaQueryWrapper<Receipt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Receipt::getReceiptNumber, receiptNumber);
        return receiptMapper.selectCount(wrapper) > 0;
    }

    private ApprovalRecord requirePendingApprovalRecord(Long receiptId, int level) {
        LambdaQueryWrapper<ApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalRecord::getReceiptId, receiptId)
                .eq(ApprovalRecord::getApprovalLevel, level)
                .eq(ApprovalRecord::getStatus, ApprovalRecordStatus.PENDING);
        List<ApprovalRecord> records = approvalRecordMapper.selectList(wrapper);
        if (records.isEmpty()) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "当前审批级别不存在待审批记录或已越级");
        }
        return records.get(0);
    }
}
