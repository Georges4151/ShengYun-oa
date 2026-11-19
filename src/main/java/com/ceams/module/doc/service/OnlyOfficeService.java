package com.ceams.module.doc.service;

import com.ceams.module.doc.vo.EditorConfigVO;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

/** OnlyOffice 在线编辑服务：编辑器配置、原件取件、保存回调。 */
public interface OnlyOfficeService {

    /** 构建编辑器配置（含整份 config 的 JWT 签名）；不可用时抛 503 语义异常。 */
    EditorConfigVO buildEditorConfig(Long fileId);

    /**
     * 供 Document Server 拉取原件（token 鉴权，不消耗一次性 nonce）。
     *
     * @param fileId 路径上携带的文件 ID（当前接口无路径参数，可传 null 由 token 内 fid 决定）
     * @param token onlyoffice-file 短时令牌
     * @param response 输出流
     */
    void serveDocument(Long fileId, String token, HttpServletResponse response);

    /** 保存回调（Document Server 发起，JWT 鉴权）；返回 OnlyOffice 约定的 {"error":0/1}。 */
    Map<String, Integer> callback(String authorizationHeader, String body);
}
