package com.ceams.module.doc.service;

import com.ceams.module.doc.dto.PermissionGrantRequest;
import com.ceams.module.doc.vo.PermissionVO;
import java.util.List;

/** 文档授权服务（P2 仅文件级授权）。 */
public interface DocPermissionService {

    /** 文件授权列表。 */
    List<PermissionVO> list(Long fileId);

    /** 新增或覆盖授权（同一主体唯一），返回授权记录 ID。 */
    String grant(Long fileId, PermissionGrantRequest request);

    /** 取消授权。 */
    void revoke(Long fileId, Long permId);
}
