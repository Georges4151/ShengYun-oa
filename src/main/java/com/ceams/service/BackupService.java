package com.ceams.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
public class BackupService {

    @Autowired
    private ConfigService configService;

    @Autowired
    private LogService logService;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    // 从配置服务获取是否启用备份压缩
    private boolean isBackupCompressionEnabled() {
        return configService.getBooleanConfig("backup.compression.enabled", true);
    }

    // 从配置服务获取备份路径
    private String getBackupPath() {
        return configService.getConfig("backup.path", "backup/");
    }

    // 从配置服务获取备份保留天数
    private int getBackupKeepDays() {
        return configService.getIntConfig("backup.keep.days", 30);
    }

    // 从配置服务获取是否启用自动备份
    private boolean isAutoBackupEnabled() {
        return configService.getBooleanConfig("backup.auto.enabled", false);
    }

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    public String createBackup(boolean isFullBackup) throws IOException, InterruptedException {
        return createBackup(isFullBackup, null);
    }

    /**
     * 创建备份
     * 
     * @param isFullBackup 是否为全量备份
     * @param userId       操作用户ID，null表示系统自动备份
     * @return 备份文件路径
     * @throws IOException          IO异常
     * @throws InterruptedException 中断异常
     */
    public String createBackup(boolean isFullBackup, Integer userId) throws IOException, InterruptedException {
        // 创建备份目录
        String backupPath = getBackupPath();
        Path backupDir = Paths.get(backupPath);
        Files.createDirectories(backupDir);

        // 生成备份文件名
        String backupType = isFullBackup ? "full" : "incremental";
        String backupFileName = String.format("backup_%s_%s.sql", backupType, sdf.format(new Date()));
        Path backupFile = backupDir.resolve(backupFileName);

        // 构建mysqldump命令
        String[] command = {
                "cmd", "/c",
                "mysqldump",
                "--user=" + dbUsername,
                "--password=" + dbPassword,
                "--databases", "ceams",
                "--result-file=" + backupFile.toString()
        };

        // 执行备份命令
        Process process = Runtime.getRuntime().exec(command);
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            String errorMessage = "数据库备份失败，退出码: " + exitCode;
            logService.saveBackupLog(userId, backupFileName, false, errorMessage);
            throw new IOException(errorMessage);
        }

        String finalBackupPath = backupFile.toString();

        // 如果启用了压缩，进行压缩
        if (isBackupCompressionEnabled()) {
            finalBackupPath = compressBackup(backupFile);
            // 删除原始SQL文件
            Files.deleteIfExists(backupFile);
        }

        // 增强的备份验证
        if (!enhancedVerifyBackup(finalBackupPath)) {
            String errorMessage = "备份文件验证失败: " + finalBackupPath;
            logService.saveBackupLog(userId, backupFileName, false, errorMessage);
            // 删除无效的备份文件
            Files.deleteIfExists(Paths.get(finalBackupPath));
            throw new IOException(errorMessage);
        }

        // 清理旧备份
        cleanupOldBackups();

        // 记录备份日志
        logService.saveBackupLog(userId, backupFileName, true, "备份成功");

