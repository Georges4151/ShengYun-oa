package com.ceams.module.doc.vo;

import com.ceams.module.doc.enums.LibraryType;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 目录树节点展示对象。 */
@Data
public class DirectoryNodeVO {

    /** 目录 ID。 */
    private Long id;

    /** 目录名。 */
    private String name;

    /** 父目录 ID（null 为库根）。 */
    private Long parentId;

    /** 库类型。 */
    private LibraryType libraryType;

    /** 目录路径（/id/id/）。 */
    private String path;

    /** 同级排序号。 */
    private Integer sortOrder;

    /** 子目录（按 sortOrder 升序）。 */
    private List<DirectoryNodeVO> children = new ArrayList<>();
}
