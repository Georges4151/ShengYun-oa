package com.ceams.module.hr.support;

import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.support.BpmBizCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 薪资更正审批回调：分发表按 bizType 一一映射，更正流程复用调薪回调的台账派生逻辑。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HrSalaryCorrectionBpmCallback implements BpmBizCallback {

    private final HrSalaryBpmCallback delegate;

    @Override
    public BpmBizType bizType() {
        return BpmBizType.HR_SALARY_CORRECTION;
    }

    @Override
    public void onInstanceFinished(BpmInstance instance, BpmInstanceStatus status) {
        delegate.handle(instance, status);
    }
}
