package com.ceams.dao;

import com.ceams.model.entity.SubcontractorContract;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SubcontractorContractMapper {
    /**
     * 根据ID查询合同
     * 
     * @param id 合同ID
     * @return 合同信息
     */
    SubcontractorContract selectById(Integer id);

    /**
     * 根据分包商ID查询合同
     * 
     * @param subcontractorId 分包商ID
     * @return 合同列表
     */
    List<SubcontractorContract> selectBySubcontractorId(Integer subcontractorId);

    /**
     * 根据项目ID查询合同
     * 
     * @param projectId 项目ID
     * @return 合同列表
     */
    List<SubcontractorContract> selectByProjectId(Integer projectId);

    /**
     * 根据状态查询合同
     * 
     * @param status 合同状态
     * @return 合同列表
     */
    List<SubcontractorContract> selectByStatus(String status);

    /**
     * 插入合同
     * 
     * @param contract 合同信息
     * @return 影响行数
     */
    int insert(SubcontractorContract contract);

    /**
     * 更新合同
     * 
     * @param contract 合同信息
     * @return 影响行数
     */
    int update(SubcontractorContract contract);

    /**
     * 删除合同
     * 
     * @param id 合同ID
     * @return 影响行数
     */
    int deleteById(Integer id);

    /**
     * 查询所有合同
     * 
     * @return 合同列表
     */
    List<SubcontractorContract> selectAll();
}
