package com.ceams.module.bpm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.bpm.dto.TemplateCreateRequest;
import com.ceams.module.bpm.dto.TemplatePublishRequest;
import com.ceams.module.bpm.dto.TemplateUpdateRequest;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.entity.BpmTemplate;
import com.ceams.module.bpm.entity.BpmTemplateVersion;
import com.ceams.module.bpm.enums.AuditAction;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.enums.TemplateStatus;
import com.ceams.module.bpm.enums.TemplateVersionStatus;
import com.ceams.module.bpm.mapper.BpmInstanceMapper;
import com.ceams.module.bpm.mapper.BpmTemplateMapper;
import com.ceams.module.bpm.mapper.BpmTemplateVersionMapper;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.bpm.service.BpmTemplateService;
import com.ceams.module.bpm.support.BpmSchema;
import com.ceams.module.bpm.support.BpmSchemaParser;
import com.ceams.module.bpm.support.BpmSecurityGuard;
import com.ceams.module.bpm.vo.TemplateVO;
import com.ceams.module.bpm.vo.TemplateVersionVO;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 流程模板服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpmTemplateServiceImpl implements BpmTemplateService {

    private final BpmTemplateMapper templateMapper;
    private final BpmTemplateVersionMapper versionMapper;
    private final BpmInstanceMapper instanceMapper;
    private final BpmSchemaParser schemaParser;
    private final BpmAuditService auditService;
    private final BpmSecurityGuard securityGuard;

    private static final String PERMISSION_MANAGE = "bpm:template:manage";
    private static final String PERMISSION_CREATE = "bpm:template:create";
    private static final String PERMISSION_DELETE = "bpm:template:delete";

    @Override
    public PageResult<TemplateVO> page(
            long page, long size, String bizType, TemplateStatus status, String keyword) {
        Page<BpmTemplate> p = new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 100));
        LambdaQueryWrapper<BpmTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(bizType), BpmTemplate::getBizType, bizType);
        wrapper.eq(status != null, BpmTemplate::getStatus, status);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(BpmTemplate::getCode, keyword).or().like(BpmTemplate::getName, keyword));
        }
        wrapper.orderByDesc(BpmTemplate::getCreateTime);
        IPage<BpmTemplate> result = templateMapper.selectPage(p, wrapper);
        List<TemplateVO> records = result.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(TemplateCreateRequest request) {
        securityGuard.requireAdminOrPermission(PERMISSION_CREATE);
        requireKnownBizType(request.getBizType());
        long exists =
                templateMapper.selectCount(
                        new LambdaQueryWrapper<BpmTemplate>().eq(BpmTemplate::getCode, request.getCode()));
        if (exists > 0) {
            throw BusinessException.of(ErrorCode.BIZ_DUPLICATE_CODE, "模板编码已存在");
        }
        BpmTemplate template = new BpmTemplate();
        template.setCode(request.getCode());
        template.setName(request.getName());
        template.setBizType(request.getBizType());
        template.setStatus(TemplateStatus.DISABLED);
        templateMapper.insert(template);
        return String.valueOf(template.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, TemplateUpdateRequest request) {
        securityGuard.requireAdminOrPermission(PERMISSION_MANAGE);
        BpmTemplate template = requireTemplate(id);
        if (StringUtils.hasText(request.getName())) {
            template.setName(request.getName());
        }
        if (StringUtils.hasText(request.getBizType())) {
            requireKnownBizType(request.getBizType());
            template.setBizType(request.getBizType());
        }
        templateMapper.updateById(template);
    }

    @Override
    public TemplateVO detail(Long id) {
        return toVO(requireTemplate(id));
    }

    @Override
    public List<TemplateVersionVO> versions(Long templateId) {
        requireTemplate(templateId);
        return versionMapper
                .selectList(
                        new LambdaQueryWrapper<BpmTemplateVersion>()
                                .eq(BpmTemplateVersion::getTemplateId, templateId)
                                .orderByDesc(BpmTemplateVersion::getVersionNo))
                .stream()
                .map(this::toVersionVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publish(Long templateId, TemplatePublishRequest request) {
        securityGuard.requireAdminOrPermission(PERMISSION_MANAGE);
        BpmTemplate template = requireTemplate(templateId);
        requireKnownBizType(template.getBizType());
        BpmSchema schema = schemaParser.parse(request.getSchemaJson());

        List<BpmTemplateVersion> history = versionMapper.selectList(
                new LambdaQueryWrapper<BpmTemplateVersion>()
                        .eq(BpmTemplateVersion::getTemplateId, templateId));
        int nextVersion = history.stream().mapToInt(BpmTemplateVersion::getVersionNo).max().orElse(0) + 1;

        // 旧发布版本归档：历史实例仍锁定原版本，新实例只取当前版本
        for (BpmTemplateVersion old : history) {
            if (old.getStatus() == TemplateVersionStatus.PUBLISHED) {
                old.setStatus(TemplateVersionStatus.ARCHIVED);
                versionMapper.updateById(old);
            }
        }

        BpmTemplateVersion version = new BpmTemplateVersion();
        version.setTemplateId(templateId);
        version.setVersionNo(nextVersion);
        version.setSchemaJson(request.getSchemaJson());
        version.setStatus(TemplateVersionStatus.PUBLISHED);
        version.setEffectiveTime(
                request.getEffectiveTime() == null ? LocalDateTime.now() : request.getEffectiveTime());
        versionMapper.insert(version);

        template.setCurrentVersionId(version.getId());
        template.setStatus(TemplateStatus.ENABLED);
        templateMapper.updateById(template);

        auditService.record(
                BpmAuditService.TARGET_TEMPLATE,
                templateId,
                AuditAction.TEMPLATE_PUBLISH,
                template,
                version,
                null,
                SecurityUtils.getCurrentUserId());
        log.info(
                "发布流程模板：templateId={}，versionNo={}，节点数={}",
                templateId,
                nextVersion,
                schema.getNodes().size());
        return version.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enable(Long id) {
        securityGuard.requireAdminOrPermission(PERMISSION_MANAGE);
        BpmTemplate template = requireTemplate(id);
        if (template.getCurrentVersionId() == null) {
            throw BusinessException.of(ErrorCode.BPM_TEMPLATE_NOT_ENABLED, "模板尚未发布，无法启用");
        }
        BpmTemplateVersion version = versionMapper.selectById(template.getCurrentVersionId());
        if (version == null || version.getStatus() == TemplateVersionStatus.ARCHIVED) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "当前版本已归档，请重新发布后再启用");
        }
        template.setStatus(TemplateStatus.ENABLED);
        templateMapper.updateById(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(Long id) {
        securityGuard.requireAdminOrPermission(PERMISSION_MANAGE);
        BpmTemplate template = requireTemplate(id);
        template.setStatus(TemplateStatus.DISABLED);
        templateMapper.updateById(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        securityGuard.requireAdminOrPermission(PERMISSION_DELETE);
        BpmTemplate template = requireTemplate(id);
        List<BpmTemplateVersion> versions =
                versionMapper.selectList(
                        new LambdaQueryWrapper<BpmTemplateVersion>()
                                .eq(BpmTemplateVersion::getTemplateId, id));
        if (!versions.isEmpty()) {
            List<Long> versionIds = versions.stream().map(BpmTemplateVersion::getId).toList();
            long instanceCount =
                    instanceMapper.selectCount(
                            new LambdaQueryWrapper<BpmInstance>()
                                    .in(BpmInstance::getTemplateVersionId, versionIds));
            if (instanceCount > 0) {
                throw BusinessException.of(
                        ErrorCode.BIZ_STATE_CONFLICT, "该模板已有流程实例，不能删除，可改为停用");
            }
            versionMapper.delete(
                    new LambdaQueryWrapper<BpmTemplateVersion>()
                            .eq(BpmTemplateVersion::getTemplateId, id));
        }
        templateMapper.deleteById(id);
        auditService.record(
                BpmAuditService.TARGET_TEMPLATE,
                id,
                AuditAction.TEMPLATE_DELETE,
                template,
                null,
                null,
                SecurityUtils.getCurrentUserId());
        log.info(
                "删除流程模板：templateId={}，code={}，版本数={}",
                id,
                template.getCode(),
                versions.size());
    }

    /** 业务类型必须是受支持的枚举（EXPENSE/CHANGE_REQUEST），防止脏数据进入模板。 */
    private void requireKnownBizType(String bizType) {
        if (BpmBizType.from(bizType) == null) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "不支持的业务类型：" + bizType);
        }
    }

    private BpmTemplate requireTemplate(Long id) {
        BpmTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "流程模板不存在");
        }
        return template;
    }

    private TemplateVO toVO(BpmTemplate template) {
        TemplateVO vo = new TemplateVO();
        vo.setId(template.getId());
        vo.setCode(template.getCode());
        vo.setName(template.getName());
        vo.setBizType(template.getBizType());
        vo.setCurrentVersionId(template.getCurrentVersionId());
        vo.setStatus(template.getStatus());
        vo.setCreateTime(template.getCreateTime());
        vo.setUpdateTime(template.getUpdateTime());
        if (template.getCurrentVersionId() != null) {
            BpmTemplateVersion version = versionMapper.selectById(template.getCurrentVersionId());
            if (version != null) {
                vo.setCurrentVersion(toVersionVO(version));
            }
        }
        return vo;
    }

    private TemplateVersionVO toVersionVO(BpmTemplateVersion version) {
        TemplateVersionVO vo = new TemplateVersionVO();
        vo.setId(version.getId());
        vo.setTemplateId(version.getTemplateId());
        vo.setVersionNo(version.getVersionNo());
        vo.setSchemaJson(version.getSchemaJson());
        vo.setStatus(version.getStatus());
        vo.setEffectiveTime(version.getEffectiveTime());
        vo.setCreateTime(version.getCreateTime());
        return vo;
    }
}
