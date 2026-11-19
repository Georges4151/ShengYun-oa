package com.ceams.module.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.hr.converter.HrLeaveRequestConverter;
import com.ceams.module.hr.dto.LeaveRequestCreateRequest;
import com.ceams.module.hr.entity.HrLeaveRequest;
import com.ceams.module.hr.enums.HrApprovalStatus;
import com.ceams.module.hr.mapper.HrLeaveRequestMapper;
import com.ceams.module.hr.service.HrEmployeeService;
import com.ceams.module.hr.service.HrLeaveRequestService;
import com.ceams.module.hr.vo.LeaveRequestVO;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 请假申请服务实现。
 *
 * <p>提交后统一发起 {@code HR_LEAVE} 流程；额度扣减发生在审批通过时（见
 * {@code HrLeaveBpmCallback}），避免驳回后需要回滚额度。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrLeaveRequestServiceImpl implements HrLeaveRequestService {

    /** 请假流程模板编码。 */
    private static final String LEAVE_TEMPLATE = BpmBizType.HR_LEAVE.name();

    private final HrLeaveRequestMapper leaveRequestMapper;
    private final HrEmployeeService employeeService;
    private final HrLeaveRequestConverter leaveRequestConverter;
    private final BpmInstanceService bpmInstanceService;

    @Override
    @Transactional
    public String create(LeaveRequestCreateRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "结束日期不能早于开始日期");
        }
        Long employeeId = employeeService.currentEmployeeId();
        BigDecimal days = request.getDays() == null ? computeDays(request) : request.getDays();
        if (days.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "请假天数必须大于 0");
        }

        HrLeaveRequest leaveRequest = new HrLeaveRequest();
        leaveRequest.setEmployeeId(employeeId);
        leaveRequest.setLeaveType(request.getLeaveType());
        leaveRequest.setStartDate(request.getStartDate());
        leaveRequest.setEndDate(request.getEndDate());
        leaveRequest.setDays(days);
        leaveRequest.setReason(request.getReason());
        leaveRequest.setApprovalStatus(HrApprovalStatus.PENDING);
        leaveRequestMapper.insert(leaveRequest);

        InstanceStartRequest start = new InstanceStartRequest();
        start.setTemplateCode(LEAVE_TEMPLATE);
        start.setBizType(LEAVE_TEMPLATE);
        start.setBizId(leaveRequest.getId());
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("employeeId", String.valueOf(employeeId));
        variables.put("leaveType", request.getLeaveType().name());
        variables.put("days", days.stripTrailingZeros().toPlainString());
        variables.put("startDate", request.getStartDate().toString());
        variables.put("endDate", request.getEndDate().toString());
        start.setVariables(variables);
        String instanceId = bpmInstanceService.start(start);

        leaveRequest.setBpmInstanceId(parseInstanceId(instanceId));
        leaveRequestMapper.updateById(leaveRequest);
        log.info("员工 {} 提交请假申请 {}，天数 {}，BPM 实例 {}", employeeId, leaveRequest.getId(), days, instanceId);
        return String.valueOf(leaveRequest.getId());
    }

    @Override
    public List<LeaveRequestVO> my() {
        Long employeeId = employeeService.currentEmployeeId();
        List<HrLeaveRequest> requests =
                leaveRequestMapper.selectList(
                        new LambdaQueryWrapper<HrLeaveRequest>()
                                .eq(HrLeaveRequest::getEmployeeId, employeeId)
                                .orderByDesc(HrLeaveRequest::getCreateTime));
        return leaveRequestConverter.toVOList(requests);
    }

    /** 按起止日期（含首尾）计算天数。 */
    private BigDecimal computeDays(LeaveRequestCreateRequest request) {
        long between =
                ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        return BigDecimal.valueOf(between);
    }

    private Long parseInstanceId(String instanceId) {
        if (StringUtils.isBlank(instanceId)) {
            return null;
        }
        try {
            return Long.valueOf(instanceId.trim());
        } catch (NumberFormatException e) {
            log.warn("BPM 实例 ID 非数字，无法回写请假申请：{}", instanceId);
            return null;
        }
    }
}
