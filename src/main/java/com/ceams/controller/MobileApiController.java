package com.ceams.controller;

import com.ceams.dto.ApiResponse;
import com.ceams.dto.MobileLoginRequest;
import com.ceams.dto.MobileLoginResponse;
import com.ceams.dto.StampApprovalDTO;
import com.ceams.model.entity.ApprovalRecord;
import com.ceams.model.entity.Notification;
import com.ceams.model.entity.Receipt;
import com.ceams.model.entity.StampApproval;
import com.ceams.model.entity.User;
import com.ceams.model.entity.UserNotification;
import com.ceams.security.JwtUtil;
import com.ceams.service.ApprovalFlowService;
import com.ceams.service.ElectronicSealService;
import com.ceams.service.NotificationService;
import com.ceams.service.PdfWatermarkService;
import com.ceams.service.ProjectService;
import com.ceams.service.UserService;
import com.ceams.model.entity.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mobile")
public class MobileApiController {

    @Autowired
    private UserService userService;

    @Autowired
    private ElectronicSealService electronicSealService;

    @Autowired
    private ApprovalFlowService approvalFlowService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PdfWatermarkService pdfWatermarkService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody MobileLoginRequest request) {
        try {
            User user = userService.authenticate(request.getUsername(), request.getPassword());
            if (user == null) {
                return ResponseEntity.ok(ApiResponse.error("用户名或密码错误"));
            }

            String token = jwtUtil.generateToken(user.getUserId(), user.getUsername(), user.getRole());

            MobileLoginResponse response = new MobileLoginResponse();
            response.setUserId(user.getUserId());
            response.setUsername(user.getUsername());
            response.setName(user.getName());
            response.setRole(user.getRole());
            response.setToken(token);

            return ResponseEntity.ok(ApiResponse.success("登录成功", response));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("登录失败: " + e.getMessage()));
        }
    }

    @GetMapping("/stamp-approvals/pending")
    public ResponseEntity<?> getPendingApprovals(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            List<StampApproval> approvals = electronicSealService.getPendingStampApprovals();
            List<StampApprovalDTO> dtoList = approvals.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("list", dtoList);
            data.put("total", dtoList.size());

            return ResponseEntity.ok(ApiResponse.success("获取成功", data));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取待审批列表失败: " + e.getMessage()));
        }
    }

    @GetMapping("/stamp-approvals/{id}")
    public ResponseEntity<?> getApprovalDetail(@PathVariable Integer id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            StampApproval approval = electronicSealService.getStampApprovalById(id);
            if (approval == null) {
                return ResponseEntity.ok(ApiResponse.error("审批记录不存在"));
            }
            // 直接返回实体，不转换为 DTO
            return ResponseEntity.ok(ApiResponse.success("获取成功", approval));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取详情失败：" + e.getMessage()));
        }
    }

    @PostMapping("/stamp-approvals/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Integer id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            Integer userId = extractUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录或Token无效"));
            }
            User user = userService.getUserById(userId);
            if (user == null) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户不存在"));
            }

            StampApproval approval = electronicSealService.getStampApprovalById(id);
            if (approval == null) {
                return ResponseEntity.ok(ApiResponse.error("审批记录不存在"));
            }

            if (!"pending".equals(approval.getStatus()) && approval.getStatus() != null) {
                return ResponseEntity.ok(ApiResponse.error("该申请已处理"));
            }

            electronicSealService.approveStamp(id, user.getUserId(), user.getName());

            return ResponseEntity.ok(ApiResponse.success("审批通过"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("审批失败: " + e.getMessage()));
        }
    }

    @PostMapping("/stamp-approvals/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Integer id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> body) {
        try {
            Integer userId = extractUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录或Token无效"));
            }
            User user = userService.getUserById(userId);
            if (user == null) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户不存在"));
            }

            StampApproval approval = electronicSealService.getStampApprovalById(id);
            if (approval == null) {
                return ResponseEntity.ok(ApiResponse.error("审批记录不存在"));
            }

            if (!"pending".equals(approval.getStatus()) && approval.getStatus() != null) {
                return ResponseEntity.ok(ApiResponse.error("该申请已处理"));
            }

            String rejectReason = body != null ? (String) body.get("reason") : "";
            if (rejectReason == null || rejectReason.trim().isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("请填写拒绝理由"));
            }

            electronicSealService.rejectStamp(id, user.getUserId(), user.getName(), rejectReason);

            return ResponseEntity.ok(ApiResponse.success("已拒绝申请"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("操作失败: " + e.getMessage()));
        }
    }

    @GetMapping("/stamp-approvals/all")
    public ResponseEntity<?> getAllApprovals(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            List<StampApproval> approvals = electronicSealService.getAllStampApprovals();
            List<StampApprovalDTO> dtoList = approvals.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("list", dtoList);
            data.put("total", dtoList.size());

            return ResponseEntity.ok(ApiResponse.success("获取成功", data));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取列表失败: " + e.getMessage()));
        }
    }

    @GetMapping("/user/info")
    public ResponseEntity<?> getUserInfo(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Integer userId = extractUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录或Token无效"));
            }
            User user = userService.getUserById(userId);
            if (user == null) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户不存在"));
            }

            Map<String, Object> data = new HashMap<>();
            data.put("userId", user.getUserId());
            data.put("username", user.getUsername());
            data.put("name", user.getName());
            data.put("role", user.getRole());
            data.put("email", user.getEmail());

            return ResponseEntity.ok(ApiResponse.success("获取成功", data));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取用户信息失败: " + e.getMessage()));
        }
    }

    private Integer extractUserIdFromToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtil.getUserIdFromToken(token);
        }
        return null;
    }

    /**
     * 从Authorization头或URL参数中提取用户ID
     */
    private Integer extractUserIdFromTokenWithParam(String authHeader, String tokenParam) {
        // 先尝试从Authorization头获取
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                return jwtUtil.getUserIdFromToken(token);
            }
        }
        // 再尝试从URL参数获取
        if (tokenParam != null && jwtUtil.validateToken(tokenParam)) {
            return jwtUtil.getUserIdFromToken(tokenParam);
        }
        return null;
    }

    /**
     * 检查用户是否有权限查看印章审批合同
     */
    private boolean hasViewPermission(StampApproval approval, User user) {
        if (user == null || approval == null) {
            return false;
        }
        // 申请人、审批人或管理员可以查看
        if (user.getUserId().equals(approval.getApplicantId()) ||
                user.getUserId().equals(approval.getApproverId()) ||
                "ADMIN".equals(user.getRole()) || "管理员".equals(user.getRole())) {
            return true;
        }
        return false;
    }

    /**
     * 检查用户是否有权限查看票据
     */
    private boolean hasReceiptViewPermission(Receipt receipt, User user) {
        if (user == null || receipt == null) {
            return false;
        }
        // 票据上传者、各审核人、管理员可以查看
        if (user.getUserId().equals(receipt.getUserId()) ||
                (receipt.getFinanceVerifiedBy() != null && user.getUserId().equals(receipt.getFinanceVerifiedBy())) ||
                (receipt.getProjectManagerVerifiedBy() != null && user.getUserId().equals(receipt.getProjectManagerVerifiedBy())) ||
                (receipt.getManagerVerifiedBy() != null && user.getUserId().equals(receipt.getManagerVerifiedBy())) ||
                "ADMIN".equals(user.getRole()) || "管理员".equals(user.getRole())) {
            return true;
        }
        // 检查是否有审批角色权限
        Integer approvalLevel = getApprovalLevelByRole(user.getRole());
        return approvalLevel != null;
    }

    private StampApprovalDTO convertToDTO(StampApproval approval) {
        StampApprovalDTO dto = new StampApprovalDTO();
        dto.setId(approval.getId());
        dto.setStampId(approval.getStampId());
        dto.setContractName(approval.getContractName());
        dto.setContractPath(approval.getContractPath());
        dto.setSealId(approval.getSealId());
        dto.setSealName(approval.getSealName());
        dto.setSealCode(approval.getSealCode());
        dto.setSealPermission(approval.getSealPermission());
        dto.setProjectId(approval.getProjectId());
        dto.setProjectName(approval.getProjectName());
        dto.setApplicantId(approval.getApplicantId());
        dto.setApplicantName(approval.getApplicantName());
        dto.setApproverId(approval.getApproverId());
        dto.setApproverName(approval.getApproverName());
        dto.setStatus(approval.getStatus());
        dto.setStatusText(approval.getStatusText());
        dto.setRejectReason(approval.getRejectReason());
        dto.setApplyTime(approval.getApplyTime());
        dto.setApprovalTime(approval.getApprovalTime());
        dto.setCreateTime(approval.getCreateTime());
        return dto;
    }

    @GetMapping("/files/{fileName}")
    public ResponseEntity<?> getFile(@PathVariable String fileName,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // 验证用户登录状态
            Integer userId = extractUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录或Token无效"));
            }

            // 从数据库中查找对应的审批记录
            List<StampApproval> approvals = electronicSealService.getAllStampApprovals();
            StampApproval targetApproval = null;

            for (StampApproval approval : approvals) {
                if (approval.getContractPath() != null &&
                        approval.getContractPath().endsWith(fileName)) {
                    targetApproval = approval;
                    break;
                }
            }

            if (targetApproval == null || targetApproval.getContractPath() == null) {
                return ResponseEntity.notFound().build();
            }

            // 验证用户是否有权查看该合同
            User user = userService.getUserById(userId);
            if (user == null) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户不存在"));
            }

            // 检查权限：申请人、审批人或管理员可以查看
            if (!userId.equals(targetApproval.getApplicantId()) &&
                    !userId.equals(targetApproval.getApproverId()) &&
                    !"ADMIN".equals(user.getRole()) &&
                    !"管理员".equals(user.getRole())) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("无权限查看此合同"));
            }

            java.io.File file = new java.io.File(targetApproval.getContractPath());
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            // 读取文件内容
            byte[] fileContent = java.nio.file.Files.readAllBytes(file.toPath());

            // 设置响应头 - 支持预览
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            // inline预览模式不设置文件名，避免HTTP头部编码问题
            headers.setContentDispositionFormData("inline", null);
            headers.setContentLength(fileContent.length);

            return new ResponseEntity<>(fileContent, headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取文件失败：" + e.getMessage()));
        }
    }

    @GetMapping("/stamp-approvals/{id}/file")
    public ResponseEntity<?> getStampApprovalFile(@PathVariable Integer id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String tokenParam) {
        try {
            // 验证用户登录状态
            Integer userId = null;
            String validToken = null;

            // 先尝试从Authorization头获取
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                validToken = authHeader.substring(7);
                if (jwtUtil.validateToken(validToken)) {
                    userId = jwtUtil.getUserIdFromToken(validToken);
                }
            }

            // 再尝试从URL参数获取
            if (userId == null && tokenParam != null) {
                if (jwtUtil.validateToken(tokenParam)) {
                    userId = jwtUtil.getUserIdFromToken(tokenParam);
                    validToken = tokenParam;
                } else {
                    System.err.println("Token参数无效");
                }
            }

            if (userId == null) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录或Token无效"));
            }

            StampApproval approval = electronicSealService.getStampApprovalById(id);
            if (approval == null) {
                return ResponseEntity.ok(ApiResponse.error("审批记录不存在"));
            }

            // 验证用户是否有权查看该合同
            User user = userService.getUserById(userId);
            if (user == null) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户不存在"));
            }

            // 检查权限：申请人、审批人或管理员可以查看
            if (!userId.equals(approval.getApplicantId()) &&
                    !userId.equals(approval.getApproverId()) &&
                    !"ADMIN".equals(user.getRole()) &&
                    !"管理员".equals(user.getRole())) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("无权限查看此合同"));
            }

            String contractPath = approval.getContractPath();
            if (contractPath == null || contractPath.isEmpty()) {
                // 如果盖章后的文件不存在，尝试使用原始文件
                contractPath = approval.getOriginalContractPath();
            }

            if (contractPath == null || contractPath.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("合同文件不存在"));
            }

            java.io.File file = new java.io.File(contractPath);
            if (!file.exists()) {
                return ResponseEntity.ok(ApiResponse.error("合同文件不存在"));
            }

            // 读取文件内容
            byte[] fileContent = java.nio.file.Files.readAllBytes(file.toPath());

            // 获取文件名
            String fileName = file.getName();

            // 设置响应头 - 支持预览
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            // inline预览模式不设置文件名，避免HTTP头部编码问题
            headers.setContentDispositionFormData("inline", null);
            headers.setContentLength(fileContent.length);

            return new ResponseEntity<>(fileContent, headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取文件失败：" + e.getMessage()));
        }
    }

    /**
     * 印章审批合同预览（带水印）- 用于审批过程中
     * 返回带有动态水印的PDF，水印包含用户名和时间戳
     */
    @GetMapping("/stamp-approvals/{id}/preview")
    public ResponseEntity<?> getStampApprovalPreview(@PathVariable Integer id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String tokenParam) {
        try {
            // 验证用户登录状态
            Integer userId = extractUserIdFromTokenWithParam(authHeader, tokenParam);
            if (userId == null) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录或Token无效"));
            }

            StampApproval approval = electronicSealService.getStampApprovalById(id);
            if (approval == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("审批记录不存在"));
            }

            // 验证权限
            User user = userService.getUserById(userId);
            if (!hasViewPermission(approval, user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("无权限查看此合同"));
            }

            // 优先使用已盖章的文件
            // 无论contractPath是否为空，都先尝试在stamped_contracts目录查找盖章文件
            String contractPath = null;
            String originalPath = approval.getOriginalContractPath();
            
            System.out.println("=== 预览合同调试信息 ===");
            System.out.println("审批ID: " + id);
            System.out.println("审批状态: " + approval.getStatus());
            System.out.println("originalContractPath: " + originalPath);
            System.out.println("contractPath: " + approval.getContractPath());
            
            // 尝试从originalContractPath或contractPath查找盖章文件
            String fileName = null;
            if (originalPath != null && !originalPath.isEmpty()) {
                java.io.File originalFile = new java.io.File(originalPath);
                fileName = originalFile.getName();
                System.out.println("从originalContractPath获取文件名: " + fileName);
            } else if (approval.getContractPath() != null && !approval.getContractPath().isEmpty()) {
                java.io.File contractFile = new java.io.File(approval.getContractPath());
                fileName = contractFile.getName();
                System.out.println("从contractPath获取文件名: " + fileName);
            }
            
            // 如果获取到了文件名，查找stamped_contracts目录中的盖章文件
            if (fileName != null) {
                String stampedPath = "E:\\sxassist\\stamped_contracts\\" + fileName;
                java.io.File stampedFile = new java.io.File(stampedPath);
                System.out.println("查找盖章文件: " + stampedPath);
                System.out.println("盖章文件是否存在: " + stampedFile.exists());
                
                if (stampedFile.exists()) {
                    contractPath = stampedPath;
                    System.out.println("✓ 使用已盖章文件: " + contractPath);
                }
            }
            
            // 如果没找到盖章文件，尝试使用contractPath
            if (contractPath == null || contractPath.isEmpty()) {
                contractPath = approval.getContractPath();
                System.out.println("未找到盖章文件，使用contractPath: " + contractPath);
            }
            
            // 如果还是没有，使用原始文件
            if (contractPath == null || contractPath.isEmpty()) {
                contractPath = originalPath;
                System.out.println("未找到contractPath，使用originalContractPath: " + contractPath);
            }
            
            if (contractPath == null || contractPath.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("合同文件不存在"));
            }

            java.io.File file = new java.io.File(contractPath);
            if (!file.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("合同文件不存在: " + contractPath));
            }

            // 获取用户名用于水印
            String watermarkUsername = (user != null && user.getName() != null)
                    ? user.getName() : (user != null ? user.getUsername() : "未知用户");

            // 添加水印并返回
            byte[] watermarkedContent = pdfWatermarkService.addWatermarkToPdf(contractPath, watermarkUsername);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            // inline预览模式不设置文件名，避免HTTP头部编码问题
            headers.setContentDispositionFormData("inline", null);
            headers.setContentLength(watermarkedContent.length);

            return new ResponseEntity<>(watermarkedContent, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("生成预览失败: " + e.getMessage()));
        }
    }

    @GetMapping("/receipts/{id}/file")
    public ResponseEntity<?> getReceiptFile(@PathVariable Integer id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String tokenParam) {
        try {
            // 验证用户登录状态
            Integer userId = null;
            String validToken = null;
            
            // 先尝试从Authorization头获取
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                validToken = authHeader.substring(7);
                if (jwtUtil.validateToken(validToken)) {
                    userId = jwtUtil.getUserIdFromToken(validToken);
                }
            }
            
            // 再尝试从URL参数获取
            if (userId == null && tokenParam != null) {
                if (jwtUtil.validateToken(tokenParam)) {
                    userId = jwtUtil.getUserIdFromToken(tokenParam);
                    validToken = tokenParam;
                } else {
                    System.err.println("Token参数无效");
                }
            }
            
            if (userId == null) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录或Token无效"));
            }

            Receipt receipt = approvalFlowService.getReceiptApprovalStatus(id);
            if (receipt == null) {
                return ResponseEntity.ok(ApiResponse.error("票据不存在"));
            }

            // 验证用户是否有权查看该票据
            User user = userService.getUserById(userId);
            if (user == null) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户不存在"));
            }

            // 检查权限：申请人、审批人或管理员可以查看
            if (!userId.equals(receipt.getUserId()) &&
                    !userId.equals(receipt.getFinanceVerifiedBy()) &&
                    !userId.equals(receipt.getProjectManagerVerifiedBy()) &&
                    !userId.equals(receipt.getManagerVerifiedBy()) &&
                    !"ADMIN".equals(user.getRole()) &&
                    !"管理员".equals(user.getRole())) {
                // 检查是否有审批权限
                String role = user.getRole();
                Integer approvalLevel = getApprovalLevelByRole(role);
                if (approvalLevel == null) {
                    return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                            .body(ApiResponse.error("无权限查看此票据"));
                }
            }

            String filePath = receipt.getFilePath();
            if (filePath == null || filePath.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("票据文件不存在"));
            }

            java.io.File file = new java.io.File(filePath);
            if (!file.exists()) {
                return ResponseEntity.ok(ApiResponse.error("票据文件不存在"));
            }

            // 读取文件内容
            byte[] fileContent = java.nio.file.Files.readAllBytes(file.toPath());

            // 获取文件名
            String fileName = file.getName();

            // 设置响应头 - 支持预览
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();

            // 根据文件扩展名设置正确的MIME类型
            if (fileName.toLowerCase().endsWith(".pdf")) {
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            } else if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
                headers.setContentType(org.springframework.http.MediaType.IMAGE_JPEG);
            } else if (fileName.toLowerCase().endsWith(".png")) {
                headers.setContentType(org.springframework.http.MediaType.IMAGE_PNG);
            } else if (fileName.toLowerCase().endsWith(".gif")) {
                headers.setContentType(org.springframework.http.MediaType.IMAGE_GIF);
            } else {
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
            }

            // 设置Content-Disposition为inline以支持浏览器预览
            // inline预览模式不设置文件名，避免HTTP头部编码问题
            headers.setContentDispositionFormData("inline", null);
            headers.setContentLength(fileContent.length);
            
            // 添加CORS头，支持跨域访问
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Authorization, Content-Type");
            headers.add("Access-Control-Expose-Headers", "Content-Disposition, Content-Length");
            
            // 添加缓存控制头
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");

            return new ResponseEntity<>(fileContent, headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取文件失败：" + e.getMessage()));
        }
    }

    /**
     * 票据预览（带水印）- 用于审批过程中
     * 返回带有动态水印的PDF/图片，水印包含用户名和时间戳
     */
    @GetMapping("/receipts/{id}/preview")
    public ResponseEntity<?> getReceiptPreview(@PathVariable Integer id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String tokenParam) {
        try {
            // 验证用户登录状态
            Integer userId = extractUserIdFromTokenWithParam(authHeader, tokenParam);
            if (userId == null) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录或Token无效"));
            }

            Receipt receipt = approvalFlowService.getReceiptApprovalStatus(id);
            if (receipt == null) {
                return ResponseEntity.ok(ApiResponse.error("票据不存在"));
            }

            // 验证权限
            User user = userService.getUserById(userId);
            if (!hasReceiptViewPermission(receipt, user)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("无权限查看此票据"));
            }

            String filePath = receipt.getFilePath();
            if (filePath == null || filePath.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("票据文件不存在"));
            }

            java.io.File file = new java.io.File(filePath);
            if (!file.exists()) {
                return ResponseEntity.ok(ApiResponse.error("票据文件不存在"));
            }

            // 获取用户名用于水印
            String watermarkUsername = (user != null && user.getName() != null)
                    ? user.getName() : (user != null ? user.getUsername() : "未知用户");

            byte[] watermarkedContent;
            
            // 如果是PDF文件，添加水印
            if (file.getName().toLowerCase().endsWith(".pdf")) {
                watermarkedContent = pdfWatermarkService.addWatermarkToPdf(filePath, watermarkUsername);
            } else {
                // 非PDF文件（图片等）直接返回原始内容
                watermarkedContent = java.nio.file.Files.readAllBytes(file.toPath());
            }

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();

            // 根据文件扩展名设置正确的MIME类型
            String fileName = file.getName();
            if (fileName.toLowerCase().endsWith(".pdf")) {
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            } else if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
                headers.setContentType(org.springframework.http.MediaType.IMAGE_JPEG);
            } else if (fileName.toLowerCase().endsWith(".png")) {
                headers.setContentType(org.springframework.http.MediaType.IMAGE_PNG);
            } else {
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
            }
            // inline预览模式不设置文件名，避免HTTP头部编码问题
            headers.setContentDispositionFormData("inline", null);
            headers.setContentLength(watermarkedContent.length);

            return new ResponseEntity<>(watermarkedContent, headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("生成预览失败: " + e.getMessage()));
        }
    }
    
    /**
     * 处理CORS预检请求
     */
    @RequestMapping(value = "/receipts/{id}/file", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleReceiptFileOptions() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Authorization, Content-Type");
        headers.add("Access-Control-Max-Age", "3600");
        return new ResponseEntity<>(headers, org.springframework.http.HttpStatus.OK);
    }

    @GetMapping("/receipts/pending")
    public ResponseEntity<?> getPendingReceipts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Integer userId = extractUserIdFromToken(authHeader);
            User user = null;
            if (userId != null) {
                user = userService.getUserById(userId);
            }
            if (user == null) {
                return ResponseEntity.ok(ApiResponse.error("用户未登录"));
            }

            String role = user.getRole();
            Integer approvalLevel = getApprovalLevelByRole(role);
            if (approvalLevel == null) {
                return ResponseEntity.ok(ApiResponse.error("无审批权限"));
            }

            Map<String, Object> result = approvalFlowService.getPendingApprovals(approvalLevel, page, pageSize);
            return ResponseEntity.ok(ApiResponse.success("获取成功", result));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取待审批列表失败: " + e.getMessage()));
        }
    }

    @GetMapping("/receipts/pending/all")
    public ResponseEntity<?> getAllPendingReceipts(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Integer userId = extractUserIdFromToken(authHeader);
            User user = null;
            if (userId != null) {
                user = userService.getUserById(userId);
            }
            if (user == null) {
                return ResponseEntity.ok(ApiResponse.error("用户未登录"));
            }

            String role = user.getRole();
            Integer approvalLevel = getApprovalLevelByRole(role);
            if (approvalLevel == null) {
                return ResponseEntity.ok(ApiResponse.error("无审批权限"));
            }

            List<Receipt> pendingReceipts = approvalFlowService.getPendingApprovals(approvalLevel);
            return ResponseEntity.ok(ApiResponse.success("获取成功", pendingReceipts));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取待审批列表失败: " + e.getMessage()));
        }
    }

    @GetMapping("/receipts/{id}")
    public ResponseEntity<?> getReceiptDetail(@PathVariable Integer id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Receipt receipt = approvalFlowService.getReceiptApprovalStatus(id);
            if (receipt == null) {
                return ResponseEntity.ok(ApiResponse.error("票据不存在"));
            }
            return ResponseEntity.ok(ApiResponse.success("获取成功", receipt));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取票据详情失败: " + e.getMessage()));
        }
    }

    @GetMapping("/receipts/{receiptId}/history")
    public ResponseEntity<?> getReceiptApprovalHistory(@PathVariable Integer receiptId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            List<ApprovalRecord> history = approvalFlowService.getApprovalHistory(receiptId);
            return ResponseEntity.ok(ApiResponse.success("获取成功", history));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取审批历史失败: " + e.getMessage()));
        }
    }

    @PostMapping("/receipts/approve")
    public ResponseEntity<?> approveReceipt(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody ApprovalRequest approvalRequest) {
        try {
            Integer userId = extractUserIdFromToken(authHeader);
            User user = null;
            if (userId != null) {
                user = userService.getUserById(userId);
            }
            if (user == null) {
                return ResponseEntity.ok(ApiResponse.error("用户未登录"));
            }

            String role = user.getRole();
            boolean canApprove = approvalFlowService.canApprove(approvalRequest.getReceiptId(), userId, role);
            if (!canApprove) {
                return ResponseEntity.ok(ApiResponse.error("无审批权限"));
            }

            boolean success = approvalFlowService.processApproval(
                    approvalRequest.getReceiptId(),
                    userId,
                    user.getName(),
                    approvalRequest.getStatus(),
                    approvalRequest.getComment());

            if (success) {
                return ResponseEntity.ok(ApiResponse.success("审批处理成功", null));
            } else {
                return ResponseEntity.ok(ApiResponse.error("审批处理失败"));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("审批处理失败: " + e.getMessage()));
        }
    }

    private Integer getApprovalLevelByRole(String role) {
        if (role == null) {
            return null;
        }
        switch (role) {
            case "财务":
            case "FINANCE":
                return 0;
            case "项目经理":
            case "PROJECT_MANAGER":
                return 1;
            case "经理":
            case "MANAGER":
                return 2;
            case "管理员":
            case "ADMIN":
                return 2;
            default:
                return null;
        }
    }

    public static class ApprovalRequest {
        private Integer receiptId;
        private String status;
        private String comment;

        public Integer getReceiptId() {
            return receiptId;
        }

        public void setReceiptId(Integer receiptId) {
            this.receiptId = receiptId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<?> getDashboardStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Integer userId = extractUserIdFromToken(authHeader);
            User user = null;
            if (userId != null) {
                user = userService.getUserById(userId);
            }
            if (user == null) {
                return ResponseEntity.ok(ApiResponse.error("用户未登录"));
            }

            String role = user.getRole();
            Map<String, Object> stats = new HashMap<>();

            int stampPendingCount = electronicSealService.getPendingStampApprovals().size();
            stats.put("stampPendingCount", stampPendingCount);

            Integer approvalLevel = getApprovalLevelByRole(role);
            if (approvalLevel != null) {
                List<Receipt> pendingReceipts = approvalFlowService.getPendingApprovals(approvalLevel);
                stats.put("receiptPendingCount", pendingReceipts.size());
            } else {
                stats.put("receiptPendingCount", 0);
            }

            stats.put("role", role);
            stats.put("userName", user.getName());

            return ResponseEntity.ok(ApiResponse.success("获取成功", stats));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取统计数据失败: " + e.getMessage()));
        }
    }

    @GetMapping("/notifications")
    public ResponseEntity<?> getUserNotifications(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Integer userId = extractUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.ok(ApiResponse.error("用户未登录"));
            }

            List<UserNotification> notifications = notificationService.getUserNotifications(userId);
            System.out.println("=== getUserNotifications: userId=" + userId + ", count=" + notifications.size());
            for (UserNotification un : notifications) {
                System.out.println("  - id=" + un.getId() + ", notificationId=" + un.getNotificationId() + ", isRead="
                        + un.getIsRead());
            }
            return ResponseEntity.ok(ApiResponse.success("获取成功", notifications));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取通知列表失败: " + e.getMessage()));
        }
    }

    @GetMapping("/notifications/unread-count")
    public ResponseEntity<?> getUnreadCount(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Integer userId = extractUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.ok(ApiResponse.error("用户未登录"));
            }

            int count = notificationService.getUnreadCount(userId);
            Map<String, Object> result = new HashMap<>();
            result.put("count", count);
            return ResponseEntity.ok(ApiResponse.success("获取成功", result));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取未读数量失败: " + e.getMessage()));
        }
    }

    @GetMapping("/notifications/{id}")
    public ResponseEntity<?> getNotificationDetail(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Integer id) {
        try {
            Integer userId = extractUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.ok(ApiResponse.error("用户未登录"));
            }

            UserNotification userNotification = notificationService.getUserNotificationById(userId, id);
            if (userNotification == null) {
                return ResponseEntity.ok(ApiResponse.error("通知不存在"));
            }

            if (userNotification.isUnread()) {
                notificationService.markAsRead(userId, userNotification.getNotificationId());
                userNotification.setIsRead(true);
            }

            return ResponseEntity.ok(ApiResponse.success("获取成功", userNotification));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取通知详情失败: " + e.getMessage()));
        }
    }

    @PostMapping("/notifications/read")
    public ResponseEntity<?> markAsRead(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Integer> request) {
        try {
            Integer userId = extractUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.ok(ApiResponse.error("用户未登录"));
            }

            Integer notificationId = request.get("notificationId");
            System.out.println("=== markAsRead request: userId=" + userId + ", notificationId=" + notificationId
                    + ", request=" + request);
            if (notificationId != null) {
                notificationService.markAsRead(userId, notificationId);
            } else {
                notificationService.markAllAsRead(userId);
            }
            return ResponseEntity.ok(ApiResponse.success("操作成功", null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("标记已读失败: " + e.getMessage()));
        }
    }

    @PostMapping("/notifications/delete")
    public ResponseEntity<?> deleteNotification(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Integer> request) {
        try {
            Integer userId = extractUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.ok(ApiResponse.error("用户未登录"));
            }

            Integer notificationId = request.get("notificationId");
            if (notificationId != null) {
                notificationService.deleteNotificationForUser(userId, notificationId);
            }
            return ResponseEntity.ok(ApiResponse.success("删除成功", null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("删除失败: " + e.getMessage()));
        }
    }

    @PostMapping("/notifications/batch-delete")
    public ResponseEntity<?> batchDeleteNotifications(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, List<Integer>> request) {
        try {
            Integer userId = extractUserIdFromToken(authHeader);
            System.out.println("=== batchDeleteNotifications: userId=" + userId + ", request=" + request);

            if (userId == null) {
                return ResponseEntity.ok(ApiResponse.error("用户未登录"));
            }

            List<Integer> notificationIds = request.get("notificationIds");
            System.out.println("=== notificationIds=" + notificationIds);

            if (notificationIds != null && !notificationIds.isEmpty()) {
                notificationService.batchDelete(userId, notificationIds);
            }
            return ResponseEntity.ok(ApiResponse.success("批量删除成功", null));
        } catch (Exception e) {
            System.out.println("=== batchDeleteNotifications error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(ApiResponse.error("批量删除失败: " + e.getMessage()));
        }
    }

    @GetMapping("/projects")
    public ResponseEntity<?> getProjects(
            @RequestParam(required = false) String keyword,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Integer userId = extractUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.ok(ApiResponse.error("用户未登录"));
            }

            List<Project> projects;
            if (keyword != null && !keyword.trim().isEmpty()) {
                projects = projectService.searchProjects(keyword.trim());
            } else {
                projects = projectService.getAllProjects();
            }

            return ResponseEntity.ok(ApiResponse.success("获取成功", projects));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取项目列表失败: " + e.getMessage()));
        }
    }
}
