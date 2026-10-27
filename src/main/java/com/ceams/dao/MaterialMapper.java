package com.ceams.dao;

import com.ceams.model.entity.Material;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface MaterialMapper {
    /**
     * 根据ID查询材料
     * 
     * @param id 材料ID
     * @return 材料信息
     */
    Material selectById(Integer id);

    /**
     * 根据分类查询材料
     * 
     * @param categoryId 分类ID
     * @return 材料列表
     */
    List<Material> selectByCategory(@Param("categoryId") Integer categoryId);

    /**
     * 根据供应商查询材料
     * 
     * @param supplierId 供应商ID
     * @return 材料列表
     */
    List<Material> selectBySupplier(@Param("supplierId") Integer supplierId);

    /**
     * 根据价格范围查询材料
     * 
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @return 材料列表
     */
    List<Material> selectByPriceRange(@Param("minPrice") Double minPrice, @Param("maxPrice") Double maxPrice);

    /**
     * 获取材料统计信息
     * 
     * @return 统计信息
     */
    Map<String, Object> getMaterialStatistics();

    /**
     * 插入材料
     * 
     * @param material 材料信息
     * @return 影响行数
     */
    int insert(Material material);

    /**
     * 更新材料
     * 
     * @param material 材料信息
     * @return 影响行数
     */
    int update(Material material);

    /**
     * 删除材料
     * 
     * @param id 材料ID
     * @return 影响行数
     */
    int deleteById(Integer id);

    /**
     * 查询所有材料
     * 
     * @return 材料列表
     */
    List<Material> selectAll();

    /**
     * 根据条件模糊查询材料
     * 
     * @param materialName  材料名称
     * @param materialCode  材料编码
     * @param specification 规格
     * @return 材料列表
     */
    List<Material> selectByLike(@Param("materialName") String materialName,
            @Param("materialCode") String materialCode,
            @Param("specification") String specification);

    /**
     * 分页查询所有材料
     * 
     * @param pagination 分页参数
     * @return 材料列表
     */
    List<Material> selectByPage(@Param("pagination") com.ceams.model.Pagination pagination);

    /**
     * 统计所有材料数量
     * 
     * @return 材料数量
     */
    long count();

    /**
     * 分页根据条件模糊查询材料
     * 
     * @param materialName  材料名称
     * @param materialCode  材料编码
     * @param specification 规格
     * @param pagination    分页参数
     * @return 材料列表
     */
    List<Material> selectByLikeAndPage(@Param("materialName") String materialName,
            @Param("materialCode") String materialCode,
            @Param("specification") String specification,
            @Param("pagination") com.ceams.model.Pagination pagination);

    /**
     * 统计根据条件模糊查询的材料数量
     * 
     * @param materialName  材料名称
     * @param materialCode  材料编码
     * @param specification 规格
     * @return 材料数量
     */
    long countByLike(@Param("materialName") String materialName,
            @Param("materialCode") String materialCode,
            @Param("specification") String specification);
}