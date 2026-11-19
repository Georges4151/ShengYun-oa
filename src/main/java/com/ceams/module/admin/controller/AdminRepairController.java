package com.ceams.module.admin.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.admin.dto.RepairAssignRequest;
import com.ceams.module.admin.dto.RepairCreateRequest;
import com.ceams.module.admin.dto.RepairFinishRequest;
import com.ceams.module.admin.dto.RepairProcessRequest;
import com.ceams.module.admin.enums.RepairStatus;
import com.ceams.module.admin.service.AdminRepairService;
import com.ceams.module.admin.vo.RepairVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 行政报修接口（统一前缀 /api/v1/admin/repairs）。 */
@RestController
@RequestMapping("/api/v1/admin/repairs")
@RequiredArgsConstructor
public class AdminRepairController {

    private final AdminRepairService repairService;

    /** 提交报修（BPM）。 */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> create(@Valid @RequestBody RepairCreateRequest request) {
        return ApiResponse.success(repairService.create(request));
    }

    /** 工单分页查询，支持 status、assigneeId。 */
    @GetMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<PageResult<RepairVO>> page(
            @RequestParam(required = false) RepairStatus status,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.success(repairService.page(status, assigneeId, page, size));
    }

    /** 本人报修工单。 */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<RepairVO>> my() {
        return ApiResponse.success(repairService.my());
    }

    /**
     * 派给我的工单（当前登录用户为维修人，按创建时间倒序）。
     *
     * <p>注意：本方法必须声明在 {@code /{id}} 之前。Spring 路由匹配虽会优先命中字面量路径
     * {@code /assigned}，但保持声明顺序可避免后续维护时误判路径冲突。
     */
    @GetMapping("/assigned")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<RepairVO>> assigned() {
        return ApiResponse.success(repairService.assigned());
    }

    /** 工单详情（仅报修人、维修人或管理员可查看）。 */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<RepairVO> detail(@PathVariable Long id) {
        return ApiResponse.success(repairService.detail(id));
    }

    /** 派单 / 改派。 */
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<Void> assign(
            @PathVariable Long id, @Valid @RequestBody RepairAssignRequest request) {
        repairService.assign(id, request);
        return ApiResponse.success();
    }

    /** 开始处理（维修人或管理员）。 */
    @PutMapping("/{id}/process")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> process(
            @PathVariable Long id, @RequestBody(required = false) RepairProcessRequest request) {
        repairService.process(id, request);
        return ApiResponse.success();
    }

    /** 完工（维修人或管理员）。 */
    @PutMapping("/{id}/finish")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> finish(
            @PathVariable Long id, @RequestBody(required = false) RepairFinishRequest request) {
        repairService.finish(id, request);
        return ApiResponse.success();
    }

    /** 验收（报修人或管理员）。 */
    @PutMapping("/{id}/accept")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> accept(@PathVariable Long id) {
        repairService.accept(id);
        return ApiResponse.success();
    }
}
