package com.ceams.module.project.converter;

import com.ceams.module.project.entity.Budget;
import com.ceams.module.project.vo.BudgetVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 预算实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface BudgetConverter {

    default BudgetVO toVO(Budget budget) {
        if (budget == null) {
            return null;
        }
        BudgetVO vo = new BudgetVO();
        vo.setId(budget.getId());
        vo.setBudgetName(budget.getBudgetName());
        vo.setAmount(budget.getAmount());
        vo.setProjectId(budget.getProjectId());
        vo.setContractId(budget.getContractId());
        vo.setStartDate(budget.getStartDate() == null ? null : budget.getStartDate().toLocalDate());
        vo.setEndDate(budget.getEndDate() == null ? null : budget.getEndDate().toLocalDate());
        vo.setStatus(budget.getStatus());
        vo.setUsedAmount(budget.getUsedAmount());
        vo.setRemainingAmount(budget.getRemainingAmount());
        vo.setCreateTime(budget.getCreateTime());
        return vo;
    }

    List<BudgetVO> toVOList(List<Budget> budgets);
}
