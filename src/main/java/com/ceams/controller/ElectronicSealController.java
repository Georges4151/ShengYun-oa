package com.ceams.controller;

import com.ceams.model.entity.ElectronicSeal;
import com.ceams.model.entity.ElectronicSealApplication;
import com.ceams.model.entity.ElectronicSealStamp;
import com.ceams.model.entity.ElectronicSealUsage;
import com.ceams.service.ElectronicSealService;
import com.ceams.service.UserSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/electronic-seal")
public class ElectronicSealController {

    @Autowired
    private ElectronicSealService electronicSealService;

    @Autowired
    private UserSessionService userSessionService;

    // 电子章管理
    @GetMapping("/seals")
    public List<ElectronicSeal> getAllSeals() {
        return electronicSealService.getAllSeals();
    }

    @GetMapping("/seals/{id}")
    public ElectronicSeal getSealById(@PathVariable Integer id) {
        return electronicSealService.getSealById(id);
    }

    @PostMapping("/seals")
    public void addSeal(@RequestBody ElectronicSeal seal) {
        electronicSealService.addSeal(seal);
    }

    @PutMapping("/seals/{id}")
    public void updateSeal(@PathVariable Integer id, @RequestBody ElectronicSeal seal) {
        seal.setId(id);
        electronicSealService.updateSeal(seal);
    }

    @DeleteMapping("/seals/{id}")
    public void deleteSeal(@PathVariable Integer id) {
        electronicSealService.deleteSeal(id);
    }

    // 电子章申请管理
    @PostMapping("/applications")
    public void submitApplication(@RequestBody ElectronicSealApplication application) {
        Integer currentUserId = userSessionService.getCurrentUserId();
        if (currentUserId != null) {
            application.setApplicantId(currentUserId);
            application.setApplicantName(userSessionService.getCurrentUserName());
            electronicSealService.submitApplication(application);
        }
    }

    @GetMapping("/applications/my")
    public List<ElectronicSealApplication> getMyApplications() {
        Integer currentUserId = userSessionService.getCurrentUserId();
        if (currentUserId != null) {
            return electronicSealService.getApplicationsByApplicantId(currentUserId);
        }
        return List.of();
    }

    @GetMapping("/applications/pending")
    public List<ElectronicSealApplication> getPendingApplications() {
        return electronicSealService.getPendingApplications();
    }

    @PutMapping("/applications/{id}/approve")
    public void approveApplication(@PathVariable Integer id, @RequestParam String comment) {
        Integer currentUserId = userSessionService.getCurrentUserId();
        if (currentUserId != null) {
            electronicSealService.approveApplication(id, currentUserId, userSessionService.getCurrentUserName(),
                    comment);
        }
    }

    @PutMapping("/applications/{id}/reject")
    public void rejectApplication(@PathVariable Integer id, @RequestParam String comment) {
        Integer currentUserId = userSessionService.getCurrentUserId();
        if (currentUserId != null) {
            electronicSealService.rejectApplication(id, currentUserId, userSessionService.getCurrentUserName(),
                    comment);
        }
    }

    // 电子章使用管理
    @GetMapping("/can-use/{sealId}")
    public boolean canUseSeal(@PathVariable Integer sealId) {
        Integer currentUserId = userSessionService.getCurrentUserId();
        if (currentUserId != null) {
            return electronicSealService.canUseSeal(currentUserId, sealId);
        }
        return false;
    }

    @PostMapping("/usage")
    public void recordUsage(@RequestBody ElectronicSealUsage usage) {
        Integer currentUserId = userSessionService.getCurrentUserId();
        if (currentUserId != null) {
            usage.setUserId(currentUserId);
            usage.setUserName(userSessionService.getCurrentUserName());
            electronicSealService.recordSealUsage(usage);
        }
    }

    // 电子章导入导出
    @PostMapping("/import")
    public void importSeal(@RequestBody ElectronicSeal seal) {
        electronicSealService.importSeal(seal);
    }

    @GetMapping("/export")
    public List<ElectronicSeal> exportSeals() {
        return electronicSealService.exportSeals();
    }

    @GetMapping("/usage/my")
    public List<ElectronicSealUsage> getMyUsages() {
        Integer currentUserId = userSessionService.getCurrentUserId();
        if (currentUserId != null) {
            return electronicSealService.getUsagesByUserId(currentUserId);
        }
        return List.of();
    }

    @GetMapping("/usage/seal/{sealId}")
    public List<ElectronicSealUsage> getSealUsages(@PathVariable Integer sealId) {
        return electronicSealService.getUsagesBySealId(sealId);
    }

    // 电子章盖章相关
    @PostMapping("/stamp")
    public void stampContract(@RequestBody ElectronicSealStamp stamp) {
        Integer currentUserId = userSessionService.getCurrentUserId();
        if (currentUserId != null) {
            stamp.setUserId(currentUserId);
            stamp.setUserName(userSessionService.getCurrentUserName());
            stamp.setIpAddress("127.0.0.1");
            electronicSealService.stampContract(stamp);
        }
    }

    @GetMapping("/stamps/project/{projectId}")
    public List<ElectronicSealStamp> getProjectStamps(@PathVariable Integer projectId) {
        return electronicSealService.getStampsByProjectId(projectId);
    }

    @GetMapping("/stamps/my")
    public List<ElectronicSealStamp> getMyStamps() {
        Integer currentUserId = userSessionService.getCurrentUserId();
        if (currentUserId != null) {
            return electronicSealService.getStampsByUserId(currentUserId);
        }
        return List.of();
    }
}