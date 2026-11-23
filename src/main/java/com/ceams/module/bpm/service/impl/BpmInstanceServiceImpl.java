package com.ceams.module.bpm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.enums.ContractApprovalStatus;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.admin.entity.AdminRepair;
import com.ceams.module.admin.enums.RepairCategory;
import com.ceams.module.admin.enums.RepairStatus;
import com.ceams.module.admin.mapper.AdminRepairMapper;
import com.ceams.module.bpm.dto.FlowPreviewRequest;
import com.ceams.module.bpm.dto.InstanceActionRequest;
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.entity.BpmCcRecord;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.entity.BpmNode;
import com.ceams.module.bpm.entity.BpmTask;
import com.ceams.module.bpm.entity.BpmTemplate;
import com.ceams.module.bpm.entity.BpmTemplateVersion;
import com.ceams.module.bpm.entity.BpmVariable;
import com.ceams.module.bpm.enums.AuditAction;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.enums.BpmDataScope;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.enums.BpmNodeStatus;
import com.ceams.module.bpm.enums.BpmNodeType;
import com.ceams.module.bpm.enums.TemplateStatus;
import com.ceams.module.bpm.enums.TemplateVersionStatus;
import com.ceams.module.bpm.mapper.BpmCcRecordMapper;
import com.ceams.module.bpm.mapper.BpmInstanceMapper;
import com.ceams.module.bpm.mapper.BpmNodeMapper;
import com.ceams.module.bpm.mapper.BpmTaskMapper;
import com.ceams.module.bpm.mapper.BpmTemplateMapper;
import com.ceams.module.bpm.mapper.BpmTemplateVersionMapper;
import com.ceams.module.bpm.mapper.BpmVariableMapper;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.bpm.support.BpmAssigneeResolver;
import com.ceams.module.bpm.support.BpmFlowEngine;
import com.ceams.module.bpm.support.BpmResolvedAssignee;
import com.ceams.module.bpm.support.BpmSchema;
import com.ceams.module.bpm.support.BpmSchemaNode;
import com.ceams.module.bpm.support.BpmSchemaParser;
import com.ceams.module.bpm.support.BpmSecurityGuard;
import com.ceams.module.bpm.support.BpmViewAssembler;
import com.ceams.module.bpm.vo.BizAttachmentGroupVO;
import com.ceams.module.bpm.vo.BizAttachmentVO;
import com.ceams.module.bpm.vo.BizDetailItemVO;
import com.ceams.module.bpm.vo.BizDetailVO;
import com.ceams.module.bpm.vo.FlowAssigneeVO;
import com.ceams.module.bpm.vo.FlowPreviewVO;
import com.ceams.module.bpm.vo.InstanceDetailVO;
import com.ceams.module.bpm.vo.InstanceVO;
import com.ceams.module.bpm.vo.NodeVO;
import com.ceams.module.bpm.vo.PreviewNodeVO;
import com.ceams.module.bpm.vo.TaskVO;
import com.ceams.module.expense.entity.Receipt;
import com.ceams.module.expense.enums.PaymentStatus;
import com.ceams.module.expense.enums.ReceiptApprovalStatus;
import com.ceams.module.expense.enums.ReceiptType;
import com.ceams.module.expense.mapper.ReceiptMapper;
import com.ceams.module.file.service.FileService;
import com.ceams.module.hr.entity.HrCheckinSupplement;
import com.ceams.module.hr.entity.HrContract;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.entity.HrEmployeeChange;
import com.ceams.module.hr.entity.HrJobLevel;
import com.ceams.module.hr.entity.HrPosition;
import com.ceams.module.hr.entity.HrSalaryChangeApply;
import com.ceams.module.hr.enums.ChangeType;
import com.ceams.module.hr.enums.CheckinType;
import com.ceams.module.hr.enums.HrApprovalStatus;
import com.ceams.module.hr.enums.SalaryChangeStatus;
import com.ceams.module.hr.enums.SalaryChangeType;
import com.ceams.module.hr.mapper.HrCheckinSupplementMapper;
import com.ceams.module.hr.mapper.HrContractMapper;
import com.ceams.module.hr.mapper.HrEmployeeChangeMapper;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.mapper.HrJobLevelMapper;
import com.ceams.module.hr.mapper.HrPositionMapper;
import com.ceams.module.hr.mapper.HrSalaryChangeApplyMapper;
import com.ceams.module.iam.entity.SysDept;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.mapper.DeptMapper;
import com.ceams.module.iam.mapper.UserMapper;
import com.ceams.module.project.entity.ChangeRequest;
import com.ceams.module.project.entity.Project;
import com.ceams.module.project.entity.Subcontractor;
import com.ceams.module.project.entity.SubcontractorContract;
import com.ceams.module.project.enums.ApprovalStatus;
import com.ceams.module.project.mapper.ChangeRequestMapper;
import com.ceams.module.project.mapper.ProjectMapper;
import com.ceams.module.project.mapper.SubcontractorContractMapper;
import com.ceams.module.project.mapper.SubcontractorMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 流程实例服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpmInstanceServiceImpl implements BpmInstanceService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String EMPTY_VALUE = "-";
    private static final String ATTACHMENT_KIND_IMAGE = "IMAGE";
    private static final String ATTACHMENT_KIND_PDF = "PDF";
    private static final String ATTACHMENT_KIND_OFFICE = "OFFICE";
    private static final String ATTACHMENT_KIND_OTHER = "OTHER";

    private final BpmInstanceMapper instanceMapper;
    private final BpmTemplateMapper templateMapper;
    private final BpmTemplateVersionMapper versionMapper;
    private final BpmNodeMapper nodeMapper;
    private final BpmTaskMapper taskMapper;
    private final BpmVariableMapper variableMapper;
    private final BpmCcRecordMapper ccRecordMapper;
    private final BpmSchemaParser schemaParser;
    private final BpmFlowEngine flowEngine;
    private final BpmAuditService auditService;
    private final BpmSecurityGuard securityGuard;
    private final BpmViewAssembler assembler;
    private final BpmAssigneeResolver assigneeResolver;
    private final UserMapper userMapper;
    private final ReceiptMapper receiptMapper;
    private final ProjectMapper projectMapper;
    private final ChangeRequestMapper changeRequestMapper;
    private final AdminRepairMapper adminRepairMapper;
    private final HrEmployeeChangeMapper employeeChangeMapper;
    private final HrEmployeeMapper employeeMapper;
    private final HrSalaryChangeApplyMapper salaryChangeApplyMapper;
    private final HrCheckinSupplementMapper checkinSupplementMapper;
    private final DeptMapper deptMapper;
    private final HrPositionMapper positionMapper;
    private final HrJobLevelMapper jobLevelMapper;
    private final HrContractMapper hrContractMapper;
    private final SubcontractorContractMapper subcontractorContractMapper;
    private final SubcontractorMapper subcontractorMapper;
    private final ObjectMapper objectMapper;
    private final FileService fileService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String start(InstanceStartRequest request) {
        BpmTemplate template =
                templateMapper.selectOne(
                        new LambdaQueryWrapper<BpmTemplate>()
                                .eq(BpmTemplate::getCode, request.getTemplateCode()));
        if (template == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "流程模板不存在");
        }
        if (template.getStatus() != TemplateStatus.ENABLED || template.getCurrentVersionId() == null) {
            throw BusinessException.of(ErrorCode.BPM_TEMPLATE_NOT_ENABLED, "流程模板未启用，不可发起");
        }
        BpmTemplateVersion version = versionMapper.selectById(template.getCurrentVersionId());
        if (version == null || version.getStatus() != TemplateVersionStatus.PUBLISHED) {
            throw BusinessException.of(ErrorCode.BPM_TEMPLATE_NOT_ENABLED, "流程模板当前版本未发布");
        }
        BpmSchema schema = schemaParser.parse(version.getSchemaJson());

        Long initiatorId = securityGuard.currentUserId();
        if (initiatorId == null) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "未登录不可发起流程");
        }
        String bizType =
                StringUtils.hasText(request.getBizType()) ? request.getBizType() : template.getBizType();
        // 业务类型必须与模板一致，避免通过传入任意 bizType 绕过发起权限
        if (!Objects.equals(bizType, template.getBizType()) || BpmBizType.from(bizType) == null) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "业务类型与模板不一致或不受支持：" + bizType);
        }
        securityGuard.requireBizPermission(bizType);

        BpmInstance instance = new BpmInstance();
        instance.setTemplateVersionId(version.getId());
        instance.setBizType(bizType);
        instance.setBizId(request.getBizId());
        instance.setInitiatorId(initiatorId);
        instance.setStatus(BpmInstanceStatus.RUNNING);
        instance.setStartedAt(LocalDateTime.now());
        instanceMapper.insert(instance);

        List<BpmNode> nodes = createNodes(instance, schema);
        saveVariables(instance, template, request.getVariables());

        flowEngine.start(instance, nodes, schema);
        auditService.record(
                BpmAuditService.TARGET_INSTANCE,
                instance.getId(),
                AuditAction.START,
                null,
                instance,
                null,
                initiatorId);
        log.info(
                "发起 BPM 实例：instanceId={}，templateCode={}，bizType={}，initiator={}",
                instance.getId(),
                template.getCode(),
                instance.getBizType(),
                initiatorId);
        return String.valueOf(instance.getId());
    }

    @Override
    public InstanceDetailVO detail(Long id) {
        BpmInstance instance = requireInstance(id);
        assertVisible(instance);
        List<BpmNode> nodes = flowEngine.loadNodes(id);
        List<BpmTask> tasks =
                taskMapper.selectList(new LambdaQueryWrapper<BpmTask>().eq(BpmTask::getInstanceId, id));
        Map<String, String> variables =
                variableMapper
                        .selectList(
                                new LambdaQueryWrapper<BpmVariable>()
                                        .eq(BpmVariable::getInstanceId, id))
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        BpmVariable::getVarKey,
                                        v -> Objects.toString(v.getVarValue(), ""),
                                        (a, b) -> a,
                                        LinkedHashMap::new));
        List<BpmCcRecord> ccRecords =
                ccRecordMapper.selectList(
                        new LambdaQueryWrapper<BpmCcRecord>().eq(BpmCcRecord::getInstanceId, id));

        InstanceDetailVO detail = new InstanceDetailVO();
        detail.setInstance(assembler.toInstanceVO(instance, nodes, tasks));
        List<NodeVO> nodeVOs = assembler.toNodeVOs(nodes, tasks);
        enrichNodes(instance, nodeVOs);
        detail.setNodes(nodeVOs);
        detail.setVariables(variables);
        detail.setCcRecords(
                ccRecords.stream()
                        .map(
                                r ->
                                        assembler.toCcRecordVO(
                                                r,
                                                Map.of(instance.getId(), instance),
                                                assembler.index(nodes, BpmNode::getId)))
                        .toList());
        return detail;
    }

    @Override
    public BizDetailVO bizDetail(Long instanceId) {
        BpmInstance instance = requireInstance(instanceId);
        // 与流程详情保持同一可见性口径，避免通过业务详情绕过权限
        assertVisible(instance);
        BpmBizType bizType = BpmBizType.from(instance.getBizType());
        if (bizType == null) {
            return unsupportedDetail(instance);
        }
        BizDetailVO detail =
                switch (bizType) {
                    case EXPENSE -> receiptDetail(instance);
                    case CHANGE_REQUEST -> changeRequestDetail(instance);
                    case ADMIN_REPAIR -> adminRepairDetail(instance);
                    case HR_REGULAR, HR_TRANSFER, HR_PROMOTION, HR_RESIGN ->
                            employeeChangeDetail(instance);
                    case HR_SALARY_CHANGE, HR_SALARY_CORRECTION -> salaryChangeDetail(instance);
                    case HR_CHECKIN_SUPPLEMENT -> checkinSupplementDetail(instance);
                    case HR_CONTRACT -> hrContractDetail(instance);
                    case SUBCONTRACT_CONTRACT -> subcontractorContractDetail(instance);
                    default -> null;
                };
        return detail != null ? detail : unsupportedDetail(instance);
    }

    @Override
    public byte[] loadAttachment(Long instanceId, String name) {
        BpmInstance instance = requireInstance(instanceId);
        // 附件下载沿用详情可见性校验，避免直接猜文件名越权下载
        assertVisible(instance);
        // 只传存储名给文件模块，由其统一做安全校验与磁盘读取，BPM 不拼接任何目录
        return fileService.load(name);
    }

    /** 报销单详情：字段取自 biz_receipt 实体，项目名称按项目 ID 反查。 */
    private BizDetailVO receiptDetail(BpmInstance instance) {
        Receipt receipt =
                instance.getBizId() == null ? null : receiptMapper.selectById(instance.getBizId());
        if (receipt == null) {
            return unsupportedDetail(instance);
        }
        BizDetailVO detail =
                newDetail(instance, "报销单详情", receiptApprovalText(receipt.getApprovalStatus()));
        addItem(detail, "票据编号", receipt.getReceiptNumber());
        addItem(detail, "票据类型", receiptTypeText(receipt.getType()));
        addItem(detail, "金额", receipt.getAmount());
        addItem(detail, "项目", projectName(receipt.getProjectId()));
        addItem(detail, "报票人", receipt.getReporterName());
        addItem(detail, "票据日期", receipt.getReceiptDate());
        addItem(detail, "摘要", receipt.getDescription());
        addItem(detail, "审批状态", receiptApprovalText(receipt.getApprovalStatus()));
        addItem(detail, "付款状态", paymentStatusText(receipt.getPaymentStatus()));
        addItem(detail, "创建时间", receipt.getCreateTime());
        // 票据图片：仅一个文件，按扩展名识别种类，无法识别时按图片处理
        addAttachmentGroup(detail, "票据图片", receiptAttachments(receipt.getFilePath()));
        return detail;
    }

    /** 项目变更申请详情：字段取自 biz_change_request 实体。 */
    private BizDetailVO changeRequestDetail(BpmInstance instance) {
        ChangeRequest change =
                instance.getBizId() == null
                        ? null
                        : changeRequestMapper.selectById(instance.getBizId());
        if (change == null) {
            return unsupportedDetail(instance);
        }
        BizDetailVO detail =
                newDetail(instance, "项目变更申请", approvalStatusText(change.getStatus()));
        addItem(detail, "项目", projectName(change.getProjectId()));
        addItem(detail, "变更类型", change.getRequestType());
        addItem(detail, "变更说明", change.getDescription());
        addItem(detail, "金额影响", change.getEstimatedCost());
        addItem(detail, "审批状态", approvalStatusText(change.getStatus()));
        addItem(detail, "申请人", userName(change.getRequestedBy()));
        addItem(detail, "审批意见", change.getApprovalComment());
        addItem(detail, "申请时间", change.getCreateTime());
        addItem(detail, "审批时间", change.getApprovedAt());
        return detail;
    }

    /** 行政报修详情：字段取自 admin_repair 实体。 */
    private BizDetailVO adminRepairDetail(BpmInstance instance) {
        AdminRepair repair =
                instance.getBizId() == null
                        ? null
                        : adminRepairMapper.selectById(instance.getBizId());
        if (repair == null) {
            return unsupportedDetail(instance);
        }
        BizDetailVO detail = newDetail(instance, "报修单详情", repairStatusText(repair.getStatus()));
        addItem(detail, "报修单号", repair.getRepairNo());
        addItem(detail, "报修人", userName(repair.getReporterId()));
        addItem(detail, "报修位置", repair.getLocation());
        addItem(detail, "报修分类", repairCategoryText(repair.getCategory()));
        addItem(detail, "故障描述", repair.getDescription());
        addItem(detail, "状态", repairStatusText(repair.getStatus()));
        addItem(detail, "处理说明", repair.getRemark());
        addItem(detail, "申请时间", repair.getCreateTime());
        addItem(detail, "完工时间", repair.getFinishedAt());
        // 报修图片 / 完工图片：JSON 数组字符串，元素可能是存储名或 /api/v1/files/xxx 路径
        addAttachmentGroup(detail, "报修图片", parseImageAttachments(repair.getImagesJson()));
        addAttachmentGroup(detail, "完工图片", parseImageAttachments(repair.getFinishImagesJson()));
        return detail;
    }

    /** 入转调离申请详情：原值取员工档案，目标值取申请单，名称按已有字典 Mapper 反查。 */
    private BizDetailVO employeeChangeDetail(BpmInstance instance) {
        HrEmployeeChange change =
                instance.getBizId() == null
                        ? null
                        : employeeChangeMapper.selectById(instance.getBizId());
        if (change == null) {
            return unsupportedDetail(instance);
        }
        HrEmployee employee =
                change.getEmployeeId() == null ? null : employeeMapper.selectById(change.getEmployeeId());
        BizDetailVO detail =
                newDetail(
                        instance,
                        employeeChangeTitle(change.getChangeType()),
                        hrApprovalText(change.getApprovalStatus()));
        addItem(detail, "员工姓名", employee == null ? null : employee.getName());
        addItem(detail, "变更类型", changeTypeText(change.getChangeType()));
        addItem(detail, "原部门", employee == null ? null : deptName(employee.getDeptId()));
        addItem(detail, "目标部门", deptName(change.getTargetDeptId()));
        addItem(detail, "原岗位", employee == null ? null : positionName(employee.getPositionId()));
        addItem(detail, "目标岗位", positionName(change.getTargetPositionId()));
        addItem(detail, "原职级", employee == null ? null : jobLevelName(employee.getLevelId()));
        addItem(detail, "目标职级", jobLevelName(change.getTargetLevelId()));
        if (change.getResignLeaderId() != null) {
            addItem(detail, "交接上级", employeeName(change.getResignLeaderId()));
        }
        addItem(detail, "生效日期", change.getEffectiveDate());
        addItem(detail, "申请原因", change.getReason());
        addItem(detail, "备注", change.getRemark());
        addItem(detail, "审批状态", hrApprovalText(change.getApprovalStatus()));
        addItem(detail, "申请时间", change.getCreateTime());
        return detail;
    }

    /**
     * 薪资变动/更正申请详情。
     *
     * <p>变更前后快照内的金额字段为服务端密文，不在待办详情中展示，避免密文泄露。
     */
    private BizDetailVO salaryChangeDetail(BpmInstance instance) {
        HrSalaryChangeApply apply =
                instance.getBizId() == null
                        ? null
                        : salaryChangeApplyMapper.selectById(instance.getBizId());
        if (apply == null) {
            return unsupportedDetail(instance);
        }
        boolean correction = BpmBizType.HR_SALARY_CORRECTION.name().equals(instance.getBizType());
        BizDetailVO detail =
                newDetail(
                        instance,
                        correction ? "薪资更正申请" : "薪资变动申请",
                        salaryStatusText(apply.getStatus()));
        addItem(detail, "员工姓名", employeeName(apply.getEmployeeId()));
        addItem(detail, "变动类型", salaryChangeTypeText(apply.getChangeType()));
        addItem(detail, "期望生效日期", apply.getEffectiveDate());
        addItem(detail, "申请原因", apply.getReason());
        addItem(detail, "审批状态", salaryStatusText(apply.getStatus()));
        addItem(detail, "申请时间", apply.getCreateTime());
        return detail;
    }

    /** 补卡申请详情：字段取自 hr_checkin_supplement 实体。 */
    private BizDetailVO checkinSupplementDetail(BpmInstance instance) {
        HrCheckinSupplement supplement =
                instance.getBizId() == null
                        ? null
                        : checkinSupplementMapper.selectById(instance.getBizId());
        if (supplement == null) {
            return unsupportedDetail(instance);
        }
        BizDetailVO detail =
                newDetail(
                        instance, "补卡申请", hrApprovalText(supplement.getApprovalStatus()));
        addItem(detail, "员工姓名", employeeName(supplement.getEmployeeId()));
        addItem(detail, "补卡日期", supplement.getCheckinDate());
        addItem(detail, "补卡时间", supplement.getCheckinTime());
        addItem(detail, "打卡类型", checkinTypeText(supplement.getCheckinType()));
        addItem(detail, "补卡事由", supplement.getReason());
        addItem(detail, "审批状态", hrApprovalText(supplement.getApprovalStatus()));
        addItem(detail, "申请时间", supplement.getCreateTime());
        return detail;
    }

    /** 劳动合同详情：字段取自 hr_contract 实体，并附带合同文件附件。 */
    private BizDetailVO hrContractDetail(BpmInstance instance) {
        HrContract contract =
                instance.getBizId() == null
                        ? null
                        : hrContractMapper.selectById(instance.getBizId());
        if (contract == null) {
            return unsupportedDetail(instance);
        }
        BizDetailVO detail =
                newDetail(
                        instance,
                        "劳动合同详情",
                        contractApprovalStatusText(contract.getApprovalStatus()));
        addItem(detail, "合同编号", contract.getContractNo());
        addItem(detail, "合同名称", hrContractName(contract));
        addItem(detail, "员工姓名", employeeName(contract.getEmployeeId()));
        // 劳动合同不关联项目，也没有金额字段，保持占位便于前端统一渲染
        addItem(detail, "项目", null);
        addItem(detail, "合同金额", null);
        addItem(detail, "开始日期", contract.getStartDate());
        addItem(detail, "结束日期", contract.getEndDate());
        addItem(detail, "签订日期", contract.getSignDate());
        addItem(detail, "合同状态", hrContractStatusText(contract.getStatus()));
        addItem(detail, "审批状态", contractApprovalStatusText(contract.getApprovalStatus()));
        addItem(detail, "备注", contract.getRemark());
        addAttachmentGroup(
                detail,
                "合同文件",
                contractAttachments(contract.getFilePath(), contract.getFileName()));
        return detail;
    }

    /** 分包合同详情：字段取自 biz_subcontractor_contract 实体，分包商/项目名称反查。 */
    private BizDetailVO subcontractorContractDetail(BpmInstance instance) {
        SubcontractorContract contract =
                instance.getBizId() == null
                        ? null
                        : subcontractorContractMapper.selectById(instance.getBizId());
        if (contract == null) {
            return unsupportedDetail(instance);
        }
        BizDetailVO detail =
                newDetail(
                        instance,
                        "分包合同详情",
                        contractApprovalStatusText(contract.getApprovalStatus()));
        addItem(detail, "合同编号", contract.getContractNumber());
        addItem(detail, "合同名称", contract.getContractName());
        addItem(detail, "分包商", subcontractorName(contract.getSubcontractorId()));
        addItem(detail, "项目", projectName(contract.getProjectId()));
        addItem(detail, "合同金额", contract.getContractAmount());
        addItem(detail, "开始日期", contract.getStartDate());
        addItem(detail, "结束日期", contract.getEndDate());
        // 分包合同表未记录签订日期，保持占位便于前端统一渲染
        addItem(detail, "签订日期", null);
        addItem(detail, "合同状态", subcontractContractStatusText(contract.getStatus()));
        addItem(detail, "审批状态", contractApprovalStatusText(contract.getApprovalStatus()));
        addItem(detail, "备注", contract.getDescription());
        addAttachmentGroup(
                detail,
                "合同文件",
                contractAttachments(contract.getFilePath(), contract.getFileName()));
        return detail;
    }

    /** 不支持或查不到单据时的兜底：只展示业务类型与单据 ID，并给出中文说明。 */
    private BizDetailVO unsupportedDetail(BpmInstance instance) {
        BizDetailVO detail = newDetail(instance, "业务单据", "该业务类型暂不支持查看详情");
        addItem(detail, "业务类型", instance.getBizType());
        addItem(detail, "单据 ID", instance.getBizId());
        return detail;
    }

    private BizDetailVO newDetail(BpmInstance instance, String title, String statusText) {
        BizDetailVO detail = new BizDetailVO();
        detail.setBizType(instance.getBizType());
        detail.setBizId(Objects.toString(instance.getBizId(), null));
        detail.setTitle(title);
        detail.setStatusText(statusText);
        return detail;
    }

    private void addItem(BizDetailVO detail, String label, Object value) {
        detail.getItems().add(new BizDetailItemVO(label, text(value)));
    }

    /** 统一空值占位与时间格式：空值 "-"，日期 yyyy-MM-dd，时间 yyyy-MM-dd HH:mm:ss。 */
    private String text(Object value) {
        if (value == null) {
            return EMPTY_VALUE;
        }
        if (value instanceof LocalDateTime dateTime) {
            return DATE_TIME_FORMATTER.format(dateTime);
        }
        if (value instanceof LocalDate date) {
            return DATE_FORMATTER.format(date);
        }
        String text = value.toString();
        return StringUtils.hasText(text) ? text : EMPTY_VALUE;
    }

    private String projectName(Long projectId) {
        if (projectId == null) {
            return null;
        }
        Project project = projectMapper.selectById(projectId);
        return project == null ? null : project.getProjectName();
    }

    private String userName(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = userMapper.selectById(userId);
        return user == null ? null : user.getName();
    }

    private String employeeName(Long employeeId) {
        if (employeeId == null) {
            return null;
        }
        HrEmployee employee = employeeMapper.selectById(employeeId);
        return employee == null ? null : employee.getName();
    }

    private String deptName(Long deptId) {
        if (deptId == null) {
            return null;
        }
        SysDept dept = deptMapper.selectById(deptId);
        return dept == null ? null : dept.getName();
    }

    private String positionName(Long positionId) {
        if (positionId == null) {
            return null;
        }
        HrPosition position = positionMapper.selectById(positionId);
        return position == null ? null : position.getName();
    }

    private String jobLevelName(Long levelId) {
        if (levelId == null) {
            return null;
        }
        HrJobLevel level = jobLevelMapper.selectById(levelId);
        return level == null ? null : level.getName();
    }

    private String subcontractorName(Long subcontractorId) {
        if (subcontractorId == null) {
            return null;
        }
        Subcontractor subcontractor = subcontractorMapper.selectById(subcontractorId);
        return subcontractor == null ? null : subcontractor.getName();
    }

    /** 劳动合同名称：合同表无独立名称字段，用「合同类型 + 劳动合同」组合展示。 */
    private String hrContractName(HrContract contract) {
        String typeText = hrContractTypeText(contract.getContractType());
        if (!StringUtils.hasText(typeText)) {
            return null;
        }
        return typeText + "劳动合同";
    }

    private String hrContractTypeText(com.ceams.module.hr.enums.ContractType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case FIXED -> "固定期限";
            case UNFIXED -> "无固定期限";
            case INTERN -> "实习";
        };
    }

    private String hrContractStatusText(com.ceams.module.hr.enums.ContractStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case ACTIVE -> "生效中";
            case EXPIRED -> "已到期";
            case TERMINATED -> "已终止";
        };
    }

    private String subcontractContractStatusText(
            com.ceams.module.project.enums.ContractStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case DRAFT -> "草稿";
            case ACTIVE -> "生效中";
            case COMPLETED -> "已完成";
            case TERMINATED -> "已终止";
        };
    }

    private String contractApprovalStatusText(ContractApprovalStatus status) {
        if (status == null) {
            return "未提交";
        }
        return switch (status) {
            case NONE -> "未提交";
            case PENDING -> "审批中";
            case APPROVED -> "已通过";
            case REJECTED -> "已驳回";
        };
    }

    /** 报销票据附件：只取单文件，扩展名无法识别时按图片处理。 */
    private List<BizAttachmentVO> receiptAttachments(String filePath) {
        BizAttachmentVO attachment = attachment(filePath, null);
        if (attachment == null) {
            return List.of();
        }
        if (ATTACHMENT_KIND_OTHER.equals(attachment.getKind())) {
            attachment.setKind(ATTACHMENT_KIND_IMAGE);
        }
        return List.of(attachment);
    }

    /** 合同附件：filePath 为存储名，fileName 为原始展示名。 */
    private List<BizAttachmentVO> contractAttachments(String filePath, String fileName) {
        BizAttachmentVO attachment = attachment(filePath, fileName);
        return attachment == null ? List.of() : List.of(attachment);
    }

    /**
     * 解析报修图片 JSON 数组。
     *
     * <p>元素可能是存储名或 /api/v1/files/xxx 路径；非法 JSON 只记录告警并忽略，不影响业务详情。
     */
    private List<BizAttachmentVO> parseImageAttachments(String imagesJson) {
        if (!StringUtils.hasText(imagesJson)) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(imagesJson);
            if (root == null || !root.isArray()) {
                return List.of();
            }
            List<BizAttachmentVO> attachments = new ArrayList<>();
            for (JsonNode element : root) {
                if (element == null || element.isNull()) {
                    continue;
                }
                // 兼容历史数据中的纯字符串数组；非文本元素按字符串兜底处理
                BizAttachmentVO attachment = attachment(element.asText(null), null);
                if (attachment != null) {
                    attachments.add(attachment);
                }
            }
            return attachments;
        } catch (Exception ex) {
            // 历史数据可能不是合法 JSON，只记录告警，绝不因为附件解析失败影响审批详情
            log.warn(
                    "业务详情图片 JSON 解析失败，已忽略该附件分组：{}，原因：{}",
                    imagesJson,
                    ex.getMessage());
            return List.of();
        }
    }

    private BizAttachmentVO attachment(String rawName, String displayName) {
        String name = normalizeStorageName(rawName);
        if (!StringUtils.hasText(name)) {
            return null;
        }
        String fileName = StringUtils.hasText(displayName) ? displayName : name;
        return new BizAttachmentVO(name, fileName, attachmentKind(name));
    }

    /** 兼容完整访问路径与带查询串的地址，只保留最后一段存储名。 */
    private String normalizeStorageName(String rawName) {
        if (!StringUtils.hasText(rawName)) {
            return null;
        }
        String value = rawName.trim();
        int queryIndex = value.indexOf('?');
        if (queryIndex >= 0) {
            value = value.substring(0, queryIndex);
        }
        int slashIndex = value.lastIndexOf('/');
        if (slashIndex >= 0) {
            value = value.substring(slashIndex + 1);
        }
        return StringUtils.hasText(value) ? value : null;
    }

    private String attachmentKind(String name) {
        int dotIndex = name.lastIndexOf('.');
        String extension =
                dotIndex >= 0 ? name.substring(dotIndex + 1).toLowerCase(Locale.ROOT) : "";
        return switch (extension) {
            case "jpg", "jpeg", "png", "webp", "gif", "bmp" -> ATTACHMENT_KIND_IMAGE;
            case "pdf" -> ATTACHMENT_KIND_PDF;
            case "doc", "docx", "xls", "xlsx", "ppt", "pptx" -> ATTACHMENT_KIND_OFFICE;
            default -> ATTACHMENT_KIND_OTHER;
        };
    }

    private void addAttachmentGroup(
            BizDetailVO detail, String groupName, List<BizAttachmentVO> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        BizAttachmentGroupVO group = new BizAttachmentGroupVO();
        group.setGroupName(groupName);
        group.setItems(attachments);
        detail.getAttachmentGroups().add(group);
    }

    private String receiptTypeText(ReceiptType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case TRANSPORTATION -> "交通费";
            case ACCOMMODATION -> "住宿费";
            case MEAL -> "餐饮费";
            case OFFICE -> "办公费";
            case OTHER -> "其他";
        };
    }

    private String receiptApprovalText(ReceiptApprovalStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case DRAFT -> "草稿";
            case PENDING -> "审批中";
            case APPROVED -> "已通过";
            case REJECTED -> "已驳回";
            case CANCELLED -> "已取消";
        };
    }

    private String paymentStatusText(PaymentStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case UNPAID -> "未支付";
            case PARTIAL -> "部分支付";
            case PAID -> "已支付";
        };
    }

    private String approvalStatusText(ApprovalStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case DRAFT -> "草稿";
            case PENDING -> "审批中";
            case APPROVED -> "已通过";
            case REJECTED -> "已驳回";
            case CANCELLED -> "已终止";
        };
    }

    private String repairStatusText(RepairStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case SUBMITTED -> "已提交";
            case ASSIGNED -> "已受理";
            case PROCESSING -> "处理中";
            case DONE -> "已完工";
            case ACCEPTED -> "已验收";
            case REJECTED -> "已驳回";
        };
    }

    private String repairCategoryText(RepairCategory category) {
        if (category == null) {
            return null;
        }
        return switch (category) {
            case ELECTRIC -> "电气";
            case WATER -> "给排水";
            case FURNITURE -> "家具";
            case IT -> "信息技术";
            case OTHER -> "其他";
        };
    }

    private String hrApprovalText(HrApprovalStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case PENDING -> "审批中";
            case APPROVED -> "已通过";
            case REJECTED -> "已驳回";
            case WITHDRAWN -> "已撤回";
            case CANCELLED, TERMINATED -> "已终止";
        };
    }

    private String salaryStatusText(SalaryChangeStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case DRAFT -> "草稿";
            case APPROVING -> "审批中";
            case APPROVED -> "已通过";
            case REJECTED -> "已驳回";
        };
    }

    private String changeTypeText(ChangeType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case REGULAR -> "转正";
            case TRANSFER -> "调岗";
            case RESIGN -> "离职";
            case PROMOTION -> "晋升";
            case LEADER_TRANSFER -> "上级转交";
        };
    }

    private String employeeChangeTitle(ChangeType type) {
        if (type == null) {
            return "入转调离申请";
        }
        return switch (type) {
            case REGULAR -> "转正申请";
            case TRANSFER -> "调岗申请";
            case RESIGN -> "离职申请";
            case PROMOTION -> "晋升申请";
            case LEADER_TRANSFER -> "上级转交申请";
        };
    }

    private String salaryChangeTypeText(SalaryChangeType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case ADJUST -> "薪资调整";
            case PROMOTION -> "晋升调薪";
            case CORRECTION -> "数据更正";
        };
    }

    private String checkinTypeText(CheckinType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case IN -> "上班打卡";
            case OUT -> "下班打卡";
        };
    }

    @Override
    public PageResult<InstanceVO> pageByInitiator(
            long page, long size, BpmInstanceStatus status, String bizType) {
        Long initiatorId = securityGuard.currentUserId();
        if (initiatorId == null) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "未登录不可查询");
        }
        Page<BpmInstance> p = new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 100));
        LambdaQueryWrapper<BpmInstance> wrapper = new LambdaQueryWrapper<>();
        // 数据范围：SELF=仅本人；DEPT=同部门及子部门用户；ALL=管理员不限制
        applyInitiatorScope(wrapper, initiatorId);
        wrapper.eq(status != null, BpmInstance::getStatus, status);
        wrapper.eq(StringUtils.hasText(bizType), BpmInstance::getBizType, bizType);
        wrapper.orderByDesc(BpmInstance::getCreateTime);
        IPage<BpmInstance> result = instanceMapper.selectPage(p, wrapper);
        List<BpmInstance> records = result.getRecords();
        if (records.isEmpty()) {
            return new PageResult<>(List.of(), result.getTotal(), result.getCurrent(), result.getSize());
        }
        List<Long> ids = records.stream().map(BpmInstance::getId).toList();
        Map<Long, List<BpmNode>> nodeMap =
                nodeMapper
                        .selectList(
                                new LambdaQueryWrapper<BpmNode>()
                                        .in(BpmNode::getInstanceId, ids)
                                        .orderByAsc(BpmNode::getSeqNo))
                        .stream()
                        .collect(Collectors.groupingBy(BpmNode::getInstanceId));
        Map<Long, List<BpmTask>> taskMap =
                taskMapper
                        .selectList(new LambdaQueryWrapper<BpmTask>().in(BpmTask::getInstanceId, ids))
                        .stream()
                        .collect(Collectors.groupingBy(BpmTask::getInstanceId));
        List<InstanceVO> vos =
                records.stream()
                        .map(
                                i ->
                                        assembler.toInstanceVO(
                                                i,
                                                nodeMap.getOrDefault(i.getId(), List.of()),
                                                taskMap.getOrDefault(i.getId(), List.of())))
                        .toList();
        return new PageResult<>(vos, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long id, InstanceActionRequest request) {
        BpmInstance instance = requireInstance(id);
        if (instance.getStatus() == BpmInstanceStatus.WITHDRAWN) {
            // 重复撤回：返回首次结果
            return;
        }
        if (instance.getStatus() != BpmInstanceStatus.RUNNING) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅运行中实例可撤回");
        }
        Long current = securityGuard.currentUserId();
        if (current == null || !current.equals(instance.getInitiatorId())) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "仅发起人可撤回");
        }
        requireInstanceVersion(instance, request.getInstanceVersion());
        if (flowEngine.hasProcessedTask(id)) {
            throw BusinessException.of(ErrorCode.BPM_INSTANCE_CONFLICT, "首节点已处理，不可撤回");
        }
        Map<String, Object> before = snapshotInstance(instance);
        List<BpmNode> nodes = flowEngine.loadNodes(id);
        flowEngine.withdraw(instance, nodes);
        auditService.record(
                BpmAuditService.TARGET_INSTANCE,
                id,
                AuditAction.WITHDRAW,
                before,
                instance,
                null,
                current);
        log.info("撤回 BPM 实例：instanceId={}，operator={}，reason={}", id, current, request.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void terminate(Long id, InstanceActionRequest request) {
        securityGuard.requireAdmin();
        BpmInstance instance = requireInstance(id);
        if (instance.getStatus() == BpmInstanceStatus.TERMINATED) {
            // 重复终止：返回首次结果
            return;
        }
        if (instance.getStatus() != BpmInstanceStatus.RUNNING) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅运行中实例可终止");
        }
        requireInstanceVersion(instance, request.getInstanceVersion());
        Map<String, Object> before = snapshotInstance(instance);
        List<BpmNode> nodes = flowEngine.loadNodes(id);
        flowEngine.terminate(instance, nodes);
        auditService.record(
                BpmAuditService.TARGET_INSTANCE,
                id,
                AuditAction.TERMINATE,
                before,
                instance,
                null,
                securityGuard.currentUserId());
        log.info(
                "终止 BPM 实例：instanceId={}，operator={}，reason={}",
                id,
                securityGuard.currentUserId(),
                request.getReason());
    }

    private List<BpmNode> createNodes(BpmInstance instance, BpmSchema schema) {
        List<BpmNode> nodes = new ArrayList<>();
        for (BpmSchemaNode def : schema.getNodes()) {
            BpmNode node = new BpmNode();
            node.setInstanceId(instance.getId());
            node.setNodeKey(def.getNodeKey());
            node.setNodeType(def.getNodeType());
            node.setSeqNo(def.getSeqNo());
            node.setStatus(BpmNodeStatus.PENDING);
            nodeMapper.insert(node);
            nodes.add(node);
        }
        return nodes;
    }

    /** 写入流程变量：系统变量 biz_id/biz_type/template_code/initiator_id 优先，用户变量不得覆盖。 */
    private void saveVariables(
            BpmInstance instance, BpmTemplate template, Map<String, String> userVariables) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("biz_id", Objects.toString(instance.getBizId(), ""));
        variables.put("biz_type", instance.getBizType());
        variables.put("template_code", template.getCode());
        variables.put("initiator_id", String.valueOf(instance.getInitiatorId()));
        if (userVariables != null) {
            userVariables.forEach((key, value) -> {
                if (StringUtils.hasText(key)) {
                    variables.putIfAbsent(key, value);
                }
            });
        }
        variables.forEach(
                (key, value) -> {
                    BpmVariable variable = new BpmVariable();
                    variable.setInstanceId(instance.getId());
                    variable.setVarKey(key);
                    variable.setVarValue(value);
                    variableMapper.insert(variable);
                });
    }

    @Override
    public FlowPreviewVO preview(FlowPreviewRequest request) {
        BpmTemplate template =
                templateMapper.selectOne(
                        new LambdaQueryWrapper<BpmTemplate>()
                                .eq(BpmTemplate::getCode, request.getTemplateCode()));
        if (template == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "流程模板不存在");
        }
        if (template.getStatus() != TemplateStatus.ENABLED || template.getCurrentVersionId() == null) {
            throw BusinessException.of(ErrorCode.BPM_TEMPLATE_NOT_ENABLED, "流程模板未启用，不可预览");
        }
        BpmTemplateVersion version = versionMapper.selectById(template.getCurrentVersionId());
        if (version == null || version.getStatus() != TemplateVersionStatus.PUBLISHED) {
            throw BusinessException.of(ErrorCode.BPM_TEMPLATE_NOT_ENABLED, "流程模板当前版本未发布");
        }
        Long initiatorId = securityGuard.currentUserId();
        if (initiatorId == null) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "未登录不可预览流程");
        }
        String bizType =
                StringUtils.hasText(request.getBizType()) ? request.getBizType() : template.getBizType();
        BpmBizType bizTypeEnum = BpmBizType.from(bizType);
        if (bizTypeEnum == null || !Objects.equals(bizType, template.getBizType())) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "业务类型与模板不一致或不受支持：" + bizType);
        }
        securityGuard.requireBizPermission(bizType);

        // 发起前业务单据尚未创建时，可按目标员工账号解析其部门负责人/直属上级
        Long subjectUserId =
                request.getSubjectUserId() != null ? request.getSubjectUserId() : initiatorId;

        BpmSchema schema = schemaParser.parse(version.getSchemaJson());
        Set<Long> userIds = new HashSet<>();
        List<PreviewNodeVO> previewNodes = new ArrayList<>();
        List<List<BpmResolvedAssignee>> resolvedNodes = new ArrayList<>();
        for (BpmSchemaNode def : schema.getNodes()) {
            List<BpmResolvedAssignee> resolved =
                    assigneeResolver.resolve(def, subjectUserId, bizTypeEnum, request.getBizId());
            resolved.forEach(item -> userIds.add(item.getAssigneeId()));
            resolvedNodes.add(resolved);
            PreviewNodeVO node = new PreviewNodeVO();
            node.setNodeKey(def.getNodeKey());
            node.setNodeName(def.getNodeName());
            node.setNodeType(def.getNodeType());
            node.setSeqNo(def.getSeqNo());
            if (def.getNodeType() != BpmNodeType.START && def.getNodeType() != BpmNodeType.END) {
                node.setAssigneeRule(ruleText(def));
            }
            previewNodes.add(node);
        }
        Map<Long, String> userNames = loadUserNames(userIds);
        for (int i = 0; i < previewNodes.size(); i++) {
            previewNodes.get(i).setAssignees(toFlowAssignees(resolvedNodes.get(i), userNames));
        }

        FlowPreviewVO result = new FlowPreviewVO();
        result.setTemplateCode(template.getCode());
        result.setTemplateName(template.getName());
        result.setBizType(bizType);
        result.setNodes(previewNodes);
        return result;
    }

    /** 为实例节点补节点名称与审批人姓名：已到达取任务、未到达按规则预解析。 */
    private void enrichNodes(BpmInstance instance, List<NodeVO> nodeVOs) {
        Set<Long> userIds = new HashSet<>();
        for (NodeVO node : nodeVOs) {
            if (node.getTasks() == null) {
                continue;
            }
            for (TaskVO task : node.getTasks()) {
                if (task.getAssigneeId() != null) {
                    userIds.add(task.getAssigneeId());
                }
                if (task.getOwnerAssigneeId() != null) {
                    userIds.add(task.getOwnerAssigneeId());
                }
            }
        }
        BpmTemplateVersion version =
                instance.getTemplateVersionId() == null
                        ? null
                        : versionMapper.selectById(instance.getTemplateVersionId());
        BpmSchema schema =
                version == null ? null : schemaParser.parse(version.getSchemaJson());
        BpmBizType bizType = BpmBizType.from(instance.getBizType());
        if (schema != null) {
            for (NodeVO node : nodeVOs) {
                BpmSchemaNode def = schema.findByKey(node.getNodeKey());
                if (def != null) {
                    node.setNodeName(def.getNodeName());
                    if (node.getNodeType() != BpmNodeType.START
                            && node.getNodeType() != BpmNodeType.END
                            && node.getStatus() == BpmNodeStatus.PENDING
                            && (node.getTasks() == null || node.getTasks().isEmpty())) {
                        List<BpmResolvedAssignee> resolved =
                                assigneeResolver.resolve(
                                        def,
                                        instance.getInitiatorId(),
                                        bizType,
                                        instance.getBizId());
                        resolved.forEach(item -> userIds.add(item.getAssigneeId()));
                        node.setAssignees(
                                toFlowAssignees(
                                        resolved, Map.of()));
                        node.setPredicted(true);
                        node.setAssigneeRule(ruleText(def));
                    }
                }
            }
        }
        Map<Long, String> userNames = loadUserNames(userIds);
        for (NodeVO node : nodeVOs) {
            if (node.getTasks() != null) {
                for (TaskVO task : node.getTasks()) {
                    task.setAssigneeName(userNames.get(task.getAssigneeId()));
                    task.setOwnerAssigneeName(userNames.get(task.getOwnerAssigneeId()));
                }
            }
            if (node.getTasks() != null && !node.getTasks().isEmpty()) {
                node.setAssignees(taskAssignees(node.getTasks()));
                node.setPredicted(false);
                // 已到达但任务无人（PENDING_ASSIGNEE）时展示来源规则，避免审批人区留空
                if (node.getAssignees().isEmpty() && schema != null) {
                    BpmSchemaNode def = schema.findByKey(node.getNodeKey());
                    if (def != null) {
                        node.setAssigneeRule(ruleText(def));
                    }
                }
            } else if (node.getAssignees() != null) {
                node.getAssignees().forEach(item -> item.setName(userNames.get(item.getId())));
            }
        }
    }

    /** 任务审批人去重转换。 */
    private List<FlowAssigneeVO> taskAssignees(List<TaskVO> tasks) {
        Map<Long, FlowAssigneeVO> result = new LinkedHashMap<>();
        for (TaskVO task : tasks) {
            Long assigneeId = task.getAssigneeId();
            if (assigneeId == null || result.containsKey(assigneeId)) {
                continue;
            }
            FlowAssigneeVO assignee = new FlowAssigneeVO();
            assignee.setId(assigneeId);
            assignee.setName(task.getAssigneeName());
            assignee.setSourceType(task.getSourceType());
            result.put(assigneeId, assignee);
        }
        return new ArrayList<>(result.values());
    }

    /** 解析结果转视图（姓名由调用方批量回填）。 */
    private List<FlowAssigneeVO> toFlowAssignees(
            List<BpmResolvedAssignee> resolved, Map<Long, String> userNames) {
        List<FlowAssigneeVO> result = new ArrayList<>();
        for (BpmResolvedAssignee item : resolved) {
            FlowAssigneeVO assignee = new FlowAssigneeVO();
            assignee.setId(item.getAssigneeId());
            assignee.setName(userNames.get(item.getAssigneeId()));
            assignee.setSourceType(item.getSourceType());
            result.add(assignee);
        }
        return result;
    }

    /** 审批人来源规则文案，解析不到个人时用于展示。 */
    private String ruleText(BpmSchemaNode def) {
        if (def.getAssignees() == null || def.getAssignees().isEmpty()) {
            return "未配置审批人";
        }
        List<String> parts = new ArrayList<>();
        def.getAssignees()
                .forEach(
                        source -> {
                            if (source.getType() == null) {
                                return;
                            }
                            switch (source.getType()) {
                                case USER -> parts.add("指定审批人");
                                case ROLE ->
                                        parts.add(
                                                StringUtils.hasText(source.getValue())
                                                        ? "角色：" + source.getValue()
                                                        : "角色审批");
                                case DEPT_LEADER -> parts.add("部门负责人");
                                case SUPERIOR -> parts.add("直属上级");
                                default -> parts.add(source.getType().name());
                            }
                        });
        return String.join("、", parts);
    }

    private Map<Long, String> loadUserNames(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> names = new HashMap<>();
        for (SysUser user : userMapper.selectBatchIds(userIds)) {
            names.put(user.getId(), user.getName());
        }
        return names;
    }

    private BpmInstance requireInstance(Long id) {
        BpmInstance instance = instanceMapper.selectById(id);
        if (instance == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "流程实例不存在");
        }
        return instance;
    }

    private void requireInstanceVersion(BpmInstance instance, Integer requestVersion) {
        if (requestVersion == null || !requestVersion.equals(instance.getVersion())) {
            throw BusinessException.of(ErrorCode.BPM_INSTANCE_CONFLICT, "实例版本已变更，请刷新后重试");
        }
    }

    /** 详情可见性：管理员、发起人、任务处理人、抄送接收人才可查看。 */
    private void assertVisible(BpmInstance instance) {
        if (securityGuard.isAdmin()) {
            return;
        }
        Long current = securityGuard.currentUserId();
        if (current == null) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "未登录不可查看");
        }
        if (current.equals(instance.getInitiatorId())) {
            return;
        }
        // DEPT 数据范围：同部门及子部门发起人的实例可见
        if (securityGuard.resolveDataScope() == BpmDataScope.DEPT) {
            List<Long> visible = securityGuard.visibleUserIds();
            if (visible != null && visible.contains(instance.getInitiatorId())) {
                return;
            }
        }
        long taskCount =
                taskMapper.selectCount(
                        new LambdaQueryWrapper<BpmTask>()
                                .eq(BpmTask::getInstanceId, instance.getId())
                                .eq(BpmTask::getAssigneeId, current));
        if (taskCount > 0) {
            return;
        }
        long ccCount =
                ccRecordMapper.selectCount(
                        new LambdaQueryWrapper<BpmCcRecord>()
                                .eq(BpmCcRecord::getInstanceId, instance.getId())
                                .eq(BpmCcRecord::getReceiverId, current));
        if (ccCount > 0) {
            return;
        }
        throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "无权查看该流程实例");
    }

    /** 我发起的数据范围：ALL 不过滤；DEPT 同部门及子部门用户；SELF（含范围未知）仅本人。 */
    private void applyInitiatorScope(LambdaQueryWrapper<BpmInstance> wrapper, Long initiatorId) {
        BpmDataScope scope = securityGuard.resolveDataScope();
        if (scope == BpmDataScope.ALL) {
            return;
        }
        if (scope == BpmDataScope.DEPT) {
            List<Long> visible = securityGuard.visibleUserIds();
            if (visible == null || visible.isEmpty()) {
                wrapper.eq(BpmInstance::getInitiatorId, initiatorId);
                return;
            }
            wrapper.in(BpmInstance::getInitiatorId, visible);
            return;
        }
        wrapper.eq(BpmInstance::getInitiatorId, initiatorId);
    }

    private Map<String, Object> snapshotInstance(BpmInstance instance) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("id", instance.getId());
        before.put("status", instance.getStatus());
        before.put("version", instance.getVersion());
        before.put("initiatorId", instance.getInitiatorId());
        before.put("bizType", instance.getBizType());
        before.put("bizId", instance.getBizId());
        before.put("endedAt", instance.getEndedAt());
        return before;
    }
}
