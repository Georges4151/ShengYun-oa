package com.ceams.service.impl;

import com.ceams.service.PdfWatermarkService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.awt.geom.AffineTransform;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * PDF水印服务实现
 * 
 * 使用iText在PDF每一页添加半透明斜向水印文字。
 * 水印内容格式：仅供审批使用 | 用户名 | 时间戳
 */
@Service
public class PdfWatermarkServiceImpl implements PdfWatermarkService {

    private static final Logger logger = LoggerFactory.getLogger(PdfWatermarkServiceImpl.class);

    // 水印临时文件存储目录
    private static final String WATERMARK_TEMP_DIR = "./watermark_previews";

    // 临时文件过期时间（30分钟）
    private static final long EXPIRY_MINUTES = 30;

    // 中文字体路径（Windows系统）
    private static final String FONT_PATH = "C:\\\\Windows\\\\Fonts\\\\simsun.ttc,1";

    @Value("${ceams.watermark.font-path:}")
    private String configuredFontPath;

    private BaseFont baseFont;

    public PdfWatermarkServiceImpl() {
        initFont();
    }

    private void initFont() {
        try {
            String fontToUse = (configuredFontPath != null && !configuredFontPath.isEmpty())
                    ? configuredFontPath : FONT_PATH;
            baseFont = BaseFont.createFont(fontToUse, BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
            logger.info("PDF水印字体初始化成功: {}", fontToUse);
        } catch (Exception e) {
            logger.warn("无法加载中文字体，将使用默认字体: {}", e.getMessage());
            try {
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
            } catch (Exception ex) {
                logger.error("字体初始化完全失败", ex);
            }
        }
    }

    @Override
    public byte[] addWatermarkToPdf(String originalFilePath, String username) throws IOException {
        try (FileInputStream fis = new FileInputStream(originalFilePath);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PdfReader reader = new PdfReader(fis);
            PdfStamper stamper = new PdfStamper(reader, baos);

            int totalPages = reader.getNumberOfPages();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            String watermarkText = "仅供审批使用 | " + username + " | " + sdf.format(new Date());

            logger.debug("添加水印: {}, 共{}页", watermarkText, totalPages);

            // 为每一页添加水印
            for (int i = 1; i <= totalPages; i++) {
                addWatermarkToPage(stamper, i, watermarkText);
            }

            stamper.close();
            reader.close();

            byte[] result = baos.toByteArray();
            logger.info("水印添加成功，输出大小: {} bytes", result.length);
            return result;

        } catch (Exception e) {
            throw new IOException("添加PDF水印失败: " + e.getMessage(), e);
        }
    }

    @Override
    public File addWatermarkToPdfFile(String originalFilePath, String outputFilePath, String username)
            throws IOException {
        File outputFile = new File(outputFilePath);
        File parentDir = outputFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileInputStream fis = new FileInputStream(originalFilePath);
             FileOutputStream fos = new FileOutputStream(outputFile)) {

            PdfReader reader = new PdfReader(fis);
            PdfStamper stamper = new PdfStamper(reader, fos);

            int totalPages = reader.getNumberOfPages();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            String watermarkText = "仅供审批使用 | " + username + " | " + sdf.format(new Date());

            for (int i = 1; i <= totalPages; i++) {
                addWatermarkToPage(stamper, i, watermarkText);
            }

            stamper.close();
            reader.close();

            logger.info("水印文件已保存: {} ({})", outputFilePath, outputFile.length());
            return outputFile;

        } catch (Exception e) {
            throw new IOException("生成水印PDF文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 在指定页面添加水印
     */
    private void addWatermarkToPage(PdfStamper stamper, int pageNum, String watermarkText) {
        try {
            PdfContentByte content = stamper.getOverContent(pageNum);
            Rectangle pageSize = stamper.getReader().getPageSizeWithRotation(pageNum);

            float pageWidth = pageSize.getWidth();
            float pageHeight = pageSize.getHeight();

            if (baseFont == null) {
                logger.warn("BaseFont未初始化，跳过第{}页水印", pageNum);
                return;
            }

            // 水印文字样式
            Font font = new Font(baseFont, 14f, Font.NORMAL, new BaseColor(255, 70, 70, 110)); // 半透明红色

            Phrase phrase = new Phrase(watermarkText, font);

            // 获取文本宽度用于计算间距
            float textWidth = ColumnText.getWidth(phrase);

            // 网格状排列水印（每页约6x4=24个）
            int cols = 3;
            int rows = 5;
            float colSpacing = pageWidth / cols;
            float rowSpacing = pageHeight / rows;

            PdfGState gs = new PdfGState();
            gs.setFillOpacity(0.35f); // 整体透明度
            content.setGState(gs);

            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    float x = col * colSpacing + 20;
                    float y = pageHeight - (row * rowSpacing) - 80;

                    // 创建带旋转的文本模板
                    ColumnText ct = new ColumnText(content);
                    ct.setSimpleColumn(phrase, x, y - 10, x + textWidth + 50, y + 20, 12, Element.ALIGN_LEFT);
                    ct.setRunDirection(PdfWriter.RUN_DIRECTION_LTR);

                    // 旋转-30度
                    AffineTransform at = AffineTransform.getRotateInstance(
                            Math.toRadians(-30), x + textWidth / 2, y + 5);
                    content.saveState();
                    content.concatCTM(at);
                    ct.go();
                    content.restoreState();
                }
            }

        } catch (Exception e) {
            logger.error("第{}页添加水印失败", pageNum, e);
        }
    }

    @Override
    @Scheduled(fixedRate = 30 * 60 * 1000) // 每30分钟执行一次
    public void cleanupExpiredWatermarks() {
        try {
            File dir = new File(WATERMARK_TEMP_DIR);
            if (!dir.exists() || !dir.isDirectory()) {
                return;
            }

            File[] files = dir.listFiles();
            if (files == null || files.length == 0) {
                return;
            }

            long currentTime = System.currentTimeMillis();
            long expiryTime = EXPIRY_MINUTES * 60 * 1000;
            int deletedCount = 0;

            for (File file : files) {
                if (currentTime - file.lastModified() > expiryTime) {
                    if (file.delete()) {
                        deletedCount++;
                    }
                }
            }

            if (deletedCount > 0) {
                logger.info("清理了{}个过期的临时水印文件", deletedCount);
            }
        } catch (Exception e) {
            logger.error("清理临时水印文件失败", e);
        }
    }
}
