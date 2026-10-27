package com.ceams.service;

import com.ceams.dao.ReceiptMapper;
import com.ceams.dao.ProjectMapper;
import com.ceams.dao.UserMapper;
import com.ceams.model.entity.Receipt;
import com.ceams.model.entity.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    @Autowired
    private ReceiptMapper receiptMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private UserMapper userMapper;

    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        statistics.put("projectCount", projectMapper.count());
        statistics.put("receiptCount", receiptMapper.count());
        statistics.put("userCount", userMapper.count());

        long pendingReceipts = receiptMapper.selectByVerificationStatus("pending").size();
        statistics.put("pendingReceipts", pendingReceipts);

        long financePending = receiptMapper.selectByFinanceStatus("pending").size();
        statistics.put("financePending", financePending);

        long projectManagerPending = receiptMapper.selectByProjectManagerStatus("pending").size();
        statistics.put("projectManagerPending", projectManagerPending);

        long managerPending = receiptMapper.selectByManagerStatus("pending").size();
        statistics.put("managerPending", managerPending);

        return statistics;
    }

    public List<Project> getRecentProjects(int limit) {
        return projectMapper.selectRecentProjects(limit);
    }

    public List<Receipt> getPendingApprovals(int limit) {
        return receiptMapper.selectPendingApprovals(limit);
    }

    public Map<String, Long> getMonthlyStatistics() {
        Map<String, Long> monthlyStats = new HashMap<>();

        monthlyStats.put("newProjects", projectMapper.countThisMonth());
        monthlyStats.put("newReceipts", receiptMapper.countThisMonth());

        return monthlyStats;
    }
}
