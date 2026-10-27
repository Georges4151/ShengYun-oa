package com.ceams.service;

import com.ceams.model.entity.ElectronicSealStamp;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public interface PdfService {
    /**
     * 导出电子章盖章记录为PDF
     */
    String exportStampsToPdf(List<ElectronicSealStamp> stamps, String outputPath) throws IOException, DocumentException;

    /**
     * 导出单个电子章盖章记录为PDF
     */
    String exportStampToPdf(ElectronicSealStamp stamp, String outputPath) throws IOException, DocumentException;
}
