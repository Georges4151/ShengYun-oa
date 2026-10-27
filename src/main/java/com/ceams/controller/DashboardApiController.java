package com.ceams.controller;

import com.ceams.model.entity.Project;
import com.ceams.model.entity.Receipt;
import com.ceams.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        Map<String, Object> statistics = dashboardService.getDashboardStatistics();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/recent-projects")
    public ResponseEntity<?> getRecentProjects(@RequestParam(defaultValue = "5") int limit) {
        List<Project> projects = dashboardService.getRecentProjects(limit);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/pending-approvals")
    public ResponseEntity<?> getPendingApprovals(@RequestParam(defaultValue = "10") int limit) {
        List<Receipt> receipts = dashboardService.getPendingApprovals(limit);
        return ResponseEntity.ok(receipts);
    }

    @GetMapping("/monthly-stats")
    public ResponseEntity<?> getMonthlyStats() {
        Map<String, Long> stats = dashboardService.getMonthlyStatistics();
        return ResponseEntity.ok(stats);
    }
}
