package com.ceams.module.doc.service;

import com.ceams.module.doc.dto.DirectoryCreateRequest;
import com.ceams.module.doc.dto.DirectoryMoveRequest;
import com.ceams.module.doc.dto.DirectoryUpdateRequest;
import com.ceams.module.doc.vo.DirectoryNodeVO;
import java.util.List;

/** 文档目录服务。 */
public interface DocDirectoryService {

    /**
     * 目录树（按数据权限过滤）。
     *
     * @param libraryType 库类型：PERSONAL/DEPT/PROJECT/PUBLIC
     * @param ownerId PERSONAL=用户 ID、DEPT=部门 ID、PROJECT=项目 ID，PUBLIC 忽略
     */
    List<DirectoryNodeVO> tree(String libraryType, Long ownerId);

    /** 新建目录，返回目录 ID。 */
    String create(DirectoryCreateRequest request);

    /** 改名 / 排序。 */
    void update(Long id, DirectoryUpdateRequest request);

    /** 移动（同库内，禁止移到自身或子树）。 */
    void move(Long id, DirectoryMoveRequest request);

    /** 删除（存在未删除子目录或文件时拒绝）。 */
    void delete(Long id);
}
