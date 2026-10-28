package com.ceams.module.seal.service.impl;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.pdf.PdfStampService;
import com.ceams.common.pdf.StampRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 基于 Apache PDFBox 的 PDF 盖章实现。
 *
 * <p>将印章图像按指定缩放比例叠加到源 PDF 的指定页；坐标为空时居中放置。
 * 返回新生成的 PDF 字节，不修改入参。</p>
 */
@Slf4j
@Service
public class PdfBoxStampServiceImpl implements PdfStampService {

    @Override
    public byte[] stamp(byte[] sourcePdf, StampRequest request) {
        if (sourcePdf == null || sourcePdf.length == 0) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "源 PDF 内容为空");
        }
        if (request == null || request.getSealImage() == null || request.getSealImage().length == 0) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "印章图像为空");
        }

        float scale = request.getScale() > 0 ? request.getScale() : 1.0f;

        try (PDDocument document = Loader.loadPDF(sourcePdf)) {
            int pageCount = document.getNumberOfPages();
            int pageIndex = request.getPageNumber();
            if (pageIndex < 0 || pageIndex >= pageCount) {
                throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT,
                        "页码越界：pageNumber=" + pageIndex + ", 总页数=" + pageCount);
            }

            PDPage page = document.getPage(pageIndex);
            PDImageXObject sealImage = PDImageXObject.createFromByteArray(document, request.getSealImage(), "seal");

            float stampedWidth = sealImage.getWidth() * scale;
            float stampedHeight = sealImage.getHeight() * scale;
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();

            float x = request.getX() != null ? request.getX() : (pageWidth - stampedWidth) / 2f;
            float y = request.getY() != null ? request.getY() : (pageHeight - stampedHeight) / 2f;

            try (PDPageContentStream contentStream =
                         new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                contentStream.drawImage(sealImage, x, y, stampedWidth, stampedHeight);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("PDF 盖章失败", e);
            throw new BusinessException(ErrorCode.BIZ_UNPROCESSABLE, "PDF 盖章失败", e);
        }
    }
}
