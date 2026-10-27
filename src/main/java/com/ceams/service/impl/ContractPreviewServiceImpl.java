package com.ceams.service.impl;

import com.ceams.model.entity.ElectronicSeal;
import com.ceams.model.entity.StampApproval;
import com.ceams.service.ContractPreviewService;
import com.ceams.service.ElectronicSealService;
import com.ceams.service.SealEncryptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Base64;

@Service
public class ContractPreviewServiceImpl implements ContractPreviewService {

    @Autowired
    private ElectronicSealService electronicSealService;

    @Autowired
    private SealEncryptionService sealEncryptionService;

    private static final String PREVIEW_DIR = "./temp_previews";
    private static final String STAMPED_CONTRACTS_DIR = "./stamped_contracts";
    private static final long PREVIEW_EXPIRY_MINUTES = 30;

    @Override
    public File generatePreviewWithStamps(StampApproval approval) {
        try {
            String originalPath = approval.getOriginalContractPath();
            String positionsJson = approval.getStampPositions();

            if (originalPath == null || originalPath.isEmpty()) {
                originalPath = approval.getContractPath();
            }

            if (originalPath == null || originalPath.isEmpty()) {
                return null;
            }

            File originalFile = new File(originalPath);
            if (!originalFile.exists()) {
                return null;
            }

            File previewDir = new File(PREVIEW_DIR);
            if (!previewDir.exists()) {
                previewDir.mkdirs();
            }

            String previewFileName = "preview_" + approval.getId() + "_" + System.currentTimeMillis() + ".pdf";
            File previewFile = new File(PREVIEW_DIR, previewFileName);

            PdfReader reader = new PdfReader(originalPath);
            FileOutputStream outputStream = new FileOutputStream(previewFile);
            PdfStamper stamper = new PdfStamper(reader, outputStream);

            // 获取PDF总页数，用于边界检查
            int totalPages = reader.getNumberOfPages();
            System.out.println("PDF总页数: " + totalPages);

            if (positionsJson != null && !positionsJson.isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, Object>> positions = mapper.readValue(positionsJson,
                        new TypeReference<List<Map<String, Object>>>() {
                        });

                for (Map<String, Object> pos : positions) {
                    Integer sealId = (Integer) pos.get("sealId");
                    Integer pageNum = (Integer) pos.get("pageNum");
                    Double x = ((Number) pos.get("x")).doubleValue();
                    Double y = ((Number) pos.get("y")).doubleValue();
                    Double size = ((Number) pos.get("size")).doubleValue();

                    // 边界检查：确保页码在有效范围内
                    if (pageNum == null || pageNum < 1 || pageNum > totalPages) {
                        System.err.println("警告: 印章位置页码无效: pageNum=" + pageNum + ", 总页数=" + totalPages + ", 跳过此印章");
                        continue;
                    }

                    ElectronicSeal seal = electronicSealService.getSealById(sealId);
                    if (seal != null) {
                        byte[] imageBytes;
                        if (seal.getSealImage() != null && !seal.getSealImage().isEmpty()) {
                            imageBytes = Base64.getDecoder().decode(seal.getSealImage());
                        } else {
                            continue;
                        }

                        Image stampImage = Image.getInstance(imageBytes);
                        stampImage.setAbsolutePosition(x.floatValue(), y.floatValue());
                        stampImage.scaleToFit(size.floatValue(), size.floatValue());

                        // 添加空指针检查
                        com.itextpdf.text.pdf.PdfContentByte overContent = stamper.getOverContent(pageNum);
                        if (overContent != null) {
                            overContent.addImage(stampImage);
                        } else {
                            System.err.println("警告: 无法获取第 " + pageNum + " 页的PdfContentByte，跳过此印章");
                        }
                    }
                }
            }

            stamper.close();
            reader.close();
            outputStream.close();

            return previewFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void cleanupOldPreviews() {
        try {
            File previewDir = new File(PREVIEW_DIR);
            if (!previewDir.exists()) {
                return;
            }

            File[] files = previewDir.listFiles();
            if (files == null) {
                return;
            }

            long currentTime = System.currentTimeMillis();
            long expiryTime = PREVIEW_EXPIRY_MINUTES * 60 * 1000;

            for (File file : files) {
                if (currentTime - file.lastModified() > expiryTime) {
                    file.delete();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void scheduledCleanup() {
        cleanupOldPreviews();
    }

    @Override
    public File generateStampedContract(StampApproval approval) {
        System.out.println("=== generateStampedContract 开始 ===");
        try {
            String originalPath = approval.getOriginalContractPath();
            String positionsJson = approval.getStampPositions();

            System.out.println("原始合同路径(从originalContractPath): " + originalPath);
            System.out.println("印章位置JSON长度: " + (positionsJson != null ? positionsJson.length() : "null"));

            if (originalPath == null || originalPath.isEmpty()) {
                originalPath = approval.getContractPath();
                System.out.println("从contractPath获取路径: " + originalPath);
            }

            if (originalPath == null || originalPath.isEmpty()) {
                System.err.println("原始合同路径为空，无法生成盖章文件");
                return null;
            }

            File originalFile = new File(originalPath);
            System.out.println("原始文件绝对路径: " + originalFile.getAbsolutePath());
            System.out.println("原始文件是否存在: " + originalFile.exists());

            if (!originalFile.exists()) {
                System.err.println("原始合同文件不存在: " + originalPath);
                return null;
            }

            File stampedDir = new File(STAMPED_CONTRACTS_DIR);
            if (!stampedDir.exists()) {
                boolean created = stampedDir.mkdirs();
                System.out.println("创建盖章目录: " + STAMPED_CONTRACTS_DIR + ", 结果: " + created);
            }

            String originalFileName = originalFile.getName();
            String baseName = originalFileName;
            if (baseName.toLowerCase().endsWith(".pdf")) {
                baseName = baseName.substring(0, baseName.length() - 4);
            }

            String stampedFileName = baseName + "_盖章_" + System.currentTimeMillis() + ".pdf";
            File stampedFile = new File(STAMPED_CONTRACTS_DIR, stampedFileName);
            System.out.println("目标盖章文件: " + stampedFile.getAbsolutePath());

            PdfReader reader = new PdfReader(originalPath);
            FileOutputStream outputStream = new FileOutputStream(stampedFile);
            PdfStamper stamper = new PdfStamper(reader, outputStream);

            int stampCount = 0;
            if (positionsJson != null && !positionsJson.isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, Object>> positions = mapper.readValue(positionsJson,
                        new TypeReference<List<Map<String, Object>>>() {
                        });

                int totalPages = reader.getNumberOfPages();
                System.out.println("解析到 " + positions.size() + " 个印章位置");

                for (Map<String, Object> pos : positions) {
                    Integer sealId = (Integer) pos.get("sealId");
                    Integer pageNum = (Integer) pos.get("pageNum");
                    Double x = ((Number) pos.get("x")).doubleValue();
                    Double y = ((Number) pos.get("y")).doubleValue();
                    Double size = ((Number) pos.get("size")).doubleValue();

                    if (pageNum == null || pageNum < 1 || pageNum > totalPages) {
                        System.err.println("警告: 页码无效 pageNum=" + pageNum + ", 总页数=" + totalPages + ", 跳过");
                        continue;
                    }

                    System.out.println("处理印章: sealId=" + sealId + ", pageNum=" + pageNum + ", x=" + x + ", y=" + y);

                    ElectronicSeal seal = electronicSealService.getSealById(sealId);
                    if (seal != null) {
                        byte[] imageBytes = null;

                        String sealImageFromPos = (String) pos.get("sealImage");
                        if (sealImageFromPos != null && !sealImageFromPos.isEmpty()) {
                            try {
                                imageBytes = Base64.getDecoder().decode(sealImageFromPos);
                                System.out.println("从位置数据解码印章图片成功, 大小: " + imageBytes.length);
                            } catch (Exception e) {
                                System.err.println("从位置数据解码印章图片失败: " + e.getMessage());
                            }
                        }

                        if (imageBytes == null && seal.getSealImage() != null && !seal.getSealImage().isEmpty()) {
                            imageBytes = Base64.getDecoder().decode(seal.getSealImage());
                            System.out.println("从数据库解码印章图片成功");
                        }

                        if (imageBytes == null) {
                            System.err.println("印章图片数据为空，跳过: " + seal.getSealName());
                            continue;
                        }

                        Image stampImage = Image.getInstance(imageBytes);
                        stampImage.setAbsolutePosition(x.floatValue(), y.floatValue());
                        stampImage.scaleToFit(size.floatValue(), size.floatValue());

                        stamper.getOverContent(pageNum).addImage(stampImage);
                        stampCount++;
                        System.out.println("印章添加成功: " + seal.getSealName() + " 到第" + pageNum + "页");
                    } else {
                        System.err.println("未找到印章: " + sealId);
                    }
                }
            }

            stamper.close();
            reader.close();
            outputStream.close();

            System.out.println("盖章文件生成成功: " + stampedFile.getAbsolutePath());
            System.out.println("共添加 " + stampCount + " 个印章");
            return stampedFile;
        } catch (Exception e) {
            System.err.println("生成盖章文件失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
