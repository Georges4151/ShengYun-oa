package com.ceams.module.hr.service;

import com.ceams.module.hr.dto.LeaveRequestCreateRequest;
import com.ceams.module.hr.vo.LeaveRequestVO;
import java.util.List;

/** 请假申请服务。 */
public interface HrLeaveRequestService {

    /** 提交请假申请并发起审批，返回申请单 ID。 */
    String create(LeaveRequestCreateRequest request);

    /** 我的请假申请（按创建时间倒序）。 */
    List<LeaveRequestVO> my();
}
