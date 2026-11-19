package com.ceams.module.ai.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.module.ai.service.AiTodoSummaryService;
import com.ceams.module.ai.vo.AiTodoSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** AI 待办摘要接口（登录即可访问，数据范围与待办列表一致）。 */
@RestController
@RequestMapping("/api/v1/ai/todo-summary")
@RequiredArgsConstructor
public class AiTodoSummaryController {

    private final AiTodoSummaryService aiTodoSummaryService;

    /** 汇总当前用户数据范围内的待办。 */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<AiTodoSummaryVO> summary() {
        return ApiResponse.success(aiTodoSummaryService.summary());
    }
}
