package com.ceams.module.seal.service.impl;

import com.ceams.common.pdf.StampRequest;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PdfBoxStampServiceImpl} 单元测试：默认居中、指定坐标、多页叠加，
 * 并断言产出字节非空且 PDFBox 能重新加载。
 */
class PdfBoxStampServiceImplTest {

    private final PdfBoxStampServiceImpl service = new PdfBoxStampServiceImpl();

    @Test
    void 无坐标默认居中盖章() throws IOException {
        byte[] pdf = createPdf(1);
        byte[] seal = createSealPng();

        byte[] stamped = service.stamp(pdf, new StampRequest(seal, null, null, 0, 1.0f));

        assertStampedPdf(stamped, 1);
    }

    @Test
    void 指定坐标盖章() throws IOException {
        byte[] pdf = createPdf(1);
        byte[] seal = createSealPng();

        byte[] stamped = service.stamp(pdf, new StampRequest(seal, 10, 10, 0, 1.5f));

        assertStampedPdf(stamped, 1);
    }

    @Test
    void 多页叠加盖章() throws IOException {
        byte[] pdf = createPdf(2);
        byte[] seal = createSealPng();

        // 对第二页（索引 1）盖章
        byte[] stamped = service.stamp(pdf, new StampRequest(seal, null, null, 1, 0.8f));

        assertStampedPdf(stamped, 2);
    }

    private void assertStampedPdf(byte[] stamped, int expectedPages) throws IOException {
        assertNotNull(stamped);
        assertTrue(stamped.length > 0, "产出 PDF 字节不应为空");
        try (PDDocument doc = Loader.loadPDF(stamped)) {
            assertTrue(doc.getNumberOfPages() == expectedPages,
                    "重新加载后页数应为 " + expectedPages);
        }
    }

    /** 生成指定页数的空白 PDF。 */
    private byte[] createPdf(int pages) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < pages; i++) {
                doc.addPage(new PDPage());
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    /** 生成一张红色圆形 PNG 作为印章图像。 */
    private byte[] createSealPng() throws IOException {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.RED);
        graphics.fillOval(0, 0, 100, 100);
        graphics.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
