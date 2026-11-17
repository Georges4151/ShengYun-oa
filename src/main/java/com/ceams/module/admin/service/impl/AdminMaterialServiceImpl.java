package com.ceams.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.admin.dto.MaterialApplyRequest;
import com.ceams.module.admin.dto.MaterialCreateRequest;
import com.ceams.module.admin.dto.MaterialStockInRequest;
import com.ceams.module.admin.dto.MaterialUpdateRequest;
import com.ceams.module.admin.entity.AdminMaterial;
import com.ceams.module.admin.entity.AdminMaterialApply;
import com.ceams.module.admin.enums.MaterialApplyStatus;
import com.ceams.module.admin.enums.MaterialStatus;
import com.ceams.module.admin.mapper.AdminMaterialApplyMapper;
import com.ceams.module.admin.mapper.AdminMaterialMapper;
import com.ceams.module.admin.service.AdminMaterialService;
import com.ceams.module.admin.support.AdminConverter;
import com.ceams.module.admin.vo.MaterialApplyVO;
import com.ceams.module.admin.vo.MaterialVO;
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.enums.AuditAction;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.hr.util.HrPageUtil;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.mapper.UserMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 行政易耗品服务实现。
 *
 * <p>库存扣减一律使用条件更新 {@code stock_qty = stock_qty - ? ... AND stock_qty >= ?}，
 * 影响行数为 0 即库存不足，禁止先查后减。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMaterialServiceImpl implements AdminMaterialService {

    /** 审计目标类型。 */
    private static final String AUDIT_TARGET = "ADMIN_MATERIAL";

    /** 金额保留小数位。 */
    private static final int MONEY_SCALE = 2;

    private final AdminMaterialMapper materialMapper;
    private final AdminMaterialApplyMapper applyMapper;
    private final BpmInstanceService bpmInstanceService;
    private final BpmAuditService bpmAuditService;
    private final UserMapper userMapper;

    @Override
    public PageResult<MaterialVO> page(
            String keyword, MaterialStatus status, long page, long size) {
        Page<AdminMaterial> p = HrPageUtil.resolve(page, size);
        LambdaQueryWrapper<AdminMaterial> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(AdminMaterial::getCode, kw).or().like(AdminMaterial::getName, kw));
        }
        if (status != null) {
            wrapper.eq(AdminMaterial::getStatus, status);
        }
        wrapper.orderByDesc(AdminMaterial::getCreateTime);
        IPage<AdminMaterial> result = materialMapper.selectPage(p, wrapper);
        List<MaterialVO> records =
                result.getRecords().stream().map(AdminConverter::toMaterialVO).toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public String create(MaterialCreateRequest request) {
        String code = request.getCode().trim();
        if (existsCode(code, null)) {
            throw BusinessException.of(ErrorCode.BIZ_DUPLICATE_CODE, "物料编码已存在");
        }
        AdminMaterial material = new AdminMaterial();
        material.setCode(code);
        material.setName(request.getName().trim());
        material.setSpecification(request.getSpecification());
        material.setUnit(request.getUnit());
        material.setStockQty(defaultQty(request.getStockQty()));
        material.setSafetyStock(defaultQty(request.getSafetyStock()));
        material.setUnitPrice(normalizeMoney(request.getUnitPrice()));
        material.setStatus(MaterialStatus.ENABLED);
        try {
            materialMapper.insert(material);
        } catch (DuplicateKeyException e) {
            throw BusinessException.of(ErrorCode.BIZ_DUPLICATE_CODE, "物料编码已存在");
        }
        log.info("易耗品建档成功：{}，编码 {}", material.getId(), code);
        return String.valueOf(material.getId());
    }

    @Override
    public void update(Long id, MaterialUpdateRequest request) {
        AdminMaterial material = requireMaterial(id);
        if (StringUtils.isNotBlank(request.getCode())) {
            String code = request.getCode().trim();
            if (!code.equals(material.getCode())) {
                if (existsCode(code, id)) {
                    throw BusinessException.of(ErrorCode.BIZ_DUPLICATE_CODE, "物料编码已存在");
                }
                material.setCode(code);
            }
        }
        if (StringUtils.isNotBlank(request.getName())) {
            material.setName(request.getName().trim());
        }
        if (request.getSpecification() != null) {
            material.setSpecification(request.getSpecification());
        }
        if (request.getUnit() != null) {
            material.setUnit(request.getUnit());
        }
        if (request.getSafetyStock() != null) {
            material.setSafetyStock(requireNonNegative(request.getSafetyStock(), "安全库存"));
        }
        if (request.getUnitPrice() != null) {
            material.setUnitPrice(normalizeMoney(request.getUnitPrice()));
        }
        materialMapper.updateById(material);
        log.info("易耗品 {} 基础信息已更新", id);
    }

    @Override
    @Transactional
    public void stockIn(Long id, MaterialStockInRequest request) {
        AdminMaterial material = requireMaterial(id);
        BigDecimal quantity = request.getQuantity();
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "入库数量必须大于 0");
        }
        BigDecimal before = defaultQty(material.getStockQty());
        UpdateWrapper<AdminMaterial> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", id)
                .setSql("stock_qty = stock_qty + " + quantity.toPlainString());
        int rows = materialMapper.update(null, wrapper);
        if (rows == 0) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "物料不存在或已删除");
        }
        recordAudit(
                id,
                before.toPlainString(),
                before.add(quantity).toPlainString());
        log.info("易耗品 {} 入库 {}，库存 {} → {}", id, quantity, before, before.add(quantity));
    }

    @Override
    @Transactional
    public String apply(MaterialApplyRequest request) {
        AdminMaterial material = requireMaterial(request.getMaterialId());
        if (material.getStatus() != MaterialStatus.ENABLED) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "物料已停用，无法申领");
        }
        BigDecimal quantity = request.getQuantity();
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "申领数量必须大于 0");
        }
        if (quantity.compareTo(defaultQty(material.getStockQty())) > 0) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "申领数量超过当前库存");
        }

        AdminMaterialApply apply = new AdminMaterialApply();
        apply.setMaterialId(request.getMaterialId());
        apply.setApplicantId(SecurityUtils.getCurrentUserIdOrDefault());
        apply.setQuantity(quantity);
        apply.setReason(request.getReason());
        apply.setStatus(MaterialApplyStatus.APPROVING);
        applyMapper.insert(apply);

        InstanceStartRequest start = new InstanceStartRequest();
        start.setTemplateCode(BpmBizType.ADMIN_MATERIAL_APPLY.name());
        start.setBizType(BpmBizType.ADMIN_MATERIAL_APPLY.name());
        start.setBizId(apply.getId());
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("materialId", String.valueOf(request.getMaterialId()));
        variables.put("materialCode", material.getCode());
        variables.put("applicantId", String.valueOf(apply.getApplicantId()));
        variables.put("quantity", quantity.stripTrailingZeros().toPlainString());
        start.setVariables(variables);
        String instanceId = bpmInstanceService.start(start);

        apply.setBpmInstanceId(parseInstanceId(instanceId));
        applyMapper.updateById(apply);
        log.info("易耗品 {} 发起申领，申领单 {}，BPM 实例 {}", request.getMaterialId(), apply.getId(), instanceId);
        return String.valueOf(apply.getId());
    }

    @Override
    public List<MaterialApplyVO> myApplies() {
        Long applicantId = SecurityUtils.getCurrentUserIdOrDefault();
        List<AdminMaterialApply> records =
                applyMapper.selectList(
                        new LambdaQueryWrapper<AdminMaterialApply>()
                                .eq(AdminMaterialApply::getApplicantId, applicantId)
                                .orderByDesc(AdminMaterialApply::getCreateTime));
        if (records.isEmpty()) {
            return List.of();
        }
        Map<Long, AdminMaterial> materials = loadMaterials(records);
        String applicantName = currentUserName(applicantId);
        return records.stream()
                .map(
                        apply ->
                                AdminConverter.toMaterialApplyVO(
                                        apply, materials.get(apply.getMaterialId()), applicantName))
                .toList();
    }

    @Override
    @Transactional
    public void issue(Long applyId) {
        AdminMaterialApply apply = applyId == null ? null : applyMapper.selectById(applyId);
        if (apply == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "申领单不存在");
        }
        AdminMaterialApply patch = new AdminMaterialApply();
        patch.setVersion(null);
        patch.setStatus(MaterialApplyStatus.ISSUED);
        patch.setIssuedAt(LocalDateTime.now());
        patch.setIssuedBy(SecurityUtils.getCurrentUserIdOrDefault());
        int rows =
                applyMapper.update(
                        patch,
                        new LambdaUpdateWrapper<AdminMaterialApply>()
                                .eq(AdminMaterialApply::getId, applyId)
                                .eq(AdminMaterialApply::getStatus, MaterialApplyStatus.APPROVED));
        if (rows == 0) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅审批通过的申领单可发放");
        }
        log.info("申领单 {} 已发放", applyId);
    }

    @Override
    public List<MaterialVO> alerts() {
        QueryWrapper<AdminMaterial> wrapper = new QueryWrapper<>();
        wrapper.eq("status", MaterialStatus.ENABLED.name())
                .apply("stock_qty <= safety_stock");
        List<MaterialVO> result = new ArrayList<>();
        for (AdminMaterial material : materialMapper.selectList(wrapper)) {
            result.add(AdminConverter.toMaterialVO(material));
        }
        // 缺口比例越大越靠前，便于优先补货
        result.sort((a, b) -> b.getShortageRatio().compareTo(a.getShortageRatio()));
        return result;
    }

    private AdminMaterial requireMaterial(Long id) {
        AdminMaterial material = id == null ? null : materialMapper.selectById(id);
        if (material == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "物料不存在");
        }
        return material;
    }

    private boolean existsCode(String code, Long excludeId) {
        LambdaQueryWrapper<AdminMaterial> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminMaterial::getCode, code);
        if (excludeId != null) {
            wrapper.ne(AdminMaterial::getId, excludeId);
        }
        Long count = materialMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private Map<Long, AdminMaterial> loadMaterials(List<AdminMaterialApply> records) {
        List<Long> materialIds =
                records.stream()
                        .map(AdminMaterialApply::getMaterialId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        if (materialIds.isEmpty()) {
            return Map.of();
        }
        return materialMapper.selectBatchIds(materialIds).stream()
                .collect(Collectors.toMap(AdminMaterial::getId, Function.identity(), (a, b) -> a));
    }

    private String currentUserName(Long userId) {
        if (userId == null || userId <= 0L) {
            return null;
        }
        SysUser user = userMapper.selectById(userId);
        return user == null ? null : user.getName();
    }

    private BigDecimal defaultQty(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal requireNonNegative(BigDecimal value, String label) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, label + "不能为负数");
        }
        return value;
    }

    /** 金额归一化：非空时校验非负并保留 2 位小数。 */
    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return requireNonNegative(value, "单价").setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private void recordAudit(Long targetId, String beforeQty, String afterQty) {
        bpmAuditService.record(
                AUDIT_TARGET,
                targetId,
                AuditAction.STOCK_IN,
                Map.of("stockQty", beforeQty),
                Map.of("stockQty", afterQty),
                null,
                SecurityUtils.getCurrentUserIdOrDefault());
    }

    private Long parseInstanceId(String instanceId) {
        if (StringUtils.isBlank(instanceId)) {
            return null;
        }
        try {
            return Long.valueOf(instanceId.trim());
        } catch (NumberFormatException e) {
            log.warn("BPM 实例 ID 非数字，无法回写申领单：{}", instanceId);
            return null;
        }
    }
}
