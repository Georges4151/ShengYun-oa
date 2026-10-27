package com.ceams.controller;

import com.ceams.model.entity.SubcontractorContract;
import com.ceams.service.SubcontractorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private SubcontractorService subcontractorService;

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    @GetMapping("/contract/{contractId}")
    public ResponseEntity<?> downloadContract(
            @PathVariable Integer contractId,
            @RequestParam(required = false) String token) {
        try {
            SubcontractorContract contract = subcontractorService.getContractDetail(contractId);
            if (contract == null) {
                return ResponseEntity.notFound().build();
            }

            String filePath = contract.getFilePath();
            if (filePath == null || filePath.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            File file = resolveFile(filePath);
            if (file == null || !file.exists()) {
                return ResponseEntity.notFound().build();
            }

            String fileName = contract.getFileName();
            if (fileName == null || fileName.isEmpty()) {
                fileName = file.getName();
            }

            Resource resource = new FileSystemResource(file);
            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                contentType = getContentTypeByExtension(fileName);
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .header("X-Frame-Options", "ALLOWALL")
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("下载失败: " + e.getMessage());
        }
    }

    @GetMapping("/contract/file")
    public ResponseEntity<?> getContractFile(@RequestParam String path) {
        try {
            File file = resolveFile(path);
            if (file == null || !file.exists()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);
            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                contentType = getContentTypeByExtension(file.getName());
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                    .header("X-Frame-Options", "SAMEORIGIN")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("获取文件失败: " + e.getMessage());
        }
    }

    private File resolveFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            return file;
        }
        file = new File("./" + filePath);
        if (file.exists()) {
            return file;
        }
        file = new File(uploadPath + "/" + filePath);
        if (file.exists()) {
            return file;
        }
        return null;
    }

    private String getContentTypeByExtension(String fileName) {
        if (fileName == null) return "application/octet-stream";
        
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerName.endsWith(".png")) {
            return "image/png";
        } else if (lowerName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerName.endsWith(".doc")) {
            return "application/msword";
        } else if (lowerName.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (lowerName.endsWith(".xls")) {
            return "application/vnd.ms-excel";
        } else if (lowerName.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }
        return "application/octet-stream";
    }
}
