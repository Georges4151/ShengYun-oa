package com.example.android_client;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.android_client.model.ApiResponse;
import com.example.android_client.network.RetrofitClient;
import com.example.android_client.util.TokenManager;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private CardView cvStampApproval;
    private CardView cvPendingApprovals;
    private CardView cvUploadReceipt;
    private TextView tvWelcome;
    private TextView tvStampCount;
    private TextView tvPendingCount;
    private View notificationContainer;
    private TextView tvNotificationBadge;
    private View logoutContainer;

    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化RetrofitClient
        RetrofitClient.initialize(getApplicationContext());

        setContentView(R.layout.activity_main);

        tokenManager = TokenManager.getInstance(this);

        if (!tokenManager.isLoggedIn()) {
            startLoginActivity();
            finish();
            return;
        }

        initViews();
        setupClickListeners();
        loadDashboardStats();
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tv_welcome);
        cvStampApproval = findViewById(R.id.cv_stamp_approval);
        cvPendingApprovals = findViewById(R.id.cv_pending_approvals);
        cvUploadReceipt = findViewById(R.id.cv_upload_receipt);
        tvStampCount = findViewById(R.id.tv_stamp_count);
        tvPendingCount = findViewById(R.id.tv_pending_count);
        notificationContainer = findViewById(R.id.notification_container);
        tvNotificationBadge = findViewById(R.id.tv_notification_badge);
        logoutContainer = findViewById(R.id.logout_container);

        String fullName = tokenManager.getFullName();
        tvWelcome.setText("欢迎，" + (fullName != null ? fullName : tokenManager.getUsername()));
    }

    private void setupClickListeners() {
        cvStampApproval.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startStampApprovalActivity();
            }
        });

        cvPendingApprovals.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startApprovalTodoActivity();
            }
        });

        cvUploadReceipt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startUploadReceiptActivity();
            }
        });

        notificationContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startNotificationActivity();
            }
        });

        logoutContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutDialog();
            }
        });
    }

    private void loadDashboardStats() {
        Log.d(TAG, "Loading dashboard stats...");

        RetrofitClient.getInstance().getApiService()
                .getDashboardStats()
                .enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                            Response<ApiResponse<Map<String, Object>>> response) {
                        Log.d(TAG, "Stats response: success=" + response.isSuccessful() + ", code=" + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<Map<String, Object>> apiResponse = response.body();
                            Log.d(TAG, "API response: success=" + apiResponse.isSuccess() + ", data="
                                    + apiResponse.getData());

                            if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                updateStats(apiResponse.getData());
                            } else {
                                Log.e(TAG, "API returned error: " + apiResponse.getMessage());
                            }
                        } else {
                            Log.e(TAG, "Response failed: " + response.message());
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                        Log.e(TAG, "Load dashboard stats failed: " + t.getMessage());
                    }
                });
    }

    private void updateStats(Map<String, Object> stats) {
        if (stats == null) {
            Log.e(TAG, "Stats data is null");
            return;
        }

        Log.d(TAG, "Raw stats data: " + stats.toString());

        Object stampPending = stats.get("stampPendingCount");
        Log.d(TAG, "stampPendingCount type: " + (stampPending != null ? stampPending.getClass().getName() : "null"));
        int stampCount = 0;
        if (stampPending != null) {
            if (stampPending instanceof Number) {
                stampCount = ((Number) stampPending).intValue();
            } else {
                try {
                    stampCount = Integer.parseInt(String.valueOf(stampPending));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Failed to parse stampPendingCount: " + stampPending);
                }
            }
        }
        tvStampCount.setText(stampCount > 0 ? stampCount + " 待审批" : "合同盖章");

        Object receiptPending = stats.get("receiptPendingCount");
        Log.d(TAG,
                "receiptPendingCount type: " + (receiptPending != null ? receiptPending.getClass().getName() : "null"));
        int receiptCount = 0;
        if (receiptPending != null) {
            if (receiptPending instanceof Number) {
                receiptCount = ((Number) receiptPending).intValue();
            } else {
                try {
                    receiptCount = Integer.parseInt(String.valueOf(receiptPending));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Failed to parse receiptPendingCount: " + receiptPending);
                }
            }
        }
        tvPendingCount.setText(String.valueOf(receiptCount));

        Log.d(TAG, "Stats updated - stamp: " + stampCount + ", receipt: " + receiptCount);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardStats();
        loadUnreadCount();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            logout();
            return true;
        } else if (id == R.id.action_settings) {
            Toast.makeText(this, "设置功能开发中", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_about) {
            Toast.makeText(this, "票据审批系统 v1.0", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startLoginActivity() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
    }

    private void startStampApprovalActivity() {
        Intent intent = new Intent(MainActivity.this, StampApprovalListActivity.class);
        startActivity(intent);
    }

    private void startApprovalTodoActivity() {
        Intent intent = new Intent(MainActivity.this, ApprovalTodoActivity.class);
        startActivity(intent);
    }

    private void startUploadReceiptActivity() {
        Intent intent = new Intent(MainActivity.this, UploadReceiptActivity.class);
        startActivity(intent);
    }

    private void startNotificationActivity() {
        Intent intent = new Intent(MainActivity.this, NotificationActivity.class);
        startActivity(intent);
    }

    private void loadUnreadCount() {
        RetrofitClient.getInstance().getApiService()
                .getUnreadCount()
                .enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Map<String, Object>>> call,
                            Response<ApiResponse<Map<String, Object>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<Map<String, Object>> apiResponse = response.body();
                            if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                Object countObj = apiResponse.getData().get("count");
                                int count = 0;
                                if (countObj instanceof Number) {
                                    count = ((Number) countObj).intValue();
                                }
                                updateNotificationBadge(count);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                    }
                });
    }

    private void updateNotificationBadge(int count) {
        if (count > 0) {
            tvNotificationBadge.setVisibility(View.VISIBLE);
            tvNotificationBadge.setText(count > 99 ? "99+" : String.valueOf(count));
        } else {
            tvNotificationBadge.setVisibility(View.GONE);
        }
    }

    private void logout() {
        tokenManager.clear();
        startLoginActivity();
        finish();
    }

    private void showLogoutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("退出登录")
                .setMessage("确定要退出登录吗？")
                .setPositiveButton("确定", (dialog, which) -> logout())
                .setNegativeButton("取消", null)
                .show();
    }
}
