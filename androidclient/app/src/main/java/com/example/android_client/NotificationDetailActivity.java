package com.example.android_client;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.android_client.model.ApiResponse;
import com.example.android_client.model.Notification;
import com.example.android_client.model.UserNotification;
import com.example.android_client.network.RetrofitClient;
import com.example.android_client.util.TokenManager;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationDetailActivity extends AppCompatActivity {

    public static final String EXTRA_NOTIFICATION_ID = "notification_id";
    public static final String EXTRA_NOTIFICATION = "notification";

    private View viewPriority;
    private TextView tvTitle;
    private TextView tvPriority;
    private TextView tvSender;
    private TextView tvTime;
    private TextView tvType;
    private TextView tvContent;
    private Button btnAction;
    private ProgressBar progressBar;

    private TokenManager tokenManager;
    private UserNotification userNotification;
    private Integer notificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_detail);

        tokenManager = TokenManager.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("通知详情");
        }

        initViews();
        loadNotificationData();
    }

    private void initViews() {
        viewPriority = findViewById(R.id.view_priority);
        tvTitle = findViewById(R.id.tv_title);
        tvPriority = findViewById(R.id.tv_priority);
        tvSender = findViewById(R.id.tv_sender);
        tvTime = findViewById(R.id.tv_time);
        tvType = findViewById(R.id.tv_type);
        tvContent = findViewById(R.id.tv_content);
        btnAction = findViewById(R.id.btn_action);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void loadNotificationData() {
        notificationId = getIntent().getIntExtra(EXTRA_NOTIFICATION_ID, -1);
        String notificationJson = getIntent().getStringExtra(EXTRA_NOTIFICATION);

        if (notificationId != null && notificationId > 0) {
            loadFromServer(notificationId);
        } else if (notificationJson != null) {
            Gson gson = new Gson();
            userNotification = gson.fromJson(notificationJson, UserNotification.class);
            displayNotification();
        } else {
            finish();
        }
    }

    private void loadFromServer(Integer id) {
        progressBar.setVisibility(View.VISIBLE);

        RetrofitClient.getInstance().getApiService()
                .getNotificationDetail(id)
                .enqueue(new Callback<ApiResponse<UserNotification>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<UserNotification>> call,
                            Response<ApiResponse<UserNotification>> response) {
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<UserNotification> apiResponse = response.body();
                            if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                userNotification = apiResponse.getData();
                                displayNotification();
                            } else {
                                Toast.makeText(NotificationDetailActivity.this,
                                        apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        } else {
                            Toast.makeText(NotificationDetailActivity.this,
                                    "加载失败", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<UserNotification>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(NotificationDetailActivity.this,
                                "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void displayNotification() {
        if (userNotification == null)
            return;

        Notification notification = userNotification.getNotification();
        if (notification == null)
            return;

        tvTitle.setText(notification.getTitle());
        tvContent.setText(notification.getContent());
        tvSender.setText(notification.getSenderName() != null ? notification.getSenderName() : "系统");
        tvTime.setText(formatTime(notification.getPublishTime()));
        tvType.setText(getTypeDisplayName(notification.getNotificationType()));

        String priority = notification.getPriority();
        int priorityColor = getPriorityColor(priority);
        viewPriority.setBackgroundColor(priorityColor);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(8f);
        drawable.setColor(priorityColor);
        tvPriority.setBackground(drawable);
        tvPriority.setText(getPriorityDisplayName(priority));

        String linkUrl = notification.getLinkUrl();
        if (linkUrl != null && !linkUrl.isEmpty()) {
            btnAction.setVisibility(View.VISIBLE);
            btnAction.setOnClickListener(v -> handleAction(linkUrl));
        } else {
            btnAction.setVisibility(View.GONE);
        }
    }

    private String formatTime(String time) {
        if (time == null)
            return "-";
        if (time.length() >= 16) {
            return time.substring(0, 16).replace("T", " ");
        }
        return time;
    }

    private int getPriorityColor(String priority) {
        if ("HIGH".equals(priority))
            return Color.parseColor("#F44336");
        if ("MEDIUM".equals(priority))
            return Color.parseColor("#FF9800");
        return Color.parseColor("#4CAF50");
    }

    private String getPriorityDisplayName(String priority) {
        if ("HIGH".equals(priority))
            return "高优先级";
        if ("MEDIUM".equals(priority))
            return "中优先级";
        return "普通";
    }

    private String getTypeDisplayName(String type) {
        if (type == null)
            return "系统通知";
        switch (type) {
            case "SYSTEM":
                return "系统通知";
            case "BUSINESS":
                return "业务通知";
            case "MARKETING":
                return "公告通知";
            default:
                return type;
        }
    }

    private void handleAction(String linkUrl) {
        if (linkUrl.contains("/receipts/") || linkUrl.contains("receipt")) {
            try {
                String[] parts = linkUrl.split("/");
                Integer receiptId = null;
                for (int i = 0; i < parts.length; i++) {
                    if ("receipts".equals(parts[i]) && i + 1 < parts.length) {
                        receiptId = Integer.parseInt(parts[i + 1]);
                        break;
                    }
                }
                if (receiptId != null) {
                    Intent intent = new Intent(this, ApprovalDetailActivity.class);
                    intent.putExtra("receipt_id", receiptId);
                    startActivity(intent);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (linkUrl.contains("/stamp-approvals/") || linkUrl.contains("stamp")) {
            try {
                String[] parts = linkUrl.split("/");
                Integer approvalId = null;
                for (int i = 0; i < parts.length; i++) {
                    if ("stamp-approvals".equals(parts[i]) && i + 1 < parts.length) {
                        approvalId = Integer.parseInt(parts[i + 1]);
                        break;
                    }
                }
                if (approvalId != null) {
                    Intent intent = new Intent(this, StampApprovalDetailActivity.class);
                    intent.putExtra("approval_id", approvalId);
                    startActivity(intent);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
