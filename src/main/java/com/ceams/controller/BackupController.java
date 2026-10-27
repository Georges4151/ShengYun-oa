package com.ceams.controller;

import com.ceams.service.BackupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.List;

/**
 * 备份管理控制器
 * 仅允许 MANAGER 和 ADMIN 角色访问
 */
@Controller
@RequestMapping("/backup")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class BackupController {

    @Autowired
    private BackupService backupService;

    @GetMapping("/list")
    public String list(Model model) {
        try {
            List<String> backups = backupService.listBackups();
            model.addAttribute("backups", backups);
        } catch (IOException e) {
            model.addAttribute("error", "获取备份列表失败：" + e.getMessage());
        }
        return "backup/list";
    }

    @GetMapping("/create")
    public String createForm() {
        return "backup/create";
    }

    @PostMapping("/create")
    public String create(@RequestParam("type") String type, Model model) {
        try {
            boolean isFullBackup = "full".equals(type);
            String backupFile = backupService.createBackup(isFullBackup);
            model.addAttribute("message", "备份成功！备份文件：" + backupFile);
        } catch (IOException | InterruptedException e) {
            model.addAttribute("error", "备份失败：" + e.getMessage());
        }
        return "backup/create";
    }

    @GetMapping("/restore")
    public String restoreForm(Model model) {
        try {
            List<String> backups = backupService.listBackups();
            model.addAttribute("backups", backups);
        } catch (IOException e) {
            model.addAttribute("error", "获取备份列表失败：" + e.getMessage());
        }
        return "backup/restore";
    }

    @PostMapping("/restore")
    public String restore(@RequestParam("backupFile") String backupFile, Model model) {
        try {
            String backupFilePath = "backup/" + backupFile;
            backupService.restoreBackup(backupFilePath);
            model.addAttribute("message", "恢复成功！");
        } catch (IOException | InterruptedException e) {
            model.addAttribute("error", "恢复失败：" + e.getMessage());
        }
        return "backup/restore";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam("backupFile") String backupFile) {
        try {
            backupService.deleteBackup(backupFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "redirect:/backup/list";
    }

    private boolean isValidBackupFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) return false;
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return false;
        }
        return fileName.matches("^[a-zA-Z0-9_\\-\\.]+$");
    }
}