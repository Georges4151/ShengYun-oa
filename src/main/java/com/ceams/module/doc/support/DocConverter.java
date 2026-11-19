package com.ceams.module.doc.support;

import com.ceams.module.doc.entity.DocDirectory;
import com.ceams.module.doc.entity.DocFile;
import com.ceams.module.doc.entity.DocFilePermission;
import com.ceams.module.doc.entity.DocFileVersion;
import com.ceams.module.doc.enums.DocAction;
import com.ceams.module.doc.vo.DirectoryNodeVO;
import com.ceams.module.doc.vo.DocFileVO;
import com.ceams.module.doc.vo.DocVersionVO;
import com.ceams.module.doc.vo.PermissionVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * 文档模块实体 → VO / 授权动作 JSON 转换（静态方法，避免引入额外映射框架）。
 *
 * <p>{@code actions} 列在实体中以 JSON 原文保存，统一在此处用 Jackson 解析与序列化。
 */
@Slf4j
public final class DocConverter {

    /** JSON 解析器（仅用于授权动作数组）。 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 授权动作数组类型。 */
    private static final TypeReference<List<String>> ACTION_LIST_TYPE = new TypeReference<>() {};

    private DocConverter() {}

    /** 目录实体转树节点（children 由调用方组装）。 */
    public static DirectoryNodeVO toNode(DocDirectory dir) {
        if (dir == null) {
            return null;
        }
        DirectoryNodeVO vo = new DirectoryNodeVO();
        vo.setId(dir.getId());
        vo.setName(dir.getName());
        vo.setParentId(dir.getParentId());
        vo.setLibraryType(dir.getLibraryType());
        vo.setPath(dir.getPath());
        vo.setSortOrder(dir.getSortOrder());
        return vo;
    }

    /** 文件实体转展示对象。 */
    public static DocFileVO toFileVO(DocFile file) {
        if (file == null) {
            return null;
        }
        DocFileVO vo = new DocFileVO();
        vo.setId(file.getId());
        vo.setDirId(file.getDirId());
        vo.setName(file.getName());
        vo.setExt(file.getExt());
        vo.setSize(file.getSize());
        vo.setCurrentVersionNo(file.getCurrentVersionNo());
        vo.setStatus(file.getStatus());
        vo.setUploadedBy(file.getUploadedBy());
        vo.setCreateTime(file.getCreateTime());
        vo.setUpdateTime(file.getUpdateTime());
        vo.setDownloadCount(file.getDownloadCount());
        return vo;
    }

    /** 文件版本实体转展示对象。 */
    public static DocVersionVO toVersionVO(DocFileVersion version) {
        if (version == null) {
            return null;
        }
        DocVersionVO vo = new DocVersionVO();
        vo.setVersionNo(version.getVersionNo());
        vo.setSize(version.getSize());
        vo.setChecksum(version.getChecksum());
        vo.setUploadedBy(version.getUploadedBy());
        vo.setChangeNote(version.getChangeNote());
        vo.setSource(version.getSource());
        vo.setCreateTime(version.getCreateTime());
        return vo;
    }

    /** 文件级授权实体转展示对象。 */
    public static PermissionVO toPermissionVO(DocFilePermission permission) {
        if (permission == null) {
            return null;
        }
        PermissionVO vo = new PermissionVO();
        vo.setId(permission.getId());
        vo.setSubjectType(permission.getSubjectType());
        vo.setSubjectId(permission.getSubjectId());
        vo.setActions(new ArrayList<>(parseActionCodes(permission.getActions())));
        vo.setGrantedBy(permission.getGrantedBy());
        return vo;
    }

    /** 解析授权动作 JSON（非法或空值按空集处理，记 warn 日志）。 */
    public static EnumSet<DocAction> parseActions(String actionsJson) {
        EnumSet<DocAction> actions = EnumSet.noneOf(DocAction.class);
        for (String code : parseActionCodes(actionsJson)) {
            DocAction action = DocAction.from(code);
            if (action == null) {
                log.warn("未知的文档动作，已忽略：{}", code);
                continue;
            }
            actions.add(action);
        }
        return actions;
    }

    /** 授权动作序列化为 JSON 数组原文；空集合返回 "[]"。 */
    public static String writeActions(Collection<DocAction> actions) {
        Set<String> codes = new LinkedHashSet<>();
        if (actions != null) {
            for (DocAction action : actions) {
                if (action != null) {
                    codes.add(action.name());
                }
            }
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(codes);
        } catch (JsonProcessingException e) {
            // 动作枚举序列化不会失败，兜底返回空数组避免写入脏数据
            log.warn("授权动作序列化失败，按空数组处理", e);
            return "[]";
        }
    }

    /** 解析 JSON 原文为字符串列表，解析失败按空列表处理并记 warn 日志。 */
    private static List<String> parseActionCodes(String actionsJson) {
        if (actionsJson == null || actionsJson.isBlank()) {
            return List.of();
        }
        try {
            List<String> codes = OBJECT_MAPPER.readValue(actionsJson, ACTION_LIST_TYPE);
            return codes == null ? List.of() : codes;
        } catch (JsonProcessingException e) {
            log.warn("授权动作 JSON 解析失败，按空集处理：{}", actionsJson);
            return List.of();
        }
    }
}
