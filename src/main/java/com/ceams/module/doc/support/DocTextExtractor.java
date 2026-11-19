package com.ceams.module.doc.support;

import com.ceams.module.doc.config.DocProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.WriteOutContentHandler;
import org.springframework.stereotype.Component;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * 文档文本抽取（Tika 3.x），只支持可读文本类扩展名。
 *
 * <p>要点：
 *
 * <ol>
 *   <li>只解析白名单扩展名（不支持的返回 {@code null}，不抛异常），避免把图片 / 压缩包等
 *       无效语料写入知识库。
 *   <li>抽取上限取 {@code ceams.doc.rag.max-extract-chars}；Tika 达到上限会抛
 *       {@code WriteLimitReachedException}，此时仍取已写入的文本作为截断结果。
 *   <li>结果归一化：{@code \r\n -> \n}，连续 3 个以上换行压成 2 个，首尾 trim；空文本返回
 *       {@code null}。
 * </ol>
 *
 * <p>本类只读物理文件，返回内容为纯文本；调用方禁止把 storageKey / 物理路径回传前端或模型。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocTextExtractor {

    /** 可抽取文本的扩展名（小写）。 */
    private static final Set<String> SUPPORTED_EXTENSIONS =
            Set.of("doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf", "txt", "md", "csv");

    /** 抽取上限兜底下限，避免配置为 0 导致任何内容都被截断。 */
    private static final int MIN_EXTRACT_CHARS = 1024;

    /** Tika 解析器：解析过程无状态，可复用。 */
    private final AutoDetectParser parser = new AutoDetectParser();

    private final DocProperties docProperties;

    /** 扩展名是否支持抽取（忽略大小写，非法值返回 false）。 */
    public static boolean supports(String ext) {
        if (ext == null || ext.isBlank()) {
            return false;
        }
        return SUPPORTED_EXTENSIONS.contains(ext.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * 抽取文件纯文本。
     *
     * @param path 物理文件路径（由 {@code DocStorageService.resolve} 得到）
     * @param ext 小写扩展名
     * @return 归一化文本；类型不支持、文件不存在或没有可抽取内容时返回 {@code null}
     */
    public String extract(Path path, String ext) {
        if (!supports(ext)) {
            return null;
        }
        if (path == null || !Files.isRegularFile(path)) {
            log.warn("待抽取文档不存在，跳过：{}", path);
            return null;
        }
        int maxChars = resolveMaxChars();
        WriteOutContentHandler writeOut = new WriteOutContentHandler(maxChars);
        ContentHandler handler = new BodyContentHandler(writeOut);
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();
        try (InputStream in = Files.newInputStream(path)) {
            parser.parse(in, handler, metadata, context);
        } catch (SAXException e) {
            if (!isWriteLimitReached(e)) {
                log.warn("文档文本抽取 SAX 异常，返回已解析内容：{}", path, e);
            }
        } catch (IOException | TikaException e) {
            log.warn("文档文本抽取失败，返回已解析内容：{}", path, e);
        }
        return normalize(writeOut.toString());
    }

    /** 抽取上限：配置非法（<=0）时回退默认值，且不低于 {@link #MIN_EXTRACT_CHARS}。 */
    private int resolveMaxChars() {
        int configured =
                docProperties.getRag() == null ? 0 : docProperties.getRag().getMaxExtractChars();
        if (configured <= 0) {
            configured = 2000000;
        }
        return Math.max(MIN_EXTRACT_CHARS, configured);
    }

    /** 是否因超出字符上限被 Tika 截断（异常链上出现 WriteLimitReachedException）。 */
    private boolean isWriteLimitReached(Throwable error) {
        Throwable cursor = error;
        while (cursor != null) {
            if ("WriteLimitReachedException".equals(cursor.getClass().getSimpleName())) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    /** 归一化：统一换行、压缩连续空行、去除首尾空白；空文本返回 null。 */
    private String normalize(String text) {
        if (text == null) {
            return null;
        }
        String value = text.replace("\r\n", "\n").replace('\r', '\n');
        value = value.replaceAll("\n{3,}", "\n\n");
        value = value.trim();
        return value.isEmpty() ? null : value;
    }
}
