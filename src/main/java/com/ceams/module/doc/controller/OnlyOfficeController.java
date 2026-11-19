package com.ceams.module.doc.controller;

import com.ceams.module.doc.service.OnlyOfficeService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * OnlyOffice 集成接口（统一前缀 /api/v1/doc/onlyoffice）。
 *
 * <p>两个端点均为<b>匿名</b>（Security 已放行）：取件完全依赖短时 token（scope=onlyoffice-file），
 * 保存回调依赖 JWT 签名校验，Document Server 无用户登录态。
 */
@RestController
@RequestMapping("/api/v1/doc/onlyoffice")
@RequiredArgsConstructor
public class OnlyOfficeController {

    private final OnlyOfficeService onlyOfficeService;

    /** 供 Document Server 拉取原件（token 鉴权，可多次拉取）。 */
    @GetMapping("/file")
    public void file(@RequestParam("token") String token, HttpServletResponse response) {
        onlyOfficeService.serveDocument(null, token, response);
    }

    /** 保存回调：JWT 取 Authorization（去 Bearer）或 body.token。 */
    @PostMapping("/callback")
    public Map<String, Object> callback(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) String body) {
        Map<String, Integer> result = onlyOfficeService.callback(authorization, body);
        return result == null ? new LinkedHashMap<>() : new LinkedHashMap<>(result);
    }
}
