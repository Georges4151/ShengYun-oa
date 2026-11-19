package com.ceams.module.hr.support;

import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.support.BpmBizCallback;
import com.ceams.module.hr.entity.HrPerformanceReview;
import com.ceams.module.hr.enums.PerformanceReviewStatus;
import com.ceams.module.hr.mapper.HrPerformanceReviewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 绩效考评审批回调。
 *
 * <p>通过：考评单置 APPROVED（final_score / coefficient 由 HR 复核接口写入，回调不覆盖）；
 * 驳回 / 撤回 / 终止：考评单置 REJECTED，允许后续重新发起。
 *
 * <p>幂等：考评单已是 APPROVED 直接返回；HR 复核接口先落定状态再推进流程，
 * 因此正常路径下回调幂等命中，不会重复写库。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HrPerformanceBpmCallback implements BpmBizCallback {

    private final HrPerformanceReviewMapper reviewMapper;

    @Override
    public BpmBizType bizType() {
        return BpmBizType.HR_PERFORMANCE;
    }

    @Override
    public void onInstanceFinished(BpmInstance instance, BpmInstanceStatus status) {
        if (instance == null || instance.getBizId() == null) {
            return;
        }
        HrPerformanceReview review = reviewMapper.selectById(instance.getBizId());
        if (review == null) {
            log.debug("绩效考评单 {} 不存在，忽略流程终态 {}", instance.getBizId(), status);
            return;
        }
        if (review.getStatus() == PerformanceReviewStatus.APPROVED) {
            log.info("绩效考评单 {} 已是 APPROVED，跳过重复回调", review.getId());
            return;
        }
        if (status == BpmInstanceStatus.APPROVED) {
            review.setStatus(PerformanceReviewStatus.APPROVED);
            reviewMapper.updateById(review);
            log.info("绩效考评单 {} 审批通过，状态置 APPROVED", review.getId());
            return;
        }
        if (status == BpmInstanceStatus.REJECTED
                || status == BpmInstanceStatus.WITHDRAWN
                || status == BpmInstanceStatus.TERMINATED) {
            review.setStatus(PerformanceReviewStatus.REJECTED);
            reviewMapper.updateById(review);
            log.info("绩效考评单 {} 审批终态 {}，状态置 REJECTED", review.getId(), status);
            return;
        }
        log.debug("绩效考评单 {} 收到非终态 {}，忽略", review.getId(), status);
    }
}
