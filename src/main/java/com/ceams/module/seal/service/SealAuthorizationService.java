package com.ceams.module.seal.service;

import com.ceams.module.seal.dto.SealAuthorizationCreateRequest;
import com.ceams.module.seal.vo.SealAuthorizationVO;
import java.util.List;

/** 印章授权服务。 */
public interface SealAuthorizationService {

    /** 新增授权，返回授权 ID。 */
    String create(SealAuthorizationCreateRequest request);

    /** 查询某枚印章的授权列表（按创建时间倒序）。 */
    List<SealAuthorizationVO> listBySeal(Long sealId);

    /** 撤销授权（逻辑删除）。 */
    void revoke(Long id);

    /** 用户对指定印章当前是否存在有效授权。 */
    boolean hasActiveAuthorization(Long sealId, Long userId);
}
