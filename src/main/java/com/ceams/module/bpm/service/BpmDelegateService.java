package com.ceams.module.bpm.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.bpm.dto.DelegateCreateRequest;
import com.ceams.module.bpm.vo.DelegateVO;
import com.ceams.module.bpm.vo.UserOptionVO;
import java.util.List;

/** 用户自助代理服务：委托人设置代理时段，任务生成时自动转交代理人。 */
public interface BpmDelegateService {

    /** 我的代理记录（含已停用与已过期）。 */
    PageResult<DelegateVO> pageMine(long page, long size);

    /** 新建代理，返回代理记录 ID。 */
    Long create(DelegateCreateRequest request);

    /** 停用代理（仅委托人本人）。 */
    void cancel(Long id);

    /** 代理候选人（启用用户，支持姓名/账号关键字）。 */
    List<UserOptionVO> candidates(String keyword, int limit);
}
