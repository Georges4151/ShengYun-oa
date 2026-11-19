package com.ceams.module.ai.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.module.ai.service.AiStatusService;
import com.ceams.module.ai.vo.AiStatusVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** AI 服务状态接口。 */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AiStatusController {

    private final AiStatusService aiStatusService;

    /** 查询 AI 能力状态：未启用或 Ollama 不可达均返回正常响应，不抛 500。 */
    @GetMapping("/status")
    public ApiResponse<AiStatusVO> status() {
        return ApiResponse.success(aiStatusService.getStatus());
    }
}
