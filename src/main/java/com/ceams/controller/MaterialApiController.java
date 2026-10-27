package com.ceams.controller;

import com.ceams.model.PageResult;
import com.ceams.model.Pagination;
import com.ceams.model.entity.Material;
import com.ceams.service.MaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/materials")
public class MaterialApiController {

    @Autowired
    private MaterialService materialService;

    @GetMapping
    public ResponseEntity<?> getAllMaterials(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer supplierId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        
        if (categoryId != null) {
            List<Material> materials = materialService.getByCategory(categoryId);
            return ResponseEntity.ok(materials);
        } else if (supplierId != null) {
            List<Material> materials = materialService.getBySupplier(supplierId);
            return ResponseEntity.ok(materials);
        } else if (minPrice != null && maxPrice != null) {
            List<Material> materials = materialService.getByPriceRange(minPrice, maxPrice);
            return ResponseEntity.ok(materials);
        } else {
            Pagination pagination = new Pagination(pageNum, pageSize, keyword);
            PageResult<Material> result = materialService.getMaterialsByPage(pagination);
            Map<String, Object> response = new HashMap<>();
            response.put("list", result.getList());
            response.put("total", result.getTotal());
            response.put("pageNum", result.getPageNum());
            response.put("pageSize", result.getPageSize());
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMaterialById(@PathVariable Integer id) {
        Material material = materialService.getMaterialDetail(id);
        if (material == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(material);
    }

    @PostMapping
    public ResponseEntity<?> createMaterial(@RequestBody Material material) {
        boolean success = materialService.addMaterial(material);
        if (success) {
            return ResponseEntity.ok(material);
        }
        return ResponseEntity.badRequest().body("创建材料失败");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMaterial(@PathVariable Integer id, @RequestBody Material material) {
        material.setId(id);
        boolean success = materialService.updateMaterial(material);
        if (success) {
            return ResponseEntity.ok(material);
        }
        return ResponseEntity.badRequest().body("更新材料失败");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMaterial(@PathVariable Integer id) {
        boolean success = materialService.deleteMaterial(id);
        if (success) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("删除材料失败");
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        Map<String, Object> stats = materialService.getMaterialStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchMaterials(
            @RequestParam(required = false) String materialName,
            @RequestParam(required = false) String materialCode,
            @RequestParam(required = false) String specification,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Pagination pagination = new Pagination(pageNum, pageSize);
        PageResult<Material> result = materialService.searchMaterialsByPage(
            materialName, materialCode, specification, pagination);
        Map<String, Object> response = new HashMap<>();
        response.put("list", result.getList());
        response.put("total", result.getTotal());
        return ResponseEntity.ok(response);
    }
}
