package com.ceams.module.hr.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.hr.entity.HrPerformanceReview;
import com.ceams.module.hr.enums.PerformanceReviewStatus;
import com.ceams.module.hr.mapper.HrPerformanceReviewMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 绩效审批回调单元测试：APPROVED 幂等、REJECTED 落状态。 */
@ExtendWith(MockitoExtension.class)
class HrPerformanceBpmCallbackTest {

    private static final Long REVIEW_ID = 8001L;

    @Mock private HrPerformanceReviewMapper reviewMapper;

    private HrPerformanceBpmCallback callback;

    @BeforeEach
    void setUp() {
        callback = new HrPerformanceBpmCallback(reviewMapper);
    }

    @Test
    void approvedShouldBeIdempotentWhenReviewAlreadyApproved() {
        HrPerformanceReview review = review(PerformanceReviewStatus.APPROVED);
        when(reviewMapper.selectById(REVIEW_ID)).thenReturn(review);

        callback.onInstanceFinished(instance(), BpmInstanceStatus.APPROVED);

        assertThat(review.getStatus()).isEqualTo(PerformanceReviewStatus.APPROVED);
        verify(reviewMapper, never()).updateById(any(HrPerformanceReview.class));
    }

    @Test
    void approvedShouldSetApprovedWhenStillLeaderDone() {
        HrPerformanceReview review = review(PerformanceReviewStatus.LEADER_DONE);
        when(reviewMapper.selectById(REVIEW_ID)).thenReturn(review);

        callback.onInstanceFinished(instance(), BpmInstanceStatus.APPROVED);

        assertThat(review.getStatus()).isEqualTo(PerformanceReviewStatus.APPROVED);
        verify(reviewMapper).updateById(review);
    }

    @Test
    void rejectedShouldSetReviewRejected() {
        HrPerformanceReview review = review(PerformanceReviewStatus.SELF_SUBMITTED);
        when(reviewMapper.selectById(REVIEW_ID)).thenReturn(review);

        callback.onInstanceFinished(instance(), BpmInstanceStatus.REJECTED);

        assertThat(review.getStatus()).isEqualTo(PerformanceReviewStatus.REJECTED);
        verify(reviewMapper).updateById(review);
    }

    @Test
    void withdrawnShouldSetReviewRejected() {
        HrPerformanceReview review = review(PerformanceReviewStatus.SELF_SUBMITTED);
        when(reviewMapper.selectById(REVIEW_ID)).thenReturn(review);

        callback.onInstanceFinished(instance(), BpmInstanceStatus.WITHDRAWN);

        assertThat(review.getStatus()).isEqualTo(PerformanceReviewStatus.REJECTED);
        verify(reviewMapper).updateById(review);
    }

    @Test
    void missingReviewShouldBeIgnored() {
        when(reviewMapper.selectById(REVIEW_ID)).thenReturn(null);

        callback.onInstanceFinished(instance(), BpmInstanceStatus.APPROVED);

        verify(reviewMapper, never()).updateById(any(HrPerformanceReview.class));
    }

    private HrPerformanceReview review(PerformanceReviewStatus status) {
        HrPerformanceReview review = new HrPerformanceReview();
        review.setId(REVIEW_ID);
        review.setEmployeeId(900L);
        review.setTemplateId(9000L);
        review.setPeriod("2026-11");
        review.setStatus(status);
        return review;
    }

    private BpmInstance instance() {
        BpmInstance instance = new BpmInstance();
        instance.setId(9101L);
        instance.setBizId(REVIEW_ID);
        return instance;
    }
}
