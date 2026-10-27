package com.example.android_client;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android_client.model.ApiResponse;
import com.example.android_client.model.Receipt;
import com.example.android_client.network.RetrofitClient;
import com.example.android_client.util.TokenManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ApprovalTodoActivity extends AppCompatActivity {

    private static final String TAG = "ApprovalTodoActivity";

    private RecyclerView recyclerView;
    private ApprovalTodoAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private Toolbar toolbar;

    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_approval_todo);

        tokenManager = TokenManager.getInstance(this);

        if (!tokenManager.isLoggedIn()) {
            startLoginActivity();
            finish();
            return;
        }

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        recyclerView = findViewById(R.id.recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        tvEmpty = findViewById(R.id.tv_empty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ApprovalTodoAdapter(this, null, new ApprovalTodoAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Receipt receipt) {
                startApprovalDetailActivity(receipt.getId());
            }
        });
        recyclerView.setAdapter(adapter);

        checkPermissionAndLoad();
    }

    private void checkPermissionAndLoad() {
        String role = tokenManager.getRole();
        if (!hasApprovalPermission(role)) {
            tvEmpty.setText("您没有审批权限");
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            return;
        }

        loadPendingApprovals();
    }

    private boolean hasApprovalPermission(String role) {
        if (role == null)
            return false;
        String upperRole = role.toUpperCase();
        return "MANAGER".equals(upperRole) ||
                "PROJECT_MANAGER".equals(upperRole) ||
                "ADMIN".equals(upperRole) ||
                "FINANCE".equals(upperRole);
    }

    private void loadPendingApprovals() {
        progressBar.setVisibility(View.VISIBLE);

        RetrofitClient.getInstance().getApiService()
                .getPendingApprovals()
                .enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                            Response<ApiResponse<Map<String, Object>>> response) {
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<Map<String, Object>> apiResponse = response.body();
                            if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                Map<String, Object> data = apiResponse.getData();
                                Object listObj = data.get("list");

                                if (listObj != null) {
                                    Gson gson = new Gson();
                                    Type listType = new TypeToken<List<Receipt>>() {
                                    }.getType();
                                    String json = gson.toJson(listObj);
                                    List<Receipt> receipts = gson.fromJson(json, listType);

                                    if (receipts.isEmpty()) {
                                        tvEmpty.setText("暂无待审批项目");
                                        tvEmpty.setVisibility(View.VISIBLE);
                                        recyclerView.setVisibility(View.GONE);
                                    } else {
                                        tvEmpty.setVisibility(View.GONE);
                                        recyclerView.setVisibility(View.VISIBLE);
                                        adapter.setReceipts(receipts);
                                    }
                                } else {
                                    tvEmpty.setText("暂无待审批项目");
                                    tvEmpty.setVisibility(View.VISIBLE);
                                    recyclerView.setVisibility(View.GONE);
                                }
                            } else {
                                String msg = apiResponse.getMessage() != null ? apiResponse.getMessage() : "加载失败";
                                if (isTokenExpired(msg)) {
                                    handleTokenExpired();
                                } else {
                                    Toast.makeText(ApprovalTodoActivity.this, msg, Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else if (response.code() == 401 || response.code() == 403) {
                            handleTokenExpired();
                        } else {
                            Toast.makeText(ApprovalTodoActivity.this, "加载失败，请重试", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Load pending approvals failed: " + t.getMessage());
                        String msg = t.getMessage();
                        if (msg != null && isTokenExpired(msg)) {
                            handleTokenExpired();
                        } else {
                            Toast.makeText(ApprovalTodoActivity.this, "网络连接失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private boolean isTokenExpired(String message) {
        if (message == null)
            return false;
        String lowerMsg = message.toLowerCase();
        return lowerMsg.contains("jwt expired") ||
                lowerMsg.contains("token expired") ||
                lowerMsg.contains("expired") ||
                lowerMsg.contains("token") && lowerMsg.contains("invalid");
    }

    private void handleTokenExpired() {
        Toast.makeText(this, "登录已过期，请重新登录", Toast.LENGTH_LONG).show();
        tokenManager.clearToken();
        startLoginActivity();
        finish();
    }

    private void startLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void startApprovalDetailActivity(Integer receiptId) {
        Intent intent = new Intent(this, ApprovalDetailActivity.class);
        intent.putExtra("receiptId", receiptId);
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tokenManager.isLoggedIn() && hasApprovalPermission(tokenManager.getRole())) {
            loadPendingApprovals();
        }
    }
}
