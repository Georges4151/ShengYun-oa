package com.ceams.module.doc.service;

import com.ceams.module.doc.vo.DocFileVO;
import com.ceams.module.doc.vo.DocVersionVO;
import java.util.List;

/** 文档版本服务（列表与回滚）。 */
public interface DocVersionService {

    /** 版本列表（versionNo 倒序）。 */
    List<DocVersionVO> list(Long fileId);

    /**
     * 回滚到指定版本：复制目标版本内容生成新版本（不删除历史）。
     *
     * @param fileId 文件 ID
     * @param versionNo 目标版本号
     * @return 更新后的文件元数据
     */
    DocFileVO rollback(Long fileId, int versionNo);
}
