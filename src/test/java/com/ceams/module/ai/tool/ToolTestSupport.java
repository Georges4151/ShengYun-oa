package com.ceams.module.ai.tool;

import com.ceams.module.ai.config.AiProperties;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** 工具单测公共支撑：设置安全上下文与构造 AiProperties。 */
final class ToolTestSupport {

    static final Long USER_ID = 1001L;

    private ToolTestSupport() {}

    /** 登录并赋予角色（自动补 ROLE_ 前缀）；userId 可为 null 模拟无法识别用户。 */
    static void login(Long userId, String... roles) {
        List<SimpleGrantedAuthority> authorities =
                Arrays.stream(roles)
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /** 清空安全上下文。 */
    static void logout() {
        SecurityContextHolder.clearContext();
    }

    /** 默认 200 行上限的 AI 配置。 */
    static AiProperties properties() {
        AiProperties properties = new AiProperties();
        properties.setMaxToolRows(200);
        return properties;
    }
}
