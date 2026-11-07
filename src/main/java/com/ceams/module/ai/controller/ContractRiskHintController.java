package com.ceams.module.ai.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.module.ai.service.ContractRiskHintService;
import com.ceams.module.ai.vo.ContractRiskHintVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 合同要点与风险提示接口（只读提示，不修改合同）。 */
@RestController
@RequestMapping("/api/v1/ai/contracts")
@RequiredArgsConstructor
public class ContractRiskHintController {

    private final ContractRiskHintService contractRiskHintService;

    /** 单份分包合同的要点与风险提示；合同不存在返回 BIZ_NOT_FOUND。 */
    @GetMapping("/{id}/risk-hints")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ContractRiskHintVO> riskHints(@PathVariable Long id) {
        return ApiResponse.success(contractRiskHintService.riskHints(id));
    }
}
