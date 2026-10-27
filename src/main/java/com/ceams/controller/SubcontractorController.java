package com.ceams.controller;

import com.ceams.model.entity.Subcontractor;
import com.ceams.service.SubcontractorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/subcontractor")
public class SubcontractorController {

    @Autowired
    private SubcontractorService subcontractorService;

    @GetMapping("/list")
    public String list(Model model, @RequestParam(required = false) String qualification,
            @RequestParam(required = false) String creditRating) {
        List<Subcontractor> subcontractors;
        if (qualification != null) {
            subcontractors = subcontractorService.getByQualification(qualification);
        } else if (creditRating != null) {
            subcontractors = subcontractorService.getByCreditRating(creditRating);
        } else {
            subcontractors = subcontractorService.getAllSubcontractors();
        }
        model.addAttribute("subcontractors", subcontractors);
        return "subcontractor/list";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("subcontractor", new Subcontractor());
        return "subcontractor/add";
    }

    @PostMapping("/add")
    public String add(@ModelAttribute Subcontractor subcontractor) {
        subcontractorService.addSubcontractor(subcontractor);
        return "redirect:/subcontractor/list";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        Subcontractor subcontractor = subcontractorService.getSubcontractorDetail(id);
        model.addAttribute("subcontractor", subcontractor);
        return "subcontractor/edit";
    }

    @PostMapping("/edit")
    public String edit(@ModelAttribute Subcontractor subcontractor) {
        subcontractorService.updateSubcontractor(subcontractor);
        return "redirect:/subcontractor/list";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        subcontractorService.deleteSubcontractor(id);
        return "redirect:/subcontractor/list";
    }

    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        Subcontractor subcontractor = subcontractorService.getSubcontractorDetail(id);
        model.addAttribute("subcontractor", subcontractor);
        return "subcontractor/detail";
    }
}