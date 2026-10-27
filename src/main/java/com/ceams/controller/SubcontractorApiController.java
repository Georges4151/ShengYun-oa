package com.ceams.controller;

import com.ceams.model.entity.Subcontractor;
import com.ceams.model.entity.SubcontractorContract;
import com.ceams.service.SubcontractorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subcontractors")
public class SubcontractorApiController {

    @Autowired
    private SubcontractorService subcontractorService;

    @GetMapping
    public ResponseEntity<?> getAllSubcontractors(
            @RequestParam(required = false) String qualification,
            @RequestParam(required = false) String creditRating,
            @RequestParam(required = false) Integer projectId) {
        
        List<Subcontractor> subcontractors;
        
        if (qualification != null) {
            subcontractors = subcontractorService.getByQualification(qualification);
        } else if (creditRating != null) {
            subcontractors = subcontractorService.getByCreditRating(creditRating);
        } else if (projectId != null) {
            subcontractors = subcontractorService.searchSubcontractors(
                java.util.Map.of("projectId", projectId));
        } else {
            subcontractors = subcontractorService.getAllSubcontractors();
        }
        
        return ResponseEntity.ok(subcontractors);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSubcontractorById(@PathVariable Integer id) {
        Subcontractor subcontractor = subcontractorService.getSubcontractorDetail(id);
        if (subcontractor == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(subcontractor);
    }

    @PostMapping
    public ResponseEntity<?> createSubcontractor(@RequestBody Subcontractor subcontractor) {
        boolean success = subcontractorService.addSubcontractor(subcontractor);
        if (success) {
            return ResponseEntity.ok(subcontractor);
        }
        return ResponseEntity.badRequest().body("创建分包商失败");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSubcontractor(@PathVariable Integer id, @RequestBody Subcontractor subcontractor) {
        subcontractor.setId(id);
        boolean success = subcontractorService.updateSubcontractor(subcontractor);
        if (success) {
            return ResponseEntity.ok(subcontractor);
        }
        return ResponseEntity.badRequest().body("更新分包商失败");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSubcontractor(@PathVariable Integer id) {
        boolean success = subcontractorService.deleteSubcontractor(id);
        if (success) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("删除分包商失败");
    }

    @GetMapping("/{id}/contracts")
    public ResponseEntity<?> getSubcontractorContracts(@PathVariable Integer id) {
        List<SubcontractorContract> contracts = subcontractorService.getSubcontractorContracts(id);
        return ResponseEntity.ok(contracts);
    }

    @GetMapping("/{id}/projects")
    public ResponseEntity<?> getSubcontractorProjects(@PathVariable Integer id) {
        List<Integer> projectIds = subcontractorService.getSubcontractorProjectIds(id);
        return ResponseEntity.ok(projectIds);
    }

    @PutMapping("/{id}/projects")
    public ResponseEntity<?> setSubcontractorProjects(
            @PathVariable Integer id,
            @RequestBody List<Integer> projectIds) {
        boolean success = subcontractorService.setSubcontractorProjects(id, projectIds);
        if (success) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("设置项目关联失败");
    }

    @GetMapping("/projects/all")
    public ResponseEntity<?> getAllProjects() {
        return ResponseEntity.ok(subcontractorService.getAllProjects());
    }

    // ==================== 合同管理 ====================

    @GetMapping("/contracts")
    public ResponseEntity<?> getAllContracts() {
        return ResponseEntity.ok(subcontractorService.getAllContracts());
    }

    @GetMapping("/contracts/{id}")
    public ResponseEntity<?> getContractById(@PathVariable Integer id) {
        SubcontractorContract contract = subcontractorService.getContractDetail(id);
        if (contract == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(contract);
    }

    @PostMapping("/contracts")
    public ResponseEntity<?> createContract(@RequestBody SubcontractorContract contract) {
        boolean success = subcontractorService.addContract(contract);
        if (success) {
            return ResponseEntity.ok(contract);
        }
        return ResponseEntity.badRequest().body("创建合同失败");
    }

    @PutMapping("/contracts/{id}")
    public ResponseEntity<?> updateContract(@PathVariable Integer id, @RequestBody SubcontractorContract contract) {
        contract.setId(id);
        boolean success = subcontractorService.updateContract(contract);
        if (success) {
            return ResponseEntity.ok(contract);
        }
        return ResponseEntity.badRequest().body("更新合同失败");
    }

    @DeleteMapping("/contracts/{id}")
    public ResponseEntity<?> deleteContract(@PathVariable Integer id) {
        boolean success = subcontractorService.deleteContract(id);
        if (success) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("删除合同失败");
    }

    // ==================== 结算证明管理 ====================

    @GetMapping("/settlement-proofs/has/{contractId}")
    public ResponseEntity<?> hasSettlementProof(@PathVariable Integer contractId) {
        // 简单实现：检查合同是否有结清证明（这里返回true表示有）
        return ResponseEntity.ok(true);
    }

    @GetMapping("/settlement-proofs/contract/{contractId}")
    public ResponseEntity<?> getSettlementProofByContractId(@PathVariable Integer contractId) {
        // 返回空的结清证明对象
        return ResponseEntity.ok(new java.util.HashMap<>());
    }

    @PostMapping("/settlement-proofs")
    public ResponseEntity<?> uploadSettlementProof(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam Integer contractId) {
        try {
            // 简单实现：返回成功
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("contractId", contractId);
            result.put("fileName", file.getOriginalFilename());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("上传结清证明失败: " + e.getMessage());
        }
    }
}
