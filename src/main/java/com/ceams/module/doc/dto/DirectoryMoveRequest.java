package com.ceams.module.doc.dto;

import lombok.Data;

/** 目录移动请求（parentId 为空表示移动到库根）。 */
@Data
public class DirectoryMoveRequest {

    /** 目标父目录 ID。 */
    private Long parentId;
}
