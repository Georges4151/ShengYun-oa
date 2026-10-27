package com.ceams.controller;

import com.ceams.model.entity.CostAllocation;
import com.ceams.model.Pagination;
import com.ceams.service.CostAllocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/cost-allocation")
public class CostAllocationController {

    @Autowired
    private CostAllocationService costAllocationService;

    /**
     * 成本分摊列表页面
     * @param model 模型
     * @return 成本分摊列表视图
     */
    @GetMapping("/list")
    public String list(Model model, @RequestParam(required = false) Integer accountRecordId) {
        List<CostAllocation> allocations;
        if (accountRecordId != null) {
            allocations = costAllocationService.getByAccountRecordId(accountRecordId);
        } else {
            allocations = costAllocationService.getAllAllocations();
        }
        model.addAttribute("allocations", allocations);
        return "cost-allocation/list";
    }

    /**
     * 添加成本分摊页面
     * @param model 模型
     * @param accountRecordId 账目记录ID
     * @return 添加成本分摊视图
     */
    @GetMapping("/add")
    public String addForm(Model model, @RequestParam(required = false) Integer accountRecordId) {
        CostAllocation allocation = new CostAllocation();
        allocation.setAccountRecordId(accountRecordId);
        model.addAttribute("allocation", allocation);
        return "cost-allocation/add";
    }

    /**
     * 添加成本分摊
     * @param allocation 成本分摊对象
     * @return 重定向到成本分摊列表
     */
    @PostMapping("/add")
    public String add(@ModelAttribute CostAllocation allocation) {
        costAllocationService.addAllocation(allocation);
        return "redirect:/cost-allocation/list?accountRecordId=" + allocation.getAccountRecordId();
    }

    /**
     * 编辑成本分摊页面
     * @param id 成本分摊ID
     * @param model 模型
     * @return 编辑成本分摊视图
     */
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        CostAllocation allocation = costAllocationService.getAllocationById(id);
        model.addAttribute("allocation", allocation);
        return "cost-allocation/edit";
    }

    /**
     * 编辑成本分摊
     * @param allocation 成本分摊对象
     * @return 重定向到成本分摊列表
     */
    @PostMapping("/edit")
    public String edit(@ModelAttribute CostAllocation allocation) {
        costAllocationService.updateAllocation(allocation);
        return "redirect:/cost-allocation/list?accountRecordId=" + allocation.getAccountRecordId();
    }

    /**
     * 删除成本分摊
     * @param id 成本分摊ID
     * @return 重定向到成本分摊列表
     */
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        CostAllocation allocation = costAllocationService.getAllocationById(id);
        Integer accountRecordId = allocation.getAccountRecordId();
        costAllocationService.deleteAllocation(id);
        return "redirect:/cost-allocation/list?accountRecordId=" + accountRecordId;
    }

    /**
     * 根据账目记录ID获取成本分摊
     * @param accountRecordId 账目记录ID
     * @return 成本分摊列表
     */
    @GetMapping("/account/{accountRecordId}")
    @ResponseBody
    public List<CostAllocation> getByAccountRecordId(@PathVariable Integer accountRecordId) {
        return costAllocationService.getByAccountRecordId(accountRecordId);
    }

    /**
     * 根据目标ID和类型获取成本分摊
     * @param targetId 目标ID
     * @param targetType 目标类型
     * @return 成本分摊列表
     */
    @GetMapping("/target/{targetId}/{targetType}")
    @ResponseBody
    public List<CostAllocation> getByTargetIdAndType(@PathVariable Integer targetId, @PathVariable String targetType) {
        return costAllocationService.getByTargetIdAndType(targetId, targetType);
    }

    /**
     * 分页获取成本分摊
     * @param pagination 分页参数
     * @return 成本分摊列表
     */
    @GetMapping("/page")
    @ResponseBody
    public List<CostAllocation> getAllocationsWithPagination(Pagination pagination) {
        return costAllocationService.getAllocationsWithPagination(pagination);
    }

    /**
     * 根据条件分页获取成本分摊
     * @param accountRecordId 账目记录ID
     * @param targetType 目标类型
     * @param pagination 分页参数
     * @return 成本分摊列表
     */
    @PostMapping("/page/conditions")
    @ResponseBody
    public List<CostAllocation> getAllocationsByConditionsWithPagination(@RequestParam(required = false) Integer accountRecordId,
                                                                        @RequestParam(required = false) String targetType,
                                                                        Pagination pagination) {
        return costAllocationService.getAllocationsByConditionsWithPagination(accountRecordId, targetType, pagination);
    }
}
