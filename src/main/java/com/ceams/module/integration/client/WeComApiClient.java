package com.ceams.module.integration.client;

import com.ceams.module.integration.config.WeComAppConfiguration;
import com.ceams.module.integration.config.WeComAppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * 企业微信应用 API 客户端。
 *
 * <p>仅封装 P2 组织同步所需接口，并预留 P3 扫码登录（{@link #getUser}）与 P4 应用消息
 * （{@link #sendAppMessage}）。所有方法统一校验 {@code errcode}：非 0 抛
 * {@link WeComApiException}，网络 / 解析异常包装为 {@code errcode=-1}。
 */
@Slf4j
@Component
public class WeComApiClient {

    /** 获取 access_token。 */
    private static final String PATH_GET_TOKEN = "/cgi-bin/gettoken";

    /** 部门列表。 */
    private static final String PATH_DEPT_LIST = "/cgi-bin/department/list";

    /** 部门成员列表。 */
    private static final String PATH_USER_LIST = "/cgi-bin/user/list";

    /** 成员详情。 */
    private static final String PATH_USER_GET = "/cgi-bin/user/get";

    /** 扫码登录授权码换取成员账号。 */
    private static final String PATH_AUTH_GET_USER_INFO = "/cgi-bin/auth/getuserinfo";

    /** 应用消息发送。 */
    private static final String PATH_MESSAGE_SEND = "/cgi-bin/message/send";

    private final WeComAppProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public WeComApiClient(
            WeComAppProperties properties,
            @Qualifier(WeComAppConfiguration.REST_CLIENT_BEAN_NAME) RestClient restClient,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    /** 获取 access_token。 */
    public AccessToken fetchAccessToken() {
        JsonNode root =
                get(
                        PATH_GET_TOKEN + "?corpid={corpid}&corpsecret={corpsecret}",
                        "获取 access_token",
                        properties.getCorpId(),
                        properties.getSecret());
        String token = text(root, "access_token");
        if (!StringUtils.hasText(token)) {
            throw new WeComApiException(-1, "企业微信未返回 access_token");
        }
        int expiresIn = root.path("expires_in").asInt(7200);
        return new AccessToken(token, expiresIn);
    }

    /** 拉取全部部门（含根部门）。 */
    public List<WeComDept> listDept(String token) {
        JsonNode root =
                get(PATH_DEPT_LIST + "?access_token={token}", "获取部门列表", requireToken(token));
        List<WeComDept> depts = new ArrayList<>();
        for (JsonNode node : root.path("department")) {
            depts.add(
                    new WeComDept(
                            longValue(node, "id"),
                            text(node, "name"),
                            longValue(node, "parentid"),
                            intValue(node, "order")));
        }
        return depts;
    }

    /** 拉取指定部门的直属成员（不递归子部门）。 */
    public List<WeComUser> listDeptUsers(String token, Long deptId) {
        JsonNode root =
                get(
                        PATH_USER_LIST
                                + "?access_token={token}&department_id={deptId}&fetch_child=0",
                        "获取部门成员",
                        requireToken(token),
                        deptId);
        List<WeComUser> users = new ArrayList<>();
        for (JsonNode node : root.path("userlist")) {
            users.add(toUser(node));
        }
        return users;
    }

    /** 查询单个成员详情（P3 扫码登录使用）。 */
    public WeComUser getUser(String token, String userid) {
        JsonNode root =
                get(
                        PATH_USER_GET + "?access_token={token}&userid={userid}",
                        "获取成员详情",
                        requireToken(token),
                        userid);
        return toUser(root);
    }

    /**
     * 使用扫码登录授权码换取企业微信成员账号（P3 扫码登录使用）。
     *
     * <p>非企业成员（如仅返回 openid 的外部联系人）不会返回 {@code userid}，此时抛出
     * {@link WeComApiException}（errcode=-1），由上层转换为「授权失败」提示。
     *
     * @param token access_token
     * @param code 登录页回调带回的授权码
     * @return 企业微信成员账号
     */
    public String getUserIdByCode(String token, String code) {
        JsonNode root =
                get(
                        PATH_AUTH_GET_USER_INFO + "?access_token={token}&code={code}",
                        "获取扫码登录成员信息",
                        requireToken(token),
                        code);
        String userid = text(root, "userid");
        if (!StringUtils.hasText(userid)) {
            throw new WeComApiException(-1, "企业微信授权失败：未获取到成员账号");
        }
        return userid;
    }

    /** 发送文本应用消息（P4 发件箱调度使用）。 */
    public void sendAppMessage(String token, String toUser, String content) {
        Map<String, Object> text = new LinkedHashMap<>();
        text.put("content", content == null ? "" : content);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("touser", toUser);
        payload.put("msgtype", "text");
        payload.put("agentid", agentIdValue());
        payload.put("text", text);
        payload.put("safe", 0);
        post(
                PATH_MESSAGE_SEND + "?access_token={token}",
                payload,
                "发送应用消息",
                requireToken(token));
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    private WeComUser toUser(JsonNode node) {
        List<Long> departmentIds = new ArrayList<>();
        for (JsonNode deptId : node.path("department")) {
            Long value = parseLong(deptId.asText(null));
            if (value != null) {
                departmentIds.add(value);
            }
        }
        return new WeComUser(
                text(node, "userid"),
                text(node, "name"),
                text(node, "mobile"),
                text(node, "email"),
                intValue(node, "status"),
                departmentIds);
    }

    private JsonNode get(String path, String action, Object... uriVariables) {
        try {
            String body =
                    restClient
                            .get()
                            .uri(baseUrl() + path, uriVariables)
                            .retrieve()
                            .body(String.class);
            return readAndCheck(body, action);
        } catch (WeComApiException e) {
            throw e;
        } catch (Exception e) {
            throw new WeComApiException(-1, action + "失败：" + e.getMessage(), e);
        }
    }

    private JsonNode post(String path, Object payload, String action, Object... uriVariables) {
        try {
            String body =
                    restClient
                            .post()
                            .uri(baseUrl() + path, uriVariables)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(payload)
                            .retrieve()
                            .body(String.class);
            return readAndCheck(body, action);
        } catch (WeComApiException e) {
            throw e;
        } catch (Exception e) {
            throw new WeComApiException(-1, action + "失败：" + e.getMessage(), e);
        }
    }

    private JsonNode readAndCheck(String body, String action) {
        if (!StringUtils.hasText(body)) {
            throw new WeComApiException(-1, action + "返回空响应");
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (IOException e) {
            throw new WeComApiException(-1, action + "响应解析失败：" + e.getMessage(), e);
        }
        int errcode = root.has("errcode") ? root.path("errcode").asInt(-1) : -1;
        if (errcode != 0) {
            String errmsg = text(root, "errmsg");
            if (!StringUtils.hasText(errmsg)) {
                errmsg = action + "失败（errcode=" + errcode + "）";
            }
            log.warn("企业微信接口调用失败：action={}，errcode={}，errmsg={}", action, errcode, errmsg);
            throw new WeComApiException(errcode, errmsg);
        }
        return root;
    }

    private String baseUrl() {
        String base = properties.getApiBaseUrl();
        if (!StringUtils.hasText(base)) {
            return "https://qyapi.weixin.qq.com";
        }
        String trimmed = base.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private String requireToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new WeComApiException(-1, "企业微信 access_token 为空");
        }
        return token;
    }

    private Object agentIdValue() {
        String raw = properties.getAgentId();
        if (!StringUtils.hasText(raw)) {
            return 0;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return raw.trim();
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private Long longValue(JsonNode node, String field) {
        return parseLong(text(node, field));
    }

    private Integer intValue(JsonNode node, String field) {
        String value = text(node, field);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
