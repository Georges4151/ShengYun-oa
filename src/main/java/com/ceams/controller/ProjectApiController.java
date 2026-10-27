package com.ceams.controller;

import com.ceams.model.PageResult;
import com.ceams.model.Pagination;
import com.ceams.model.dto.ProjectCostAnalysisDTO;
import com.ceams.model.entity.Project;
import com.ceams.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectApiController {

    @Autowired
    private ProjectService projectService;

    @GetMapping
    public ResponseEntity<?> getAllProjects() {
        List<Project> projects = projectService.getAllProjects();
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/page")
    public ResponseEntity<?> getProjectsByPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        Pagination pagination = new Pagination(pageNum, pageSize, keyword);
        PageResult<Project> result;
        if (keyword != null && !keyword.trim().isEmpty()) {
            result = projectService.searchProjectsByPage(pagination);
        } else {
            result = projectService.getProjectsByPage(pagination);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("list", result.getList());
        response.put("total", result.getTotal());
        response.put("pageNum", result.getPageNum());
        response.put("pageSize", result.getPageSize());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProjectById(@PathVariable Integer id) {
        Project project = projectService.getProjectDetail(id);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(project);
    }

    @PostMapping
    public ResponseEntity<?> createProject(@RequestBody Project project) {
        boolean success = projectService.addProject(project);
        if (success) {
            return ResponseEntity.ok(project);
        }
        return ResponseEntity.badRequest().body("创建项目失败");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProject(@PathVariable Integer id, @RequestBody Project project) {
        project.setId(id);
        boolean success = projectService.updateProject(project);
        if (success) {
            return ResponseEntity.ok(project);
        }
        return ResponseEntity.badRequest().body("更新项目失败");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable Integer id) {
        boolean success = projectService.deleteProject(id);
        if (success) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("删除项目失败");
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<?> getProjectsByStatus(@PathVariable String status) {
        List<Project> projects = projectService.getByStatus(status);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchProjects(@RequestParam String keyword) {
        List<Project> projects = projectService.searchProjects(keyword);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        Map<String, Object> stats = projectService.getProjectStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/cost-analysis")
    public ResponseEntity<?> getCostAnalysis() {
        List<ProjectCostAnalysisDTO> analysis = projectService.getProjectCostAnalysisDTO();
        return ResponseEntity.ok(analysis);
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActiveProjects() {
        List<Project> projects = projectService.getByStatus("active");
        return ResponseEntity.ok(projects);
    }

    @PutMapping("/{id}/manager")
    public ResponseEntity<?> bindManager(
            @PathVariable Integer id,
            @RequestParam Integer managerId,
            @RequestParam String managerName) {
        boolean success = projectService.bindManager(id, managerId, managerName);
        if (success) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("绑定项目经理失败");
    }

    @DeleteMapping("/{id}/manager")
    public ResponseEntity<?> unbindManager(@PathVariable Integer id) {
        boolean success = projectService.unbindManager(id);
        if (success) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("解绑项目经理失败");
    }
}
