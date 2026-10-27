package com.ceams.controller;

import com.ceams.model.entity.Budget;
import com.ceams.model.Pagination;
import com.ceams.service.BudgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/budget")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    /**
     * 预算列表页面
     * @param model 模型
     * @return 预算列表视图
     */
    @GetMapping("/list")
    public String list(Model model, @RequestParam(required = false) Integer projectId,
                       @RequestParam(required = false) Integer contractId,
                       @RequestParam(required = false) String status) {
        List<Budget> budgets;
        if (projectId != null) {
            budgets = budgetService.getByProjectId(projectId);
        } else if (contractId != null) {
            budgets = budgetService.getByContractId(contractId);
        } else {
            budgets = budgetService.getAllBudgets();
        }
        model.addAttribute("budgets", budgets);
        return "budget/list";
    }

    /**
     * 添加预算页面
     * @param model 模型
     * @return 添加预算视图
     */
    @GetMapping("/add")
    public String addForm(Model model) {
        Budget budget = new Budget();
        budget.setStatus("active");
        model.addAttribute("budget", budget);
        return "budget/add";
    }

    /**
     * 添加预算
     * @param budget 预算对象
     * @return 重定向到预算列表
     */
    @PostMapping("/add")
    public String add(@ModelAttribute Budget budget) {
        budgetService.addBudget(budget);
        return "redirect:/budget/list";
    }

    /**
     * 编辑预算页面
     * @param id 预算ID
     * @param model 模型
     * @return 编辑预算视图
     */
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        Budget budget = budgetService.getBudgetById(id);
        model.addAttribute("budget", budget);
        return "budget/edit";
    }

    /**
     * 编辑预算
     * @param budget 预算对象
     * @return 重定向到预算列表
     */
    @PostMapping("/edit")
    public String edit(@ModelAttribute Budget budget) {
        budgetService.updateBudget(budget);
        return "redirect:/budget/list";
    }

    /**
     * 删除预算
     * @param id 预算ID
     * @return 重定向到预算列表
     */
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        budgetService.deleteBudget(id);
        return "redirect:/budget/list";
    }

    /**
     * 获取预算执行情况
     * @param id 预算ID
     * @return 预算执行情况
     */
    @GetMapping("/execution/{id}")
    @ResponseBody
    public Map<String, Object> getExecution(@PathVariable Integer id) {
        return budgetService.getBudgetExecution(id);
    }

    /**
     * 分页获取预算列表
     * @param pagination 分页参数
     * @return 预算列表
     */
    @GetMapping("/page")
    @ResponseBody
    public List<Budget> getBudgetsWithPagination(Pagination pagination) {
        return budgetService.getBudgetsWithPagination(pagination);
    }

    /**
     * 根据条件分页获取预算列表
     * @param projectId 项目ID
     * @param contractId 合同ID
     * @param status 状态
     * @param pagination 分页参数
     * @return 预算列表
     */
    @PostMapping("/page/conditions")
    @ResponseBody
    public List<Budget> getBudgetsByConditionsWithPagination(@RequestParam(required = false) Integer projectId,
                                                             @RequestParam(required = false) Integer contractId,
                                                             @RequestParam(required = false) String status,
                                                             Pagination pagination) {
        return budgetService.getBudgetsByConditionsWithPagination(projectId, contractId, status, pagination);
    }
}
