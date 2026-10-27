package com.ceams.service;

import com.ceams.dao.MaterialMapper;
import com.ceams.model.PageResult;
import com.ceams.model.Pagination;
import com.ceams.model.entity.Material;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MaterialService {

    @Autowired
    private MaterialMapper materialMapper;

    public List<Material> getByCategory(Integer categoryId) {
        return materialMapper.selectByCategory(categoryId);
    }

    public List<Material> getMaterialsByCategoryId(Integer categoryId) {
        return materialMapper.selectByCategory(categoryId);
    }

    public List<Material> getBySupplier(Integer supplierId) {
        return materialMapper.selectBySupplier(supplierId);
    }

    public List<Material> getMaterialsBySupplierId(Integer supplierId) {
        return materialMapper.selectBySupplier(supplierId);
    }

    public List<Material> getMaterialsByCategoryIdAndSupplierId(Integer categoryId, Integer supplierId) {
        // 这里可以实现根据分类ID和供应商ID查询材料的逻辑
        // 暂时返回所有记录，后续可以扩展
        return materialMapper.selectAll();
    }

    public List<Material> getByPriceRange(Double minPrice, Double maxPrice) {
        return materialMapper.selectByPriceRange(minPrice, maxPrice);
    }

    public Map<String, Object> getMaterialStatistics() {
        return materialMapper.getMaterialStatistics();
    }

    public boolean addMaterial(Material material) {
        return materialMapper.insert(material) > 0;
    }

    public boolean updateMaterial(Material material) {
        return materialMapper.update(material) > 0;
    }

    public boolean deleteMaterial(Integer materialId) {
        return materialMapper.deleteById(materialId) > 0;
    }

    public Material getMaterialDetail(Integer materialId) {
        return materialMapper.selectById(materialId);
    }

    public List<Material> searchMaterials(Map<String, Object> params) {
        // 这里可以实现更复杂的搜索逻辑，如多条件组合查询
        // 暂时返回所有记录，后续可以扩展
        return materialMapper.selectAll();
    }

    public List<Material> searchMaterialsByLike(String materialName, String materialCode, String specification) {
        return materialMapper.selectByLike(materialName, materialCode, specification);
    }

    public List<Material> getAllMaterials() {
        return materialMapper.selectAll();
    }

    /**
     * 分页获取所有材料
     * 
     * @param pagination 分页参数
     * @return 分页结果
     */
    public PageResult<Material> getMaterialsByPage(Pagination pagination) {
        List<Material> list = materialMapper.selectByPage(pagination);
        long total = materialMapper.count();
        return new PageResult<>(list, total, pagination.getPageNum(), pagination.getPageSize());
    }

    /**
     * 分页根据关键字搜索材料
     * 
     * @param materialName 材料名称
     * @param materialCode 材料编码
     * @param specification 规格
     * @param pagination 分页参数
     * @return 分页结果
     */
    public PageResult<Material> searchMaterialsByPage(String materialName, String materialCode, String specification, Pagination pagination) {
        List<Material> list = materialMapper.selectByLikeAndPage(materialName, materialCode, specification, pagination);
        long total = materialMapper.countByLike(materialName, materialCode, specification);
        return new PageResult<>(list, total, pagination.getPageNum(), pagination.getPageSize());
    }

    /**
     * 获取所有材料记录总数
     * 
     * @return 材料记录总数
     */
    public long count() {
        return materialMapper.count();
    }
}