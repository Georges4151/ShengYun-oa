package com.ceams.module.hr.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.support.BpmBizCallback;
import com.ceams.module.hr.entity.HrCheckin;
import com.ceams.module.hr.entity.HrCheckinSupplement;
import com.ceams.module.hr.enums.CheckinSource;
import com.ceams.module.hr.enums.CheckinStatus;
import com.ceams.module.hr.enums.HrApprovalStatus;
import com.ceams.module.hr.mapper.HrCheckinMapper;
import com.ceams.module.hr.mapper.HrCheckinSupplementMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 补卡审批回调：通过后补一条 MANUAL 来源、NORMAL 状态的打卡记录。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HrCheckinSupplementBpmCallback implements BpmBizCallback {

    private final HrCheckinSupplementMapper supplementMapper;
    private final HrCheckinMapper checkinMapper;

    @Override
    public BpmBizType bizType() {
        return BpmBizType.HR_CHECKIN_SUPPLEMENT;
    }

    @Override
    public void onInstanceFinished(BpmInstance instance, BpmInstanceStatus status) {
        HrCheckinSupplement supplement = loadSupplement(instance);
        if (supplement == null) {
            return;
        }
        HrApprovalStatus target = HrBpmStatusResolver.resolve(status);
        if (target == null) {
            return;
        }
        if (target == HrApprovalStatus.APPROVED) {
            if (!checkinExists(supplement)) {
                HrCheckin checkin = new HrCheckin();
                checkin.setEmployeeId(supplement.getEmployeeId());
                checkin.setCheckinDate(supplement.getCheckinDate());
                checkin.setCheckinTime(supplement.getCheckinTime());
                checkin.setCheckinType(supplement.getCheckinType());
                checkin.setSource(CheckinSource.MANUAL);
                checkin.setStatus(CheckinStatus.NORMAL);
                checkin.setRemark("补卡审批通过，补卡单 " + supplement.getId());
                checkinMapper.insert(checkin);
                log.info(
                        "补卡单 {} 生效，已补录员工 {} {} 的 {} 打卡",
                        supplement.getId(),
                        supplement.getEmployeeId(),
                        supplement.getCheckinDate(),
                        supplement.getCheckinType());
            } else {
                log.warn("补卡单 {} 对应打卡记录已存在，跳过补录", supplement.getId());
            }
        }
        supplement.setApprovalStatus(target);
        supplementMapper.updateById(supplement);
        log.info("补卡申请 {} 审批状态派生为 {}", supplement.getId(), target);
    }

    /** 已存在同员工、同日期、同类型的打卡记录时不重复补录。 */
    private boolean checkinExists(HrCheckinSupplement supplement) {
        Long count =
                checkinMapper.selectCount(
                        new LambdaQueryWrapper<HrCheckin>()
                                .eq(HrCheckin::getEmployeeId, supplement.getEmployeeId())
                                .eq(HrCheckin::getCheckinDate, supplement.getCheckinDate())
                                .eq(HrCheckin::getCheckinType, supplement.getCheckinType()));
        return count != null && count > 0;
    }

    private HrCheckinSupplement loadSupplement(BpmInstance instance) {
        if (instance == null || instance.getBizId() == null) {
            return null;
        }
        return supplementMapper.selectById(instance.getBizId());
    }
}
