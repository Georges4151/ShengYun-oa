package com.ceams.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class BatchStampService {

    public static class StampPosition {
        private float xPercent;
        private float yPercent;
        private float scale;

        public StampPosition(float xPercent, float yPercent, float scale) {
            this.xPercent = xPercent;
            this.yPercent = yPercent;
            this.scale = scale;
        }

        public float getXPercent() {
            return xPercent;
        }

        public void setXPercent(float xPercent) {
            this.xPercent = xPercent;
        }

        public float getYPercent() {
            return yPercent;
        }

        public void setYPercent(float yPercent) {
            this.yPercent = yPercent;
        }

        public float getScale() {
            return scale;
        }

        public void setScale(float scale) {
            this.scale = scale;
        }
    }

    public static class BatchStampResult {
        private boolean success;
        private String outputPath;
        private int stampedPages;
        private String errorMessage;

        public BatchStampResult(boolean success, String outputPath, int stampedPages, String errorMessage) {
            this.success = success;
            this.outputPath = outputPath;
            this.stampedPages = stampedPages;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getOutputPath() {
            return outputPath;
        }

        public int getStampedPages() {
            return stampedPages;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public BatchStampResult batchStampPdf(String inputPdfPath, String stampImagePath, StampPosition position,
            String pageRange, String outputDir) {
        try {
            File inputFile = new File(inputPdfPath);
            if (!inputFile.exists()) {
                return new BatchStampResult(false, null, 0, "输入PDF文件不存在: " + inputPdfPath);
            }

            File stampFile = new File(stampImagePath);
            if (!stampFile.exists()) {
                return new BatchStampResult(false, null, 0, "印章图片文件不存在: " + stampImagePath);
            }

            String outputFileName = "stamped_" + System.currentTimeMillis() + "_" + inputFile.getName();
            String outputPath = outputDir + File.separator + outputFileName;

            File outputDirFile = new File(outputDir);
            if (!outputDirFile.exists()) {
                outputDirFile.mkdirs();
            }

            PDDocument document = PDDocument.load(inputFile);
            int totalPages = document.getNumberOfPages();

            List<Integer> pagesToStamp = parsePageRange(pageRange, totalPages);
            if (pagesToStamp.isEmpty()) {
                document.close();
                return new BatchStampResult(false, null, 0, "无效的页码范围: " + pageRange);
            }

            BufferedImage stampImage = ImageIO.read(stampFile);
            PDImageXObject pdStampImage = PDImageXObject.createFromByteArray(document,
                    imageToByteArray(stampImage), "stamp");

            int stampedCount = 0;
            for (int pageIndex : pagesToStamp) {
                PDPage page = document.getPage(pageIndex);
                PDRectangle pageSize = page.getMediaBox();
                float pageWidth = pageSize.getWidth();
                float pageHeight = pageSize.getHeight();

                float stampWidth = stampImage.getWidth() * position.getScale() / 100f;
                float stampHeight = stampImage.getHeight() * position.getScale() / 100f;

                float x = pageWidth * position.getXPercent() / 100f - stampWidth / 2;
                float y = pageHeight * (1 - position.getYPercent() / 100f) - stampHeight / 2;

                PDPageContentStream contentStream = new PDPageContentStream(document, page,
                        PDPageContentStream.AppendMode.APPEND, true, true);
                contentStream.drawImage(pdStampImage, x, y, stampWidth, stampHeight);
                contentStream.close();

                stampedCount++;
            }

            document.save(outputPath);
            document.close();

            return new BatchStampResult(true, outputPath, stampedCount, null);

        } catch (IOException e) {
            e.printStackTrace();
            return new BatchStampResult(false, null, 0, "批量盖章失败: " + e.getMessage());
        }
    }

    private List<Integer> parsePageRange(String pageRange, int totalPages) {
        List<Integer> pages = new ArrayList<>();

        if (pageRange == null || pageRange.trim().isEmpty() || pageRange.equalsIgnoreCase("all")) {
            for (int i = 0; i < totalPages; i++) {
                pages.add(i);
            }
            return pages;
        }

        String[] parts = pageRange.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.contains("-")) {
                String[] range = part.split("-");
                if (range.length == 2) {
                    try {
                        int start = Integer.parseInt(range[0].trim());
                        int end = Integer.parseInt(range[1].trim());
                        for (int i = Math.max(1, start); i <= Math.min(totalPages, end); i++) {
                            pages.add(i - 1);
                        }
                    } catch (NumberFormatException e) {
                        // 忽略无效的范围
                    }
                }
            } else {
                try {
                    int pageNum = Integer.parseInt(part);
                    if (pageNum >= 1 && pageNum <= totalPages) {
                        pages.add(pageNum - 1);
                    }
                } catch (NumberFormatException e) {
                    // 忽略无效的页码
                }
            }
        }

        return pages;
    }

    private byte[] imageToByteArray(BufferedImage image) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    public int getPdfPageCount(String pdfPath) throws IOException {
        File file = new File(pdfPath);
        if (!file.exists()) {
            return 0;
        }
        try (PDDocument document = PDDocument.load(file)) {
            return document.getNumberOfPages();
        }
    }
}
