package com.ceams.module.ai.controller;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.ApiResponse;
import com.ceams.module.ai.dto.AiSummaryGenerateRequest;
import com.ceams.module.ai.service.AiDailySummaryService;
import com.ceams.module.ai.vo.AiDailySummaryVO;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** AI 经营日报摘要接口（仅 SYS_ADMIN / FINANCE）。 */
@RestController
@RequestMapping("/api/v1/ai/summaries")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
public class AiSummaryController {

    private final AiDailySummaryService aiDailySummaryService;

    /** 最近 N 条摘要（summaryDate 倒序）。 */
    @GetMapping
    public ApiResponse<List<AiDailySummaryVO>> list(@RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(aiDailySummaryService.list(limit));
    }

    /** 最新一条摘要。 */
    @GetMapping("/latest")
    public ApiResponse<AiDailySummaryVO> latest() {
        AiDailySummaryVO vo = aiDailySummaryService.latest();
        if (vo == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "暂无日报摘要");
        }
        return ApiResponse.success(vo);
    }

    /** 指定日期的摘要。 */
    @GetMapping("/{date}")
    public ApiResponse<AiDailySummaryVO> byDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        AiDailySummaryVO vo = aiDailySummaryService.byDate(date);
        if (vo == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "暂无日报摘要");
        }
        return ApiResponse.success(vo);
    }

    /** 手动生成（默认昨日；同日已存在则覆盖更新）。 */
    @PostMapping("/generate")
    public ApiResponse<AiDailySummaryVO> generate(
            @RequestBody(required = false) AiSummaryGenerateRequest request) {
        LocalDate date = request == null ? null : request.getDate();
        return ApiResponse.success(
                aiDailySummaryService.generate(date, AiDailySummaryService.TriggerType.MANUAL));
    }
}
