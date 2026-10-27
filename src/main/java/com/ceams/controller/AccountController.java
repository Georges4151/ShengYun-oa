package com.ceams.controller;

import com.ceams.model.entity.Account;
import com.ceams.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/account")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @GetMapping("/list")
    public String list(Model model, @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) Date startDate, @RequestParam(required = false) Date endDate) {
        List<Account> accounts;
        if (projectId != null) {
            accounts = accountService.getByProjectId(projectId);
        } else if (startDate != null && endDate != null) {
            accounts = accountService.getByDateRange(startDate, endDate);
        } else {
            accounts = accountService.getAllAccounts();
        }
        model.addAttribute("accounts", accounts);
        return "account/list";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        Account account = new Account();
        account.setUserId(1); // 默认用户ID为1（管理员）
        account.setStatus("draft"); // 默认状态为草稿
        model.addAttribute("account", account);
        return "account/add";
    }

    @PostMapping("/add")
    public String add(@ModelAttribute Account account) {
        accountService.addAccount(account);
        return "redirect:/account/list";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        Account account = accountService.getAccountDetail(id);
        model.addAttribute("account", account);
        return "account/edit";
    }

    @PostMapping("/edit")
    public String edit(@ModelAttribute Account account) {
        accountService.updateAccount(account);
        return "redirect:/account/list";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        accountService.deleteAccount(id);
        return "redirect:/account/list";
    }

    @GetMapping("/summary")
    @ResponseBody
    public Map<String, Object> summary(@RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) Date startDate, @RequestParam(required = false) Date endDate) {
        return accountService.getAccountSummary(projectId, startDate, endDate);
    }

    @GetMapping("/monthly-stats/{year}")
    @ResponseBody
    public List<Map<String, Object>> monthlyStats(@PathVariable Integer year) {
        return accountService.getMonthlyStats(year);
    }

    @PostMapping("/search")
    @ResponseBody
    public List<Account> search(@RequestBody Map<String, Object> params) {
        return accountService.searchAccounts(params);
    }

    /**
     * 从合同创建账目记录
     * @param account 账目对象
     * @return 重定向到账目列表
     */
    @PostMapping("/from-contract")
    public String addFromContract(@ModelAttribute Account account) {
        accountService.addAccountFromContract(account);
        return "redirect:/account/list";
    }

    /**
     * 获取合同相关账目记录
     * @param contractId 合同ID
     * @return 账目列表
     */
    @GetMapping("/contract/{contractId}")
    @ResponseBody
    public List<Account> getByContractId(@PathVariable Integer contractId) {
        return accountService.getByContractId(contractId);
    }

    /**
     * 更新合同已支付金额
     * @param contractId 合同ID
     * @return 更新结果
     */
    @PutMapping("/update-contract-paid/{contractId}")
    @ResponseBody
    public boolean updateContractPaidAmount(@PathVariable Integer contractId) {
        return accountService.updateContractPaidAmount(contractId);
    }
}