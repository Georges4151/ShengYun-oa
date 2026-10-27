package com.ceams.service;

import com.ceams.dao.ReceiptMapper;
import com.ceams.model.Pagination;
import com.ceams.model.entity.Receipt;
import com.ceams.model.entity.PaymentTransaction;
import com.ceams.model.event.AccountDataChangedEvent;
import com.ceams.service.PaymentTransactionService;
import com.ceams.util.FileValidator;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

@Service
public class ReceiptService {

    @Autowired
    private ReceiptMapper receiptMapper;

    @Autowired
    private PaymentTransactionService paymentTransactionService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Value("${ceams.receipt.path:}")
    private String configuredReceiptPath;

    @Value("${ceams.ocr.data-path}")
    private String ocrDataPath;

    @Value("${ceams.ocr.language}")
    private String ocrLanguage;

    private String getReceiptStoragePath() {
        if (configuredReceiptPath != null && !configuredReceiptPath.isEmpty()
                && !configuredReceiptPath.equals("./rephoto")) {
            return configuredReceiptPath;
        }
        String appPath = System.getProperty("user.dir");
        return appPath + File.separator + "rephoto";
    }

    private String receiptPath;

    private String getReceiptPath() {
        if (receiptPath == null) {
            receiptPath = getReceiptStoragePath();
        }
        return receiptPath;
    }

    public List<Receipt> getByUserId(Integer userId) {
        return receiptMapper.selectByUserId(userId);
    }

    public List<Receipt> getByVerificationStatus(String status) {
        return receiptMapper.selectByVerificationStatus(status);
    }

    public List<Receipt> getByDateRange(Date startDate, Date endDate) {
        return receiptMapper.selectByDateRange(startDate, endDate);
    }

    public List<Receipt> getByType(String type) {
        return receiptMapper.selectByType(type);
    }

    public List<Receipt> getByReporterId(Integer reporterId) {
        return receiptMapper.selectByReporterId(reporterId);
    }

    public List<Receipt> getByProjectId(Integer projectId) {
        return receiptMapper.selectByProjectId(projectId);
    }

    public List<Receipt> getByReporterName(String reporterName) {
        return receiptMapper.selectByReporterName(reporterName);
    }

    public List<Receipt> getByProjectName(String projectName) {
        return receiptMapper.selectByProjectName(projectName);
    }

    /**
     * 检查票据编号是否重复
     * 
     * @param receiptNumber 票据编号
     * @return 如果重复返回true，否则返回false
     */
    public boolean isReceiptNumberDuplicate(String receiptNumber) {
        if (receiptNumber == null || receiptNumber.trim().isEmpty()) {
            return false;
        }
        int count = receiptMapper.checkDuplicateByReceiptNumber(receiptNumber.trim());
        return count > 0;
    }

    /**
     * 验证票据编号唯一性并抛出异常
     * 
     * @param receiptNumber 票据编号
     * @throws IOException 如果票据编号重复
     */
    private void validateReceiptNumberUnique(String receiptNumber) throws IOException {
        if (receiptNumber == null || receiptNumber.trim().isEmpty()) {
            return;
        }
        if (isReceiptNumberDuplicate(receiptNumber.trim())) {
            throw new IOException("票据编号重复：系统中已存在编号为 [" + receiptNumber + "] 的票据，请使用不同的编号");
        }
    }

    public Receipt uploadReceipt(MultipartFile file, Integer userId, Integer reporterId, Integer projectId, String type,
            String description)
            throws IOException, TesseractException, NoSuchAlgorithmException {
        return uploadReceipt(file, userId, reporterId, projectId, type, description, null);
    }

    public Receipt uploadReceipt(MultipartFile file, Integer userId, Integer reporterId, Integer projectId, String type,
            String description, String receiptNumber)
            throws IOException, TesseractException, NoSuchAlgorithmException {
        FileValidator.validateImageFile(file);

        validateReceiptNumberUnique(receiptNumber);

        String path = getReceiptPath();
        File uploadDir = new File(path);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + extension;
        String filePath = path + File.separator + fileName;

        File dest = new File(filePath);
        file.transferTo(dest);

        String fileHash = calculateFileHash(dest);

        String ocrData = performOCR(dest);

        Receipt receipt = new Receipt();
        receipt.setReceiptNumber(receiptNumber != null ? receiptNumber.trim() : null);
        receipt.setUserId(userId);
        receipt.setReporterId(reporterId);
        receipt.setProjectId(projectId);
        receipt.setType(type);
        receipt.setDescription(description);
        receipt.setFilePath(filePath);
        receipt.setFileHash(fileHash);
        receipt.setOcrData(ocrData);
        receipt.setVerificationStatus("pending");
        receipt.setPaymentStatus("unpaid");
        receipt.setFinanceStatus("pending");
        receipt.setProjectManagerStatus("pending");
        receipt.setManagerStatus("pending");
        receipt.setCurrentApprovalLevel(0);

        receiptMapper.insert(receipt);

        return receipt;
    }

    /**
     * 适用于JavaFX桌面应用的票据上传方法（带日期和金额）
     */
    public Receipt uploadReceipt(File file, Integer userId, Integer reporterId, Integer projectId, String type,
            java.util.Date receiptDate,
            java.math.BigDecimal amount, String reporterName, String description)
            throws IOException, NoSuchAlgorithmException {
        return uploadReceipt(file, userId, reporterId, projectId, type, receiptDate, amount, reporterName, description,
                null);
    }

    public Receipt uploadReceipt(File file, Integer userId, Integer reporterId, Integer projectId, String type,
            java.util.Date receiptDate,
            java.math.BigDecimal amount, String reporterName, String description, String receiptNumber)
            throws IOException, NoSuchAlgorithmException {
        FileValidator.validateImageFile(file);

        validateReceiptNumberUnique(receiptNumber);

        String path = getReceiptPath();
        File uploadDir = new File(path);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        String originalFilename = file.getName();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + extension;
        String filePath = path + File.separator + fileName;

        File dest = new File(filePath);
        FileUtils.copyFile(file, dest);

        String fileHash = calculateFileHash(dest);

        String ocrData = performOCR(dest);
        System.out.println("OCR识别结果: " + ocrData);

        Receipt receipt = new Receipt();
        receipt.setReceiptNumber(receiptNumber != null ? receiptNumber.trim() : null);
        receipt.setUserId(userId);
        receipt.setReporterId(reporterId);
        receipt.setProjectId(projectId);
        receipt.setReporterName(reporterName);
        receipt.setReceiptDate(receiptDate);
        receipt.setAmount(amount);
        receipt.setType(type);
        receipt.setDescription(description);
        receipt.setFilePath(filePath);
        receipt.setFileHash(fileHash);
        receipt.setOcrData(ocrData);
        System.out.println("设置OCR结果到票据对象: " + receipt.getOcrData());
        receipt.setVerificationStatus("pending");
        receipt.setPaymentStatus("unpaid");
        receipt.setFinanceStatus("pending");
        receipt.setProjectManagerStatus("pending");
        receipt.setManagerStatus("pending");
        receipt.setCurrentApprovalLevel(0);

        receiptMapper.insert(receipt);
        System.out.println("保存票据到数据库，ID: " + receipt.getId());

        return receipt;
    }

    /**
     * 适用于JavaFX桌面应用的票据上传方法（默认）
     */
    public Receipt uploadReceipt(File file, Integer userId, String type)
            throws IOException, NoSuchAlgorithmException {
        return uploadReceipt(file, userId, null, null, type, null, null, null, null);
    }

    /**
     * 适用于移动端和REST API的票据上传方法（接收byte数组）
     */
    public Receipt uploadReceipt(byte[] fileBytes, Integer userId, Integer reporterId, Integer projectId, String type,
            java.util.Date receiptDate,
            java.math.BigDecimal amount, String reporterName, String description)
            throws IOException, NoSuchAlgorithmException {
        return uploadReceipt(fileBytes, userId, reporterId, projectId, type, receiptDate, amount, reporterName,
                description, null);
    }

    public Receipt uploadReceipt(byte[] fileBytes, Integer userId, Integer reporterId, Integer projectId, String type,
            java.util.Date receiptDate,
            java.math.BigDecimal amount, String reporterName, String description, String receiptNumber)
            throws IOException, NoSuchAlgorithmException {
        FileValidator.validateImageBytes(fileBytes, "upload.jpg");

        validateReceiptNumberUnique(receiptNumber);

        String path = getReceiptPath();
        File uploadDir = new File(path);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        String extension = ".jpg";
        String fileName = UUID.randomUUID().toString() + extension;
        String filePath = path + File.separator + fileName;

        File dest = new File(filePath);
        FileUtils.writeByteArrayToFile(dest, fileBytes);

        String fileHash = calculateFileHash(dest);

        String ocrData = performOCR(dest);

        Receipt receipt = new Receipt();
        receipt.setReceiptNumber(receiptNumber != null ? receiptNumber.trim() : null);
        receipt.setUserId(userId);
        receipt.setReporterId(reporterId);
        receipt.setProjectId(projectId);
        receipt.setReporterName(reporterName);
        receipt.setReceiptDate(receiptDate);
        receipt.setAmount(amount);
        receipt.setType(type);
        receipt.setDescription(description);
        receipt.setFilePath(filePath);
        receipt.setFileHash(fileHash);
        receipt.setOcrData(ocrData);
        receipt.setVerificationStatus("pending");
        receipt.setPaymentStatus("unpaid");
        receipt.setFinanceStatus("pending");
        receipt.setProjectManagerStatus("pending");
        receipt.setManagerStatus("pending");
        receipt.setCurrentApprovalLevel(0);

        receiptMapper.insert(receipt);

        return receipt;
    }

    private String calculateFileHash(File file) throws IOException, NoSuchAlgorithmException {
        byte[] fileBytes = FileUtils.readFileToByteArray(file);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(fileBytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String performOCR(File file) {
        try {
            // 1. 图片预处理：提高OCR识别精度
            File processedFile = preprocessImage(file);

            // 2. OCR识别
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(ocrDataPath);
            tesseract.setLanguage(ocrLanguage);

            // 设置OCR识别参数，提高中文票据识别精度
            // 页面分割模式：11 = 假设一张票据包含多个文本块
            tesseract.setPageSegMode(11);
            // OCR引擎模式：1 = 基于LSTM的OCR引擎
            tesseract.setOcrEngineMode(1);

            String rawResult = tesseract.doOCR(processedFile);
            System.out.println("OCR原始识别结果: " + rawResult.substring(0, Math.min(100, rawResult.length())) + "...");

            // 3. 清理临时文件
            if (processedFile != file) {
                processedFile.delete();
            }

            // 4. 后处理：清理识别结果
            String cleanedResult = cleanOcrResult(rawResult);

            return cleanedResult;
        } catch (Throwable e) {
            System.out.println("OCR识别失败: " + e.getMessage());
            // 检查是否是语言包缺失的错误
            if (e.getMessage() != null && (e.getMessage().contains("Error opening data file")
                    || e.getMessage().contains("Failed loading language"))) {
                return "OCR识别失败: 缺少语言包，请安装tessdata语言包";
            }
            return "OCR识别失败: " + e.getMessage();
        }
    }

    /**
     * 图片预处理：提高OCR识别精度
     */
    private File preprocessImage(File inputFile) throws IOException {
        // 使用Java ImageIO进行图片预处理
        java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(inputFile);

        // 创建图片处理后的临时文件
        File outputFile = File.createTempFile("ocr_", ".png");

        // 1. 灰度化
        java.awt.image.BufferedImage grayImage = new java.awt.image.BufferedImage(image.getWidth(), image.getHeight(),
                java.awt.image.BufferedImage.TYPE_BYTE_GRAY);
        java.awt.Graphics g = grayImage.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        // 2. 二值化处理（阈值调整）
        java.awt.image.BufferedImage binaryImage = new java.awt.image.BufferedImage(grayImage.getWidth(),
                grayImage.getHeight(), java.awt.image.BufferedImage.TYPE_BYTE_BINARY);
        for (int x = 0; x < grayImage.getWidth(); x++) {
            for (int y = 0; y < grayImage.getHeight(); y++) {
                int pixel = grayImage.getRGB(x, y);
                int gray = (pixel >> 16) & 0xFF;
                // 调整阈值，提高文字与背景的对比度
                int threshold = 150;
                int newPixel = gray < threshold ? 0 : 0xFFFFFF;
                binaryImage.setRGB(x, y, newPixel);
            }
        }

        // 3. 保存处理后的图片
        javax.imageio.ImageIO.write(binaryImage, "png", outputFile);
        System.out.println("图片预处理完成，保存到: " + outputFile.getAbsolutePath());

        return outputFile;
    }

    /**
     * 后处理：清理OCR识别结果
     */
    private String cleanOcrResult(String rawResult) {
        // 移除多余的空格和空行
        String cleaned = rawResult
                .replaceAll("\\s+\\n", "\\n") // 移除行首空格
                .replaceAll("\\n\\s+", "\\n") // 移除行尾空格
                .replaceAll("\\n{3,}", "\\n\\n") // 最多保留两个连续换行
                .trim();

        return cleaned;
    }

    public boolean verifyReceipt(Integer receiptId, Integer verifiedBy, String status) {
        Receipt receipt = receiptMapper.selectById(receiptId);
        if (receipt != null) {
            receipt.setVerifiedBy(verifiedBy);
            receipt.setVerifiedAt(new Date());
            receipt.setVerificationStatus(status);
            boolean result = receiptMapper.update(receipt) > 0;
            if (result) {
                // 发布账目数据变化事件
                eventPublisher.publishEvent(new AccountDataChangedEvent(this, "验证了票据"));
            }
            return result;
        }
        return false;
    }

    public boolean deleteReceipt(Integer receiptId) {
        Receipt receipt = receiptMapper.selectById(receiptId);
        if (receipt != null) {
            // 删除文件
            File file = new File(receipt.getFilePath());
            if (file.exists()) {
                file.delete();
            }
            // 删除数据库记录
            boolean result = receiptMapper.deleteById(receiptId) > 0;
            if (result) {
                // 发布账目数据变化事件
                eventPublisher.publishEvent(new AccountDataChangedEvent(this, "删除了票据"));
            }
            return result;
        }
        return false;
    }

    public Receipt getReceiptDetail(Integer receiptId) {
        return receiptMapper.selectById(receiptId);
    }

    public List<Receipt> getAllReceipts() {
        return receiptMapper.selectAll();
    }

    /**
     * 分页获取所有票据记录
     * 
     * @param pagination 分页参数
     * @return 票据记录列表
     */
    public List<Receipt> getReceiptsWithPagination(Pagination pagination) {
        return receiptMapper.selectAllWithPagination(pagination.getPageSize(), pagination.getOffset());
    }

    /**
     * 分页获取票据记录（支持条件）
     * 
     * @param userId             用户ID
     * @param reporterId         报票人ID
     * @param projectId          项目ID
     * @param verificationStatus 验证状态
     * @param paymentStatus      支付状态
     * @param type               票据类型
     * @param startDate          开始日期
     * @param endDate            结束日期
     * @param pagination         分页参数
     * @return 票据记录列表
     */
    /**
     * 获取所有票据记录总数
     * 
     * @return 票据记录总数
     */
    public long count() {
        return receiptMapper.count();
    }

    public List<Receipt> getReceiptsByConditionsWithPagination(
            Integer userId, Integer reporterId, Integer projectId, String verificationStatus,
            String paymentStatus, String type, Date startDate, Date endDate, Pagination pagination) {
        return receiptMapper.selectByConditionsWithPagination(
                userId, reporterId, projectId, verificationStatus, paymentStatus, type,
                startDate, endDate, pagination.getPageSize(), pagination.getOffset());
    }

    /**
     * 获取票据记录总数（支持条件）
     * 
     * @param userId             用户ID
     * @param reporterId         报票人ID
     * @param projectId          项目ID
     * @param verificationStatus 验证状态
     * @param paymentStatus      支付状态
     * @param type               票据类型
     * @param startDate          开始日期
     * @param endDate            结束日期
     * @return 票据记录总数
     */
    public int getReceiptCountByConditions(
            Integer userId, Integer reporterId, Integer projectId, String verificationStatus,
            String paymentStatus, String type, Date startDate, Date endDate) {
        return receiptMapper.countByConditions(
                userId, reporterId, projectId, verificationStatus, paymentStatus, type, startDate, endDate);
    }

    /**
     * 更新票据支付状态
     * 
     * @param receiptId     票据ID
     * @param paymentStatus 支付状态：paid(已支付), unpaid(未支付), partially_paid(部分支付)
     * @param paidAmount    已支付金额
     * @param paidBy        支付人ID
     * @param paidAt        支付时间
     * @param paymentMethod 支付方式：cash(现金), bank_transfer(银行转账), wechat(微信),
     *                      alipay(支付宝), other(其他)
     * @param accountId     支付账户ID
     * @return 更新是否成功
     */
    public boolean updatePaymentStatus(Integer receiptId, String paymentStatus, java.math.BigDecimal paidAmount,
            Integer paidBy, Date paidAt, String paymentMethod, Integer accountId) {
        Receipt receipt = receiptMapper.selectById(receiptId);
        if (receipt != null) {
            receipt.setPaymentStatus(paymentStatus);
            receipt.setPaidAmount(paidAmount);
            receipt.setPaidBy(paidBy);
            receipt.setPaidAt(paidAt);
            receipt.setPaymentMethod(paymentMethod);
            boolean updated = receiptMapper.updatePaymentStatus(receipt) > 0;

            // 如果支付成功且账户ID不为空，创建交易记录
            if (updated && accountId != null && paidAmount != null
                    && paidAmount.compareTo(java.math.BigDecimal.ZERO) > 0) {
                PaymentTransaction transaction = new PaymentTransaction();
                transaction.setAccountId(accountId);
                transaction.setReceiptId(receiptId);
                transaction.setAmount(paidAmount);
                transaction.setType("expense"); // 票据支付为支出
                transaction.setTransactionDate(paidAt != null ? paidAt : new Date());
                transaction.setDescription("支付票据 #" + receiptId + " - " + receipt.getType());
                paymentTransactionService.addTransaction(transaction);
            }

            if (updated) {
                // 发布账目数据变化事件
                eventPublisher.publishEvent(new AccountDataChangedEvent(this, "更新了票据支付状态"));
            }

            return updated;
        }
        return false;
    }

    public List<Receipt> searchReceipts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllReceipts();
        }
        return receiptMapper.searchByKeyword(keyword.trim());
    }
}