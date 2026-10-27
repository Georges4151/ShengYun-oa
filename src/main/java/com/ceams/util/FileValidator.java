package com.ceams.util;

import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class FileValidator {

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "webp");

    private static final List<String> ALLOWED_DOCUMENT_EXTENSIONS = Arrays.asList(
            "pdf", "doc", "docx", "xls", "xlsx");

    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/bmp",
            "image/webp",
            "application/pdf");

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    public static void validateImageFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IOException("文件名不能为空");
        }

        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IOException(
                    "不支持的文件类型: " + extension + "。仅支持图片格式: " + String.join(", ", ALLOWED_IMAGE_EXTENSIONS));
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IOException("文件大小超过限制。最大允许: " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
        }

        String mimeType = file.getContentType();
        if (mimeType != null && !ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new IOException("不支持的文件MIME类型: " + mimeType);
        }
    }

    public static void validateImageFile(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new IOException("文件不存在");
        }

        String fileName = file.getName();
        if (fileName == null || fileName.isEmpty()) {
            throw new IOException("文件名不能为空");
        }

        String extension = getFileExtension(fileName);
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IOException(
                    "不支持的文件类型: " + extension + "。仅支持图片格式: " + String.join(", ", ALLOWED_IMAGE_EXTENSIONS));
        }

        if (file.length() > MAX_FILE_SIZE) {
            throw new IOException("文件大小超过限制。最大允许: " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
        }
    }

    public static void validateImageBytes(byte[] fileBytes, String fileName) throws IOException {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IOException("文件内容不能为空");
        }

        if (fileBytes.length > MAX_FILE_SIZE) {
            throw new IOException("文件大小超过限制。最大允许: " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
        }

        if (fileName != null && !fileName.isEmpty()) {
            String extension = getFileExtension(fileName);
            if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase())) {
                throw new IOException(
                        "不支持的文件类型: " + extension + "。仅支持图片格式: " + String.join(", ", ALLOWED_IMAGE_EXTENSIONS));
            }
        }

        if (!isValidImageHeader(fileBytes)) {
            throw new IOException("文件内容不是有效的图片格式");
        }
    }

    private static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }

    private static boolean isValidImageHeader(byte[] bytes) {
        if (bytes == null || bytes.length < 8) {
            return false;
        }

        if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) {
            return true;
        }

        if (bytes[0] == (byte) 0x89 && bytes[1] == (byte) 0x50 &&
                bytes[2] == (byte) 0x4E && bytes[3] == (byte) 0x47) {
            return true;
        }

        if (bytes[0] == (byte) 0x47 && bytes[1] == (byte) 0x49 &&
                bytes[2] == (byte) 0x46 && bytes[3] == (byte) 0x38) {
            return true;
        }

        if (bytes[0] == (byte) 0x42 && bytes[1] == (byte) 0x4D) {
            return true;
        }

        if (bytes[0] == (byte) 0x52 && bytes[1] == (byte) 0x49 &&
                bytes[2] == (byte) 0x46 && bytes[3] == (byte) 0x46 &&
                bytes.length > 12 && bytes[8] == (byte) 0x57 && bytes[9] == (byte) 0x45 &&
                bytes[10] == (byte) 0x42 && bytes[11] == (byte) 0x50) {
            return true;
        }

        return false;
    }

    public static String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "upload";
        }

        String sanitized = fileName.replaceAll("[^a-zA-Z0-9.\\-\\u4e00-\\u9fa5]", "_");

        if (sanitized.length() > 200) {
            String extension = getFileExtension(sanitized);
            String baseName = sanitized.substring(0, sanitized.lastIndexOf("."));
            sanitized = baseName.substring(0, 200 - extension.length() - 1) + "." + extension;
        }

        return sanitized;
    }
}
