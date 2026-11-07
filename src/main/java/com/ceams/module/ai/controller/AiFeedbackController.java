package com.ceams.module.ai.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.module.ai.dto.AiFeedbackRequest;
import com.ceams.module.ai.service.AiConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** AI 消息评价接口。 */
@RestController
@RequestMapping("/api/v1/ai/feedback")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AiFeedbackController {

    private final AiConversationService aiConversationService;

    @PostMapping
    public ApiResponse<Void> feedback(@Valid @RequestBody AiFeedbackRequest request) {
        aiConversationService.feedback(request);
        return ApiResponse.success();
    }
}
