package com.ceams.module.admin.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.admin.dto.VisitorCheckinRequest;
import com.ceams.module.admin.dto.VisitorCreateRequest;
import com.ceams.module.admin.enums.VisitorStatus;
import com.ceams.module.admin.vo.VisitorQrVO;
import com.ceams.module.admin.vo.VisitorVO;
import java.time.LocalDate;

/** 行政访客服务。 */
public interface AdminVisitorService {

    /** 访客登记并签发一次性二维码，返回二维码内容。 */
    VisitorQrVO create(VisitorCreateRequest request);

    /** 访客分页查询（按日期 / 状态过滤）。 */
    PageResult<VisitorVO> list(LocalDate date, VisitorStatus status, long page, long size);

    /** 重新签发二维码（仅当天待核销的访客，被访人或管理员）。 */
    VisitorQrVO qr(Long id);

    /** 按 token 核销入场（一次性，重复核销报状态冲突）。 */
    void checkin(VisitorCheckinRequest request);

    /** 离场登记（仅 CHECKED_IN，被访人或管理员）。 */
    void checkout(Long id);

    /** 取消登记（仅 PENDING，被访人或管理员）。 */
    void cancel(Long id);
}
