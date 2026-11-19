package com.ceams.common.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.seal.service.impl.PdfBoxStampServiceImpl;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * PDF 盖章链路测试：使用真实 {@link PdfBoxStampServiceImpl} 与 PDFBox 现场生成的 PDF/印章图片， 验证落盘产物结构合法、页数/页面尺寸不变、
 * 原文可抽取，以及损坏输入会被显式抛出。
 */
class PdfStampServiceIntegrationTest {

    /** 生产实现无外部依赖，直接构造即可。 */
    private final PdfBoxStampServiceImpl service = new PdfBoxStampServiceImpl();

    /** 测试期间使用的临时目录。 */
    @TempDir Path tempDir;

    @Test
    void stampRealPdfProducesValidStampedFile() throws IOException {
        Path sourcePdf = tempDir.resolve("source.pdf");
        Path outputPdf = tempDir.resolve("stamped.pdf");

        // 1. 现场生成 1 页 A4 PDF（含文本，用于确认盖章后内容未被破坏）
        Files.write(sourcePdf, createA4PdfWithText());
        // 2. 生成带透明通道的 120x120 PNG 印章
        byte[] sealImage = createTransparentSealPng(120, 120);

        // 3. 调用真实实现盖章并落盘
        byte[] stamped = service.stamp(
                Files.readAllBytes(sourcePdf), new StampRequest(sealImage, null, null, 0, 1.0f));
        Files.write(outputPdf, stamped);

        // 4. 断言产物存在且非空
        assertThat(outputPdf).as("盖章产物应写入临时目录").exists();
        assertThat(Files.size(outputPdf)).as("盖章产物大小应大于 0").isGreaterThan(0L);

        // 5. 重新加载产物，校验页数、页面尺寸与文本内容
        try (PDDocument source = Loader.loadPDF(sourcePdf.toFile());
                PDDocument result = Loader.loadPDF(outputPdf.toFile())) {
            assertThat(result.getNumberOfPages()).as("盖章后页数应保持不变").isEqualTo(1);

            PDRectangle sourceBox = source.getPage(0).getMediaBox();
            PDRectangle resultBox = result.getPage(0).getMediaBox();
            assertThat(resultBox.getWidth())
                    .as("盖章后页面宽度应与原 PDF 一致")
                    .isCloseTo(sourceBox.getWidth(), offset(0.01f));
            assertThat(resultBox.getHeight())
                    .as("盖章后页面高度应与原 PDF 一致")
                    .isCloseTo(sourceBox.getHeight(), offset(0.01f));

            String text = new PDFTextStripper().getText(result);
            assertThat(text).as("盖章不应破坏原有文本内容").contains("CEAMS PDF stamp integration test");
        }
    }

    @Test
    void emptySourcePdfIsRejectedWithBusinessException() {
        assertThatThrownBy(() -> service.stamp(
                        new byte[0], new StampRequest(createTransparentSealPng(120, 120), null, null, 0, 1.0f)))
                .as("空源 PDF 应作为非法参数被拒绝")
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.VALID_ILLEGAL_ARGUMENT));
    }

    @Test
    void corruptPdfBytesAreWrappedAsBusinessException() {
        // 模拟「文件不存在/无法读取」后得到的无效内容：不是合法 PDF
        byte[] corrupt = "this is not a pdf".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> service.stamp(
                        corrupt, new StampRequest(createTransparentSealPng(120, 120), null, null, 0, 1.0f)))
                .as("损坏 PDF 应转换为业务异常，且保留 IOException 根因")
                .isInstanceOf(BusinessException.class)
                .hasCauseInstanceOf(IOException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.BIZ_UNPROCESSABLE));
    }

    // ------------------------------------------------------------------
    // 辅助方法
    // ------------------------------------------------------------------

    /** 生成 1 页 A4 PDF，页面内含一段英文文本。 */
    private byte[] createA4PdfWithText() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(72, 720);
                contentStream.showText("CEAMS PDF stamp integration test");
                contentStream.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    /** 生成带透明通道的红色圆形 PNG 印章图像。 */
    private byte[] createTransparentSealPng(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        // 半透明红色印章，覆盖透明通道场景
        graphics.setColor(new Color(200, 0, 0, 180));
        graphics.fillOval(0, 0, width, height);
        graphics.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
