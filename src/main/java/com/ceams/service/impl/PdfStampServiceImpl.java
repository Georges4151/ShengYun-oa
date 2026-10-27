package com.ceams.service.impl;

import com.ceams.service.PdfStampService;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;

@Service
public class PdfStampServiceImpl implements PdfStampService {

    @Override
    public void stampPdf(String inputPath, String outputPath, String sealImageBase64, float x, float y, float width,
            float height)
            throws IOException, DocumentException {
        PdfReader reader = new PdfReader(inputPath);
        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(outputPath));

        PdfContentByte content = stamper.getOverContent(1);

        Image image = Image.getInstance(Base64.getDecoder().decode(sealImageBase64));
        image.setAbsolutePosition(x, y);
        image.scaleToFit(width, height);

        content.addImage(image);

        stamper.close();
        reader.close();
    }

    @Override
    public void stampPdfWithPosition(String inputPath, String outputPath, String sealImageBase64, int page,
            String position)
            throws IOException, DocumentException {
        PdfReader reader = new PdfReader(inputPath);
        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(outputPath));

        PdfContentByte content = stamper.getOverContent(page);

        Image image = Image.getInstance(Base64.getDecoder().decode(sealImageBase64));

        float pageWidth = reader.getPageSizeWithRotation(page).getWidth();
        float pageHeight = reader.getPageSizeWithRotation(page).getHeight();

        float x = 0;
        float y = 0;
        float width = 100;
        float height = 100;

        switch (position) {
            case "top_left":
                x = 50;
                y = pageHeight - 150;
                break;
            case "top_right":
                x = pageWidth - 150;
                y = pageHeight - 150;
                break;
            case "bottom_left":
                x = 50;
                y = 50;
                break;
            case "bottom_right":
                x = pageWidth - 150;
                y = 50;
                break;
            case "center":
                x = (pageWidth - width) / 2;
                y = (pageHeight - height) / 2;
                break;
            default:
                x = pageWidth - 150;
                y = 50;
                break;
        }

        image.setAbsolutePosition(x, y);
        image.scaleToFit(width, height);

        content.addImage(image);

        stamper.close();
        reader.close();
    }

    @Override
    public void stampPdfAtPosition(String inputPath, String outputPath, String sealImageBase64, int page,
            float x, float y, float size)
            throws IOException, DocumentException {
        PdfReader reader = new PdfReader(inputPath);
        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(outputPath));

        PdfContentByte content = stamper.getOverContent(page);

        Image image = Image.getInstance(Base64.getDecoder().decode(sealImageBase64));

        // 设置印章位置和大小
        image.setAbsolutePosition(x, y);
        image.scaleToFit(size, size);

        content.addImage(image);

        stamper.close();
        reader.close();
    }
}
