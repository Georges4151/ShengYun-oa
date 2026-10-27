package com.ceams.dao;

import com.ceams.model.entity.Subcontractor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SubcontractorMapper {
    /**
     * 根据ID查询分包商
     * 
     * @param id 分包商ID
     * @return 分包商信息
     */
    Subcontractor selectById(Integer id);

    /**
     * 根据资质等级查询分包商
     * 
     * @param qualificationLevel 资质等级
     * @return 分包商列表
     */
    List<Subcontractor> selectByQualification(@Param("qualificationLevel") String qualificationLevel);

    /**
     * 根据信用等级查询分包商
     * 
     * @param creditRating 信用等级
     * @return 分包商列表
     */
    List<Subcontractor> selectByCreditRating(@Param("creditRating") String creditRating);

    /**
     * 根据名称查询分包商
     * 
     * @param name 分包商名称
     * @return 分包商列表
     */
    List<Subcontractor> selectByName(String name);

    /**
     * 根据多条件查询分包商
     * 
     * @param name          分包商名称
     * @param contactPerson 联系人
     * @param phone         电话
     * @return 分包商列表
     */
    List<Subcontractor> selectByConditions(@Param("name") String name, @Param("contactPerson") String contactPerson,
            @Param("phone") String phone);

    /**
     * 插入分包商
     * 
     * @param subcontractor 分包商信息
     * @return 影响行数
     */
    int insert(Subcontractor subcontractor);

    /**
     * 更新分包商
     * 
     * @param subcontractor 分包商信息
     * @return 影响行数
     */
    int update(Subcontractor subcontractor);

    /**
     * 删除分包商
     * 
     * @param id 分包商ID
     * @return 影响行数
     */
    int deleteById(Integer id);

    /**
     * 查询所有分包商
     * 
     * @return 分包商列表
     */
    List<Subcontractor> selectAll();

    /**
     * 为分包商添加项目关联
     * 
     * @param subcontractorId 分包商ID
     * @param projectId       项目ID
     * @return 影响行数
     */
    int addProjectRelation(@Param("subcontractorId") Integer subcontractorId, @Param("projectId") Integer projectId);

    /**
     * 为分包商移除项目关联
     * 
     * @param subcontractorId 分包商ID
     * @param projectId       项目ID
     * @return 影响行数
     */
    int removeProjectRelation(@Param("subcontractorId") Integer subcontractorId, @Param("projectId") Integer projectId);

    /**
     * 查询分包商的关联项目
     * 
     * @param subcontractorId 分包商ID
     * @return 项目ID列表
     */
    List<Integer> selectProjectIdsBySubcontractorId(Integer subcontractorId);

    /**
     * 查询分包商的合同数量
     * 
     * @param subcontractorId 分包商ID
     * @return 合同数量
     */
    Integer selectContractCountBySubcontractorId(Integer subcontractorId);

    /**
     * 删除分包商的所有项目关联
     * 
     * @param subcontractorId 分包商ID
     * @return 影响行数
     */
    int deleteProjectRelations(Integer subcontractorId);

    /**
     * 根据项目ID查询分包商
     * 
     * @param projectId 项目ID
     * @return 分包商列表
     */
    List<Subcontractor> selectByProjectId(Integer projectId);
}
