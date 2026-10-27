package com.example.android_client;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.android_client.model.ApiResponse;
import com.example.android_client.model.StampApproval;
import com.example.android_client.network.RetrofitClient;
import com.example.android_client.util.TokenManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StampApprovalListActivity extends AppCompatActivity implements StampApprovalAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private StampApprovalAdapter adapter;
    private TokenManager tokenManager;
    private List<StampApproval> approvalList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stamp_approval_list);

        tokenManager = TokenManager.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("合同盖章审批");
        }

        recyclerView = findViewById(R.id.recycler_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        progressBar = findViewById(R.id.progress_bar);
        tvEmpty = findViewById(R.id.tv_empty);

        adapter = new StampApprovalAdapter(approvalList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(this::loadPendingApprovals);

        if (!hasStampApprovalPermission()) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("权限不足\n\n仅限经理和管理员进行盖章审批");
            recyclerView.setVisibility(View.GONE);
            swipeRefreshLayout.setEnabled(false);
            return;
        }

        loadPendingApprovals();
    }

    private boolean hasStampApprovalPermission() {
        String role = tokenManager.getRole();
        if (role == null)
            return false;
        String upperRole = role.toUpperCase();
        return "MANAGER".equals(upperRole) || "ADMIN".equals(upperRole);
    }

    private void loadPendingApprovals() {
        if (!hasStampApprovalPermission()) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        RetrofitClient.getInstance().getApiService()
                .getPendingStampApprovals()
                .enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                            Response<ApiResponse<Map<String, Object>>> response) {
                        progressBar.setVisibility(View.GONE);
                        swipeRefreshLayout.setRefreshing(false);

                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<Map<String, Object>> apiResponse = response.body();
                            if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                Map<String, Object> data = apiResponse.getData();
                                Object listObj = data.get("list");
                                if (listObj != null) {
                                    Gson gson = new Gson();
                                    String json = gson.toJson(listObj);
                                    List<StampApproval> list = gson.fromJson(json,
                                            new TypeToken<List<StampApproval>>() {
                                            }.getType());
                                    approvalList.clear();
                                    approvalList.addAll(list);
                                    adapter.notifyDataSetChanged();

                                    if (approvalList.isEmpty()) {
                                        tvEmpty.setVisibility(View.VISIBLE);
                                        tvEmpty.setText("暂无待审批合同");
                                    }
                                }
                            } else {
                                Toast.makeText(StampApprovalListActivity.this,
                                        apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(StampApprovalListActivity.this,
                                "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onItemClick(StampApproval approval) {
        if (!hasStampApprovalPermission()) {
            Toast.makeText(this, "权限不足，仅限经理和管理员进行盖章审批", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, StampApprovalDetailActivity.class);
        intent.putExtra("approval_id", approval.getId());
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasStampApprovalPermission()) {
            loadPendingApprovals();
        }
    }
}
