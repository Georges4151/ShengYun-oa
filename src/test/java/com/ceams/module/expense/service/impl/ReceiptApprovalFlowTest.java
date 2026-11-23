package com.ceams.module.expense.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.expense.converter.ApprovalRecordConverter;
import com.ceams.module.expense.converter.ReceiptConverter;
import com.ceams.module.expense.converter.ReceiptVerificationConverter;
import com.ceams.module.expense.entity.Receipt;
import com.ceams.module.expense.enums.PaymentStatus;
import com.ceams.module.expense.enums.ReceiptApprovalStatus;
import com.ceams.module.expense.enums.ReceiptType;
import com.ceams.module.expense.enums.VerificationStatus;
import com.ceams.module.expense.mapper.ApprovalRecordMapper;
import com.ceams.module.expense.mapper.ReceiptMapper;
import com.ceams.module.expense.mapper.ReceiptVerificationMapper;
import com.ceams.module.expense.service.ExpenseAmountRuleService;
import com.ceams.module.expense.support.ExpenseBpmCallback;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * 报销审批状态流转链路测试。
 *
 * <p>调用路径：提交由 {@link ReceiptServiceImpl#submit} 发起 BPM 实例；审批通过 / 驳回由
 * {@link ExpenseBpmCallback#onInstanceFinished} 依据实例终态写回 approval_status；财务核验节点由
 * {@link ExpenseBpmCallback#onNodeCompleted} 写回 verification_status。
 *
 * <p>生产实体无「提交时间 / 提交人 / 驳回原因」字段，服务层也未写入这些值；相关说明见各用例注释。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReceiptApprovalFlowTest {

    private static final Long RECEIPT_ID = 7001L;

    private static final Long BPM_INSTANCE_ID = 9001L;

    private static final Long PROJECT_ID = 42L;

    private static final Long USER_ID = 15L;

    @Mock private ReceiptMapper receiptMapper;
    @Mock private ApprovalRecordMapper approvalRecordMapper;
    @Mock private ReceiptVerificationMapper receiptVerificationMapper;
    @Mock private ReceiptConverter receiptConverter;
    @Mock private ApprovalRecordConverter approvalRecordConverter;
    @Mock private ReceiptVerificationConverter receiptVerificationConverter;
    @Mock private BpmInstanceService bpmInstanceService;
    @Mock private ExpenseAmountRuleService amountRuleService;

    private ReceiptServiceImpl receiptService;
    private ExpenseBpmCallback expenseBpmCallback;

    @BeforeEach
    void setUp() {
        receiptService =
                new ReceiptServiceImpl(
                        receiptMapper,
                        approvalRecordMapper,
                        receiptVerificationMapper,
                        receiptConverter,
                        approvalRecordConverter,
                        receiptVerificationConverter,
                        bpmInstanceService,
                        amountRuleService);
        expenseBpmCallback = new ExpenseBpmCallback(receiptMapper);
        // 默认命中兜底规则：额度规则匹配逻辑由 ExpenseAmountRuleMatcherTest 覆盖
        when(amountRuleService.resolveTemplateCode(any(), any(), any())).thenReturn("EXPENSE");
    }

    /** 草稿提交：发起 EXPENSE 流程，状态 DRAFT -> PENDING，并回写流程实例 ID。 */
    @Test
    void submitShouldMoveDraftToPendingAndStartExpenseInstance() {
        Receipt receipt = receipt(ReceiptApprovalStatus.DRAFT);
        when(receiptMapper.selectByIdForUpdate(RECEIPT_ID)).thenReturn(receipt);
        when(bpmInstanceService.start(any(InstanceStartRequest.class)))
                .thenReturn(String.valueOf(BPM_INSTANCE_ID));

        receiptService.submit(RECEIPT_ID);

        ArgumentCaptor<InstanceStartRequest> requestCaptor =
                ArgumentCaptor.forClass(InstanceStartRequest.class);
        verify(bpmInstanceService).start(requestCaptor.capture());
        InstanceStartRequest started = requestCaptor.getValue();
        assertThat(started.getTemplateCode()).isEqualTo("EXPENSE");
        assertThat(started.getBizType()).isEqualTo("EXPENSE");
        assertThat(started.getBizId()).isEqualTo(RECEIPT_ID);
        assertThat(started.getVariables())
                .containsEntry("amount", "120.50")
                .containsEntry("receiptType", "MEAL")
                .containsEntry("projectId", String.valueOf(PROJECT_ID));

        ArgumentCaptor<Receipt> receiptCaptor = ArgumentCaptor.forClass(Receipt.class);
        verify(receiptMapper).updateById(receiptCaptor.capture());
        Receipt saved = receiptCaptor.getValue();
        assertThat(saved.getApprovalStatus()).isEqualTo(ReceiptApprovalStatus.PENDING);
        assertThat(saved.getBpmInstanceId()).isEqualTo(BPM_INSTANCE_ID);
        // 提交时间/提交人不在 Receipt 字段中，由 MyBatis-Plus 审计字段在持久层填充，服务层不赋值
        assertThat(saved.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    }

    /** 审批通过：PENDING -> APPROVED，仅回写审批状态，不自动变更财务核验状态。 */
    @Test
    void approvedInstanceShouldMovePendingToApproved() {
        Receipt receipt = receipt(ReceiptApprovalStatus.PENDING);
        receipt.setCurrentApprovalLevel(2);
        when(receiptMapper.selectById(RECEIPT_ID)).thenReturn(receipt);

        expenseBpmCallback.onInstanceFinished(instance(RECEIPT_ID), BpmInstanceStatus.APPROVED);

        ArgumentCaptor<Receipt> receiptCaptor = ArgumentCaptor.forClass(Receipt.class);
        verify(receiptMapper).updateById(receiptCaptor.capture());
        Receipt saved = receiptCaptor.getValue();
        assertThat(saved.getApprovalStatus()).isEqualTo(ReceiptApprovalStatus.APPROVED);
        assertThat(saved.getVerificationStatus()).isEqualTo(VerificationStatus.PENDING);
        assertThat(saved.getCurrentApprovalLevel()).isEqualTo(2);
        // 回调只依赖 ReceiptMapper，除状态回写外没有通知等其他协作者调用
        verify(receiptMapper, times(1)).updateById(any(Receipt.class));
    }

    /** 重复的 APPROVED 终态回调应幂等：只写库一次。 */
    @Test
    void duplicateApprovedCallbackShouldWriteOnce() {
        Receipt receipt = receipt(ReceiptApprovalStatus.PENDING);
        when(receiptMapper.selectById(RECEIPT_ID)).thenReturn(receipt);

        expenseBpmCallback.onInstanceFinished(instance(RECEIPT_ID), BpmInstanceStatus.APPROVED);
        expenseBpmCallback.onInstanceFinished(instance(RECEIPT_ID), BpmInstanceStatus.APPROVED);

        verify(receiptMapper, times(1)).updateById(any(Receipt.class));
        assertThat(receipt.getApprovalStatus()).isEqualTo(ReceiptApprovalStatus.APPROVED);
    }

    /** 终态之后迟到的 REJECTED 回调不得覆盖 APPROVED，也不产生第二次写库。 */
    @Test
    void lateRejectedCallbackShouldNotOverrideApproved() {
        Receipt receipt = receipt(ReceiptApprovalStatus.PENDING);
        when(receiptMapper.selectById(RECEIPT_ID)).thenReturn(receipt);

        expenseBpmCallback.onInstanceFinished(instance(RECEIPT_ID), BpmInstanceStatus.APPROVED);
        expenseBpmCallback.onInstanceFinished(instance(RECEIPT_ID), BpmInstanceStatus.REJECTED);

        assertThat(receipt.getApprovalStatus()).isEqualTo(ReceiptApprovalStatus.APPROVED);
        verify(receiptMapper, times(1)).updateById(any(Receipt.class));
    }

    /** 审批驳回：PENDING -> REJECTED，并清空审批层级快照。 */
    @Test
    void rejectedInstanceShouldMovePendingToRejectedAndResetApprovalLevel() {
        Receipt receipt = receipt(ReceiptApprovalStatus.PENDING);
        receipt.setCurrentApprovalLevel(2);
        when(receiptMapper.selectById(RECEIPT_ID)).thenReturn(receipt);

        expenseBpmCallback.onInstanceFinished(instance(RECEIPT_ID), BpmInstanceStatus.REJECTED);

        ArgumentCaptor<Receipt> receiptCaptor = ArgumentCaptor.forClass(Receipt.class);
        verify(receiptMapper).updateById(receiptCaptor.capture());
        Receipt saved = receiptCaptor.getValue();
        assertThat(saved.getApprovalStatus()).isEqualTo(ReceiptApprovalStatus.REJECTED);
        assertThat(saved.getCurrentApprovalLevel()).isZero();
        // Receipt 实体没有驳回原因字段，回调也不落审批意见；驳回原因保存在 BPM 审批记录侧
        assertThat(saved.getBpmInstanceId()).isEqualTo(BPM_INSTANCE_ID);
    }

    /** 财务核验节点完成：核验状态写为 VERIFIED，并记录核验时间。 */
    @Test
    void financeVerifyNodeShouldMarkReceiptVerified() {
        Receipt receipt = receipt(ReceiptApprovalStatus.PENDING);
        when(receiptMapper.selectById(RECEIPT_ID)).thenReturn(receipt);

        expenseBpmCallback.onNodeCompleted(
                instance(RECEIPT_ID), ExpenseBpmCallback.NODE_FINANCE_VERIFY);

        ArgumentCaptor<Receipt> receiptCaptor = ArgumentCaptor.forClass(Receipt.class);
        verify(receiptMapper).updateById(receiptCaptor.capture());
        Receipt saved = receiptCaptor.getValue();
        assertThat(saved.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(saved.getVerifiedAt()).isNotNull();
    }

    /** 非法流转：已通过的报销单不允许再次提交，抛 BIZ_STATE_CONFLICT 且不发起流程。 */
    @Test
    void submitShouldRejectWhenReceiptAlreadyApproved() {
        when(receiptMapper.selectByIdForUpdate(RECEIPT_ID))
                .thenReturn(receipt(ReceiptApprovalStatus.APPROVED));

        assertThatThrownBy(() -> receiptService.submit(RECEIPT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅草稿态或驳回态的票据可上报")
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
        verify(bpmInstanceService, never()).start(any(InstanceStartRequest.class));
        verify(receiptMapper, never()).updateById(any(Receipt.class));
    }

    /** 无额度规则命中：拒绝提交，不发起流程、不写库。 */
    @Test
    void submitShouldRejectWhenNoAmountRuleMatched() {
        when(receiptMapper.selectByIdForUpdate(RECEIPT_ID))
                .thenReturn(receipt(ReceiptApprovalStatus.DRAFT));
        when(amountRuleService.resolveTemplateCode(any(), any(), any()))
                .thenThrow(
                        BusinessException.of(
                                ErrorCode.BIZ_UNPROCESSABLE,
                                "当前报销金额未匹配到审批流程规则，请联系管理员配置额度分级规则"));

        assertThatThrownBy(() -> receiptService.submit(RECEIPT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未匹配到审批流程规则")
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_UNPROCESSABLE);
        verify(bpmInstanceService, never()).start(any(InstanceStartRequest.class));
        verify(receiptMapper, never()).updateById(any(Receipt.class));
    }

    /** 非法流转：已终止（CANCELLED）的报销单不可再次提交。 */
    @Test
    void submitShouldRejectWhenReceiptCancelled() {
        when(receiptMapper.selectByIdForUpdate(RECEIPT_ID))
                .thenReturn(receipt(ReceiptApprovalStatus.CANCELLED));

        assertThatThrownBy(() -> receiptService.submit(RECEIPT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
        verify(bpmInstanceService, never()).start(any(InstanceStartRequest.class));
        verify(receiptMapper, never()).updateById(any(Receipt.class));
    }

    /** 重复提交同一审批请求：第二次被拒绝，不产生第二个流程实例或重复状态回写。 */
    @Test
    void duplicateSubmitShouldBeRejectedWithoutSideEffects() {
        when(receiptMapper.selectByIdForUpdate(RECEIPT_ID))
                .thenReturn(receipt(ReceiptApprovalStatus.PENDING));

        assertThatThrownBy(() -> receiptService.submit(RECEIPT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
        verify(bpmInstanceService, never()).start(any(InstanceStartRequest.class));
        verify(receiptMapper, never()).updateById(any(Receipt.class));
    }

    /** 票据不存在（含已逻辑删除）：抛 BIZ_NOT_FOUND，且不发起流程、不产生写库。 */
    @Test
    void submitShouldThrowNotFoundWhenReceiptMissing() {
        when(receiptMapper.selectByIdForUpdate(RECEIPT_ID)).thenReturn(null);

        assertThatThrownBy(() -> receiptService.submit(RECEIPT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("票据不存在")
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_NOT_FOUND);
        verify(bpmInstanceService, never()).start(any(InstanceStartRequest.class));
        verify(receiptMapper, never()).updateById(any(Receipt.class));
    }

    /** 财务核验节点重复回调应幂等：已 VERIFIED 时不重复更新。 */
    @Test
    void financeVerifyCallbackShouldBeIdempotent() {
        Receipt receipt = receipt(ReceiptApprovalStatus.PENDING);
        when(receiptMapper.selectById(RECEIPT_ID)).thenReturn(receipt);

        expenseBpmCallback.onNodeCompleted(
                instance(RECEIPT_ID), ExpenseBpmCallback.NODE_FINANCE_VERIFY);
        expenseBpmCallback.onNodeCompleted(
                instance(RECEIPT_ID), ExpenseBpmCallback.NODE_FINANCE_VERIFY);

        verify(receiptMapper, times(1)).updateById(any(Receipt.class));
        assertThat(receipt.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
    }

    /** 非终态回调（如 RUNNING）不派生审批状态。 */
    @Test
    void nonTerminalInstanceStatusShouldNotChangeApprovalStatus() {
        when(receiptMapper.selectById(RECEIPT_ID))
                .thenReturn(receipt(ReceiptApprovalStatus.PENDING));

        expenseBpmCallback.onInstanceFinished(instance(RECEIPT_ID), BpmInstanceStatus.RUNNING);

        verify(receiptMapper, never()).updateById(any(Receipt.class));
    }

    private static Receipt receipt(ReceiptApprovalStatus status) {
        Receipt receipt = new Receipt();
        receipt.setId(RECEIPT_ID);
        receipt.setReceiptNumber("BX-2026-0001");
        receipt.setUserId(USER_ID);
        receipt.setProjectId(PROJECT_ID);
        receipt.setReceiptDate(LocalDateTime.of(2026, 11, 1, 9, 0));
        receipt.setAmount(new BigDecimal("120.50"));
        receipt.setType(ReceiptType.MEAL);
        receipt.setDescription("差旅餐费");
        receipt.setVerificationStatus(VerificationStatus.PENDING);
        receipt.setPaymentStatus(PaymentStatus.UNPAID);
        receipt.setPaidAmount(BigDecimal.ZERO);
        receipt.setCurrentApprovalLevel(0);
        receipt.setApprovalStatus(status);
        receipt.setBpmInstanceId(BPM_INSTANCE_ID);
        return receipt;
    }

    private static BpmInstance instance(Long bizId) {
        BpmInstance instance = new BpmInstance();
        instance.setId(BPM_INSTANCE_ID);
        instance.setBizId(bizId);
        instance.setStatus(BpmInstanceStatus.RUNNING);
        return instance;
    }
}
