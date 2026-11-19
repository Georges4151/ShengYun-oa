package com.ceams.module.doc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.bpm.enums.AuditAction;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.bpm.support.BpmSecurityGuard;
import com.ceams.module.doc.dto.PermissionGrantRequest;
import com.ceams.module.doc.entity.DocFile;
import com.ceams.module.doc.entity.DocFilePermission;
import com.ceams.module.doc.enums.DocAction;
import com.ceams.module.doc.mapper.DocFileMapper;
import com.ceams.module.doc.mapper.DocFilePermissionMapper;
import com.ceams.module.doc.service.DocPermissionService;
import com.ceams.module.doc.support.DocConverter;
import com.ceams.module.doc.vo.PermissionVO;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文档授权服务实现（P2 仅文件级）。
 *
 * <p>同一 (fileId, subjectType, subjectId) 唯一：重复授权按覆盖处理；动作集合以 JSON 数组原样落库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocPermissionServiceImpl implements DocPermissionService {

    /** 授权管理权限码（V39 种子，仅 SYS_ADMIN）。 */
    private static final String PERMISSION_MANAGE = "doc:permission:manage";

    /** 审计目标类型。 */
    private static final String AUDIT_TARGET = "DOC_FILE";

    private final DocFileMapper fileMapper;
    private final DocFilePermissionMapper permissionMapper;
    private final BpmSecurityGuard securityGuard;
    private final BpmAuditService bpmAuditService;

    @Override
    public List<PermissionVO> list(Long fileId) {
        requireManage();
        requireFile(fileId);
        List<DocFilePermission> permissions =
                permissionMapper.selectList(
                        new LambdaQueryWrapper<DocFilePermission>()
                                .eq(DocFilePermission::getFileId, fileId)
                                .orderByAsc(DocFilePermission::getCreateTime)
                                .orderByAsc(DocFilePermission::getId));
        if (permissions == null) {
            return List.of();
        }
        return permissions.stream().map(DocConverter::toPermissionVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String grant(Long fileId, PermissionGrantRequest request) {
        requireManage();
        requireFile(fileId);
        EnumSet<DocAction> actions = resolveActions(request.getActions());

        DocFilePermission existing =
                permissionMapper.selectOne(
                        new LambdaQueryWrapper<DocFilePermission>()
                                .eq(DocFilePermission::getFileId, fileId)
                                .eq(DocFilePermission::getSubjectType, request.getSubjectType())
                                .eq(DocFilePermission::getSubjectId, request.getSubjectId())
                                .last("LIMIT 1"));
        Long operatorId = SecurityUtils.getCurrentUserIdOrDefault();
        String actionsJson = DocConverter.writeActions(actions);

        Map<String, Object> before = null;
        DocFilePermission permission;
        if (existing != null) {
            before = snapshot(existing);
            existing.setActions(actionsJson);
            existing.setGrantedBy(operatorId);
            permissionMapper.updateById(existing);
            permission = existing;
        } else {
            // 唯一键不含 deleted：取消授权是软删除，再授权需复活旧记录避免唯一键冲突
            DocFilePermission archived =
                    permissionMapper.selectAnyIncludingDeleted(
                            fileId, request.getSubjectType().name(), request.getSubjectId());
            if (archived != null) {
                before = snapshot(archived);
                permissionMapper.restore(archived.getId(), actionsJson, operatorId);
                archived.setActions(actionsJson);
                archived.setGrantedBy(operatorId);
                archived.setDeleted(0);
                permission = archived;
            } else {
                permission = new DocFilePermission();
                permission.setFileId(fileId);
                permission.setSubjectType(request.getSubjectType());
                permission.setSubjectId(request.getSubjectId());
                permission.setActions(actionsJson);
                permission.setGrantedBy(operatorId);
                permissionMapper.insert(permission);
            }
        }
        Map<String, Object> after = snapshot(permission);
        recordAudit(fileId, before, after);
        log.info(
                "文件 {} 授权：subjectType={}，subjectId={}，actions={}",
                fileId,
                request.getSubjectType(),
                request.getSubjectId(),
                actions);
        return String.valueOf(permission.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long fileId, Long permId) {
        requireManage();
        requireFile(fileId);
        DocFilePermission permission = permId == null ? null : permissionMapper.selectById(permId);
        if (permission == null || !Objects.equals(permission.getFileId(), fileId)) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "授权记录不存在");
        }
        Map<String, Object> before = snapshot(permission);
        permissionMapper.deleteById(permId);
        recordAudit(fileId, before, null);
        log.info("文件 {} 取消授权：permId={}", fileId, permId);
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    private void requireManage() {
        securityGuard.requirePermission(PERMISSION_MANAGE);
    }

    private DocFile requireFile(Long fileId) {
        DocFile file = fileId == null ? null : fileMapper.selectById(fileId);
        if (file == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "文件不存在");
        }
        return file;
    }

    /** 校验并归一化动作集合，未知动作直接 400，避免脏数据入库。 */
    private EnumSet<DocAction> resolveActions(List<String> rawActions) {
        EnumSet<DocAction> actions = EnumSet.noneOf(DocAction.class);
        if (rawActions != null) {
            for (String code : rawActions) {
                DocAction action = DocAction.from(code);
                if (action == null) {
                    throw BusinessException.of(
                            ErrorCode.VALID_ILLEGAL_ARGUMENT, "未知的文档动作：" + code);
                }
                actions.add(action);
            }
        }
        if (actions.isEmpty()) {
            throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "授权动作不能为空");
        }
        return actions;
    }

    private Map<String, Object> snapshot(DocFilePermission permission) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("subjectType", permission.getSubjectType());
        snapshot.put("subjectId", permission.getSubjectId());
        snapshot.put("actions", permission.getActions());
        return snapshot;
    }

    private void recordAudit(Long fileId, Object before, Object after) {
        bpmAuditService.record(
                AUDIT_TARGET,
                fileId,
                AuditAction.DOC_GRANT,
                before,
                after,
                null,
                SecurityUtils.getCurrentUserIdOrDefault());
    }
}