        return finalBackupPath;
    }

    /**
     * 压缩备份文件
     * 
     * @param backupFile 原始备份文件路径
     * @return 压缩后的备份文件路径
     * @throws IOException IO异常
     */
    private String compressBackup(Path backupFile) throws IOException {
        String zipFilePath = backupFile.toString() + ".zip";
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath))) {
            // 添加SQL文件到压缩包
            ZipEntry zipEntry = new ZipEntry(backupFile.getFileName().toString());
            zos.putNextEntry(zipEntry);

            // 读取并写入文件内容
            Files.copy(backupFile, zos);
            zos.closeEntry();

            // 添加备份元数据
            ZipEntry metaEntry = new ZipEntry("backup_meta.txt");
            zos.putNextEntry(metaEntry);

            // 写入元数据
            String metaData = String.format("Backup Date: %s\n" +
                    "Database: ceams\n" +
                    "Backup Type: %s\n" +
                    "Size: %d bytes",
                    sdf.format(new Date()),
                    backupFile.getFileName().toString().contains("full") ? "Full" : "Incremental",
                    Files.size(backupFile));
            zos.write(metaData.getBytes());
            zos.closeEntry();
        }
        return zipFilePath;
    }

    /**
     * 增强的备份验证
     * 
     * @param backupFilePath 备份文件路径
     * @return 是否验证通过
     * @throws IOException IO异常
     */
    private boolean enhancedVerifyBackup(String backupFilePath) throws IOException {
        File backupFile = new File(backupFilePath);
        if (!backupFile.exists() || backupFile.length() == 0) {
            return false;
        }

        // 如果是压缩文件，检查压缩文件结构
        if (backupFilePath.endsWith(".zip")) {
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(backupFile))) {
                // 检查是否包含SQL文件
                ZipEntry entry;
                boolean hasSqlFile = false;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().endsWith(".sql")) {
                        hasSqlFile = true;
                        break;
                    }
                }
                return hasSqlFile;
            }
        }

        // 对于SQL文件，检查是否包含基本的SQL结构
        try (BufferedReader reader = new BufferedReader(new FileReader(backupFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // 检查是否包含CREATE TABLE或INSERT INTO等SQL关键字
                if (line.startsWith("CREATE TABLE") || line.startsWith("INSERT INTO")) {
                    return true;
                }
            }
        }

        return false;
    }

    public void restoreBackup(String backupFilePath) throws IOException, InterruptedException {
        restoreBackup(backupFilePath, null);
    }

    /**
     * 恢复备份
     * 
     * @param backupFilePath 备份文件路径
     * @param userId         操作用户ID，null表示系统操作
     * @throws IOException          IO异常
     * @throws InterruptedException 中断异常
     */
    public void restoreBackup(String backupFilePath, Integer userId) throws IOException, InterruptedException {
        String finalBackupFilePath = backupFilePath;
        boolean isTempFile = false;

        // 如果是压缩文件，先解压
        if (backupFilePath.endsWith(".zip")) {
            finalBackupFilePath = extractBackup(backupFilePath);
            isTempFile = true;
        }

        // 使用 ProcessBuilder 正确处理输入重定向
        ProcessBuilder pb = new ProcessBuilder(
                "cmd", "/c",
                "mysql",
                "--user=" + dbUsername,
                "--password=" + dbPassword,
                "ceams");

        // 设置输入源为备份文件
        pb.redirectInput(new File(finalBackupFilePath));
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // 执行恢复命令
        int exitCode = process.waitFor();

        // 清理临时文件
        if (isTempFile) {
            Files.deleteIfExists(Paths.get(finalBackupFilePath));
        }

        if (exitCode != 0) {
            String errorMessage = "数据库恢复失败，退出码: " + exitCode;
            logService.saveBackupLog(userId, new File(backupFilePath).getName(), false, errorMessage);
            throw new IOException(errorMessage);
        }

        // 记录恢复日志
        logService.saveBackupLog(userId, new File(backupFilePath).getName(), true, "恢复成功");
    }

    /**
     * 解压备份文件
     * 
     * @param zipFilePath 压缩文件路径
     * @return 解压后的SQL文件路径
     * @throws IOException IO异常
     */
    private String extractBackup(String zipFilePath) throws IOException {
        Path tempDir = Files.createTempDirectory("backup_extract");
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".sql")) {
                    Path sqlFile = tempDir.resolve(entry.getName());
                    Files.createDirectories(sqlFile.getParent());
                    try (OutputStream os = Files.newOutputStream(sqlFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            os.write(buffer, 0, len);
                        }
                    }
                    return sqlFile.toString();
                }
            }
        }
        throw new IOException("压缩文件中未找到SQL文件");
    }

    public List<String> listBackups() throws IOException {
        String backupPath = getBackupPath();
        Path backupDir = Paths.get(backupPath);
        if (!Files.exists(backupDir)) {
            return List.of();
        }

        // 列出所有备份文件
        return Files.list(backupDir)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".sql"))
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList());
    }

    public void deleteBackup(String backupFileName) throws IOException {
        String backupPath = getBackupPath();
        Path backupFile = Paths.get(backupPath, backupFileName);
        Files.deleteIfExists(backupFile);
    }

    private void cleanupOldBackups() throws IOException {
        String backupPath = getBackupPath();
        int keepDays = getBackupKeepDays();

        Path backupDir = Paths.get(backupPath);
        if (!Files.exists(backupDir)) {
            return;
        }

        // 计算保留期限
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -keepDays);
        Date cutoffDate = calendar.getTime();

        // 删除过期备份
        Files.list(backupDir)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis() < cutoffDate.getTime();
                    } catch (IOException e) {
                        return false;
                    }
                })
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    /**
     * 自动备份任务，每天凌晨2点执行一次，根据配置的天数间隔决定是否执行备份
     * 使用@Scheduled注解实现定时任务
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void autoBackup() {
        // 检查是否启用自动备份
        if (!isAutoBackupEnabled()) {
            return;
        }

        try {
            // 获取备份间隔天数
            int backupIntervalDays = configService.getIntConfig("backup.interval.days", 7);

            // 检查是否需要执行备份（根据上次备份时间）
            String backupPath = getBackupPath();
            Path backupDir = Paths.get(backupPath);

            // 计算上次备份时间
            Date lastBackupDate = null;
            if (Files.exists(backupDir)) {
                lastBackupDate = Files.list(backupDir)
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".sql"))
                        .map(path -> {
                            try {
                                return new Date(Files.getLastModifiedTime(path).toMillis());
                            } catch (IOException e) {
                                return null;
                            }
                        })
                        .filter(date -> date != null)
                        .max(Date::compareTo)
                        .orElse(null);
            }

            // 判断是否需要执行备份
            boolean needBackup = true;
            if (lastBackupDate != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(lastBackupDate);
                calendar.add(Calendar.DAY_OF_MONTH, backupIntervalDays);
                Date nextBackupDate = calendar.getTime();

                // 如果当前时间还没到下次备份时间，则不需要备份
                if (new Date().before(nextBackupDate)) {
                    needBackup = false;
                }
            }

            if (needBackup) {
                // 执行自动备份
                createBackup(true);
                System.out.println("自动备份完成！");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.out.println("自动备份失败：" + e.getMessage());
        }
    }

    /**
     * 立即执行备份
     * 
     * @return 备份文件路径
     * @throws IOException          IO异常
     * @throws InterruptedException 中断异常
     */
    public String backupNow() throws IOException, InterruptedException {
        return backupNow(null);
    }

    /**
     * 立即执行备份
     * 
     * @param userId 操作用户ID，null表示系统操作
     * @return 备份文件路径
     * @throws IOException          IO异常
     * @throws InterruptedException 中断异常
     */
    public String backupNow(Integer userId) throws IOException, InterruptedException {
        return createBackup(true, userId);
    }

    public boolean verifyBackup(String backupFilePath) throws IOException {
        // 简单验证：检查文件是否存在且大小大于0
        File backupFile = new File(backupFilePath);
        return backupFile.exists() && backupFile.length() > 0;
    }
}