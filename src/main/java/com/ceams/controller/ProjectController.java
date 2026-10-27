package com.ceams.controller;

import com.ceams.model.entity.Project;
import com.ceams.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/project")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @GetMapping("/list")
    public String list(Model model, @RequestParam(required = false) String status,
            @RequestParam(required = false) String clientName) {
        List<Project> projects;
        if (status != null) {
            projects = projectService.getByStatus(status);
        } else if (clientName != null) {
            projects = projectService.getByClient(clientName);
        } else {
            projects = projectService.getAllProjects();
        }
        model.addAttribute("projects", projects);
        return "project/list";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("project", new Project());
        return "project/add";
    }

    @PostMapping("/add")
    public String add(@ModelAttribute Project project) {
        projectService.addProject(project);
        return "redirect:/project/list";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        Project project = projectService.getProjectDetail(id);
        model.addAttribute("project", project);
        return "project/edit";
    }

    @PostMapping("/edit")
    public String edit(@ModelAttribute Project project) {
        projectService.updateProject(project);
        return "redirect:/project/list";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        projectService.deleteProject(id);
        return "redirect:/project/list";
    }

    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        Project project = projectService.getProjectDetail(id);
        model.addAttribute("project", project);
        return "project/detail";
    }

    @GetMapping("/statistics")
    public String statistics(Model model) {
        Map<String, Object> stats = projectService.getProjectStatistics();
        model.addAttribute("stats", stats);
        return "project/statistics";
    }

    @GetMapping("/cost-analysis")
    public String costAnalysis(Model model) {
        List<Map<String, Object>> analysis = projectService.getProjectCostAnalysis();
        model.addAttribute("analysis", analysis);
        return "project/cost_analysis";
    }
}