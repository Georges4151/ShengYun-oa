package com.ceams.service.impl;

import com.ceams.model.entity.ElectronicSealStamp;
import com.ceams.service.PdfService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

@Service
public class PdfServiceImpl implements PdfService {

    private static final String FONT_PATH = "C:\\Windows\\Fonts\\simsun.ttc,1";
    private BaseFont baseFont;

    public PdfServiceImpl() {
        try {
            baseFont = BaseFont.createFont(FONT_PATH, BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
        } catch (DocumentException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String exportStampsToPdf(List<ElectronicSealStamp> stamps, String outputPath)
            throws IOException, DocumentException {
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, new FileOutputStream(outputPath));
        document.open();

        // 添加标题
        Font titleFont = new Font(baseFont, 18, Font.BOLD);
        Paragraph title = new Paragraph("电子章盖章记录", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // 添加表格
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);

        // 表头
        Font headerFont = new Font(baseFont, 12, Font.BOLD);
        String[] headers = { "电子章名称", "项目名称", "合同名称", "位置", "盖章时间" };
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }

        // 表格内容
        Font contentFont = new Font(baseFont, 10);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (ElectronicSealStamp stamp : stamps) {
            table.addCell(new Phrase(stamp.getSealName() != null ? stamp.getSealName() : "", contentFont));
            table.addCell(new Phrase(stamp.getProjectName() != null ? stamp.getProjectName() : "", contentFont));
            table.addCell(new Phrase(stamp.getContractName() != null ? stamp.getContractName() : "", contentFont));
            table.addCell(new Phrase(stamp.getStampPosition() != null ? stamp.getStampPosition() : "", contentFont));
            table.addCell(new Phrase(
                    stamp.getStampTime() != null ? dateFormat.format(stamp.getStampTime()) : "",
                    contentFont));
        }

        document.add(table);

        // 添加页脚
        Font footerFont = new Font(baseFont, 9);
        Paragraph footer = new Paragraph("生成时间: " + dateFormat.format(new java.util.Date()), footerFont);
        footer.setAlignment(Element.ALIGN_RIGHT);
        footer.setSpacingBefore(20);
        document.add(footer);

        document.close();

        return outputPath;
    }

    @Override
    public String exportStampToPdf(ElectronicSealStamp stamp, String outputPath)
            throws IOException, DocumentException {
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, new FileOutputStream(outputPath));
        document.open();

        // 添加标题
        Font titleFont = new Font(baseFont, 18, Font.BOLD);
        Paragraph title = new Paragraph("电子章盖章记录", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // 添加详细信息
        Font contentFont = new Font(baseFont, 11);
        Paragraph paragraph;

        paragraph = new Paragraph("电子章名称: " + (stamp.getSealName() != null ? stamp.getSealName() : ""), contentFont);
        paragraph.setSpacingAfter(5);
        document.add(paragraph);

        paragraph = new Paragraph("项目名称: " + (stamp.getProjectName() != null ? stamp.getProjectName() : ""),
                contentFont);
        paragraph.setSpacingAfter(5);
        document.add(paragraph);

        paragraph = new Paragraph("合同名称: " + (stamp.getContractName() != null ? stamp.getContractName() : ""),
                contentFont);
        paragraph.setSpacingAfter(5);
        document.add(paragraph);

        paragraph = new Paragraph("盖章位置: " + (stamp.getStampPosition() != null ? stamp.getStampPosition() : ""),
                contentFont);
        paragraph.setSpacingAfter(5);
        document.add(paragraph);

        paragraph = new Paragraph("操作用户: " + (stamp.getUserName() != null ? stamp.getUserName() : ""), contentFont);
        paragraph.setSpacingAfter(5);
        document.add(paragraph);

        paragraph = new Paragraph("盖章时间: " +
                (stamp.getStampTime() != null ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(stamp.getStampTime())
                        : ""),
                contentFont);
        paragraph.setSpacingAfter(20);
        document.add(paragraph);

        // 添加页脚
        Font footerFont = new Font(baseFont, 9);
        Paragraph footer = new Paragraph("生成时间: " +
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()),
                footerFont);
        footer.setAlignment(Element.ALIGN_RIGHT);
        document.add(footer);

        document.close();

        return outputPath;
    }
}
