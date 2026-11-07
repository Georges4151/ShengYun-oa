package com.ceams.module.ai.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.ai.service.AiConversationService;
import com.ceams.module.ai.vo.AiConversationVO;
import com.ceams.module.ai.vo.AiMessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 我的 AI 会话与消息接口。 */
@RestController
@RequestMapping("/api/v1/ai/conversations")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AiConversationController {

    private final AiConversationService aiConversationService;

    /** 当前用户的会话列表（按更新时间倒序）。 */
    @GetMapping
    public ApiResponse<PageResult<AiConversationVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.success(aiConversationService.conversations(page, size));
    }

    /** 指定会话的消息列表（按时间正序；非本人会话返回 404）。 */
    @GetMapping("/{id}/messages")
    public ApiResponse<PageResult<AiMessageVO>> messages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "50") long size) {
        return ApiResponse.success(aiConversationService.messages(id, page, size));
    }
}
