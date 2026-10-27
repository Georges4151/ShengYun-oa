package com.ceams.controller;

import com.ceams.model.entity.Receipt;
import com.ceams.security.CustomUserDetails;
import com.ceams.service.ReceiptService;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Controller
@RequestMapping("/receipt")
public class ReceiptController {

    @Autowired
    private ReceiptService receiptService;

    private Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            return userDetails.getUserId();
        }
        return null;
    }

    @GetMapping("/list")
    public String list(Model model, @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        List<Receipt> receipts;
        if (status != null) {
            receipts = receiptService.getByVerificationStatus(status);
        } else if (type != null) {
            receipts = receiptService.getByType(type);
        } else {
            receipts = receiptService.getAllReceipts();
        }
        model.addAttribute("receipts", receipts);
        return "receipt/list";
    }

    @GetMapping("/upload")
    public String uploadForm() {
        return "receipt/upload";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file, @RequestParam("type") String type,
            @RequestParam(value = "reporterId", required = false) Integer reporterId,
            @RequestParam(value = "projectId", required = false) Integer projectId,
            @RequestParam(value = "description", required = false) String description,
            Model model) {
        try {
            Integer userId = getCurrentUserId();
            if (userId == null) {
                model.addAttribute("error", "用户未登录");
                return "redirect:/login";
            }
            Receipt receipt = receiptService.uploadReceipt(file, userId, reporterId, projectId, type, description);
            model.addAttribute("message", "票据上传成功！");
            model.addAttribute("receipt", receipt);
        } catch (IOException | TesseractException | NoSuchAlgorithmException e) {
            model.addAttribute("error", "票据上传失败：" + e.getMessage());
            return "receipt/upload";
        }
        return "receipt/upload_success";
    }

    @GetMapping("/verify/{id}")
    public String verifyForm(@PathVariable Integer id, Model model) {
        Receipt receipt = receiptService.getReceiptDetail(id);
        model.addAttribute("receipt", receipt);
        return "receipt/verify";
    }

    @PostMapping("/verify")
    public String verify(@RequestParam Integer receiptId, @RequestParam Boolean isVerified) {
        Integer verifiedBy = getCurrentUserId();
        if (verifiedBy == null) {
            return "redirect:/login";
        }
        String status = isVerified ? "verified" : "rejected";
        receiptService.verifyReceipt(receiptId, verifiedBy, status);
        return "redirect:/receipt/list";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable Integer id, Model model) {
        Receipt receipt = receiptService.getReceiptDetail(id);
        model.addAttribute("receipt", receipt);
        return "receipt/view";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        receiptService.deleteReceipt(id);
        return "redirect:/receipt/list";
    }

    @GetMapping("/pending")
    public String pendingList(Model model) {
        List<Receipt> receipts = receiptService.getByVerificationStatus("pending");
        model.addAttribute("receipts", receipts);
        return "receipt/pending";
    }
}