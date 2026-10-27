package com.ceams.controller;

import com.ceams.model.entity.GeneralLedger;
import com.ceams.service.GeneralLedgerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/general-ledger")
public class GeneralLedgerApiController {

    @Autowired
    private GeneralLedgerService generalLedgerService;

    /**
     * 获取所有总账记录（支持分页）
     */
    @GetMapping
    public ResponseEntity<?> getAllLedgers(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            List<GeneralLedger> list = generalLedgerService.getLedgersWithPagination(pageNum, pageSize);
            long total = generalLedgerService.count();
            Map<String, Object> result = new HashMap<>();
            result.put("list", list);
            result.put("total", total);
            result.put("pageNum", pageNum);
            result.put("pageSize", pageSize);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取总账记录失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID获取总账记录
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getLedgerById(@PathVariable Long id) {
        try {
            GeneralLedger ledger = generalLedgerService.getLedgerById(id);
            if (ledger == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(ledger);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取总账记录失败：" + e.getMessage());
        }
    }

    /**
     * 根据账户名称搜索
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchByAccountName(@RequestParam String accountName) {
        try {
            List<GeneralLedger> list = generalLedgerService.getLedgersByAccountName(accountName);
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("搜索总账记录失败：" + e.getMessage());
        }
    }

    /**
     * 按日期范围查询
     */
    @GetMapping("/date-range")
    public ResponseEntity<?> getByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        try {
            List<GeneralLedger> list = generalLedgerService.getLedgersByDateRange(startDate, endDate);
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("按日期查询总账记录失败：" + e.getMessage());
        }
    }

    /**
     * 按状态查询
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<?> getByStatus(@PathVariable Integer status) {
        try {
            List<GeneralLedger> list = generalLedgerService.getLedgersByStatus(status);
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("按状态查询总账记录失败：" + e.getMessage());
        }
    }

    /**
     * 添加总账记录
     */
    @PostMapping
    public ResponseEntity<?> createLedger(@RequestBody GeneralLedger ledger) {
        try {
            ledger.setCreateTime(new Date());
            ledger.setUpdateTime(new Date());
            boolean success = generalLedgerService.addLedger(ledger);
            if (success) {
                return ResponseEntity.ok(ledger);
            }
            return ResponseEntity.badRequest().body("添加总账记录失败");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("添加总账记录失败：" + e.getMessage());
        }
    }

    /**
     * 更新总账记录
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateLedger(@PathVariable Long id, @RequestBody GeneralLedger ledger) {
        try {
            ledger.setId(id);
            ledger.setUpdateTime(new Date());
            boolean success = generalLedgerService.updateLedger(ledger);
            if (success) {
                return ResponseEntity.ok(ledger);
            }
            return ResponseEntity.badRequest().body("更新总账记录失败");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("更新总账记录失败：" + e.getMessage());
        }
    }

    /**
     * 删除总账记录
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLedger(@PathVariable Long id) {
        try {
            boolean success = generalLedgerService.deleteLedger(id);
            if (success) {
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.badRequest().body("删除总账记录失败");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("删除总账记录失败：" + e.getMessage());
        }
    }
}
