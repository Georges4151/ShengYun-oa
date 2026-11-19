package com.ceams.module.doc.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.doc.dto.FileMoveRequest;
import com.ceams.module.doc.dto.FileRenameRequest;
import com.ceams.module.doc.vo.DocFileVO;

/** 文档文件服务（P2 仅元数据：列表、重命名、移动、删除）。 */
public interface DocFileService {

    /** 文件分页列表（keyword 模糊匹配文件名）。 */
    PageResult<DocFileVO> list(Long dirId, String keyword, int page, int size);

    /** 重命名（校验非法字符与同目录重名）。 */
    void rename(Long id, FileRenameRequest request);

    /** 移动（目标目录需同库）。 */
    void move(Long id, FileMoveRequest request);

    /** 软删除文件并写审计。 */
    void delete(Long id);
}
