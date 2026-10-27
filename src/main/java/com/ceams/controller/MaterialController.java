package com.ceams.controller;

import com.ceams.model.entity.Material;
import com.ceams.service.MaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/material")
public class MaterialController {

    @Autowired
    private MaterialService materialService;

    @GetMapping("/list")
    public String list(Model model, @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer supplierId) {
        List<Material> materials;
        if (categoryId != null) {
            materials = materialService.getByCategory(categoryId);
        } else if (supplierId != null) {
            materials = materialService.getBySupplier(supplierId);
        } else {
            materials = materialService.getAllMaterials();
        }
        model.addAttribute("materials", materials);
        return "material/list";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("material", new Material());
        return "material/add";
    }

    @PostMapping("/add")
    public String add(@ModelAttribute Material material) {
        materialService.addMaterial(material);
        return "redirect:/material/list";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        Material material = materialService.getMaterialDetail(id);
        model.addAttribute("material", material);
        return "material/edit";
    }

    @PostMapping("/edit")
    public String edit(@ModelAttribute Material material) {
        materialService.updateMaterial(material);
        return "redirect:/material/list";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        materialService.deleteMaterial(id);
        return "redirect:/material/list";
    }

    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        Material material = materialService.getMaterialDetail(id);
        model.addAttribute("material", material);
        return "material/detail";
    }

    @GetMapping("/statistics")
    public String statistics(Model model) {
        Map<String, Object> stats = materialService.getMaterialStatistics();
        model.addAttribute("stats", stats);
        return "material/statistics";
    }
}