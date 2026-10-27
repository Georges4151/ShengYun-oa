package com.example.android_client;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.android_client.model.ApiResponse;
import com.example.android_client.model.UserNotification;
import com.example.android_client.network.RetrofitClient;
import com.example.android_client.util.TokenManager;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationActivity extends AppCompatActivity implements NotificationAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private TextView tvMarkAllRead;
    private TextView tvDelete;
    private LinearLayout bottomBar;
    private CheckBox cbSelectAll;
    private TextView tvSelectedCount;
    private Button btnDeleteSelected;
    private NotificationAdapter adapter;
    private TokenManager tokenManager;
    private List<UserNotification> notificationList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        tokenManager = TokenManager.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("通知中心");
            }
        }

        recyclerView = findViewById(R.id.recycler_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        progressBar = findViewById(R.id.progress_bar);
        tvEmpty = findViewById(R.id.tv_empty);
        tvMarkAllRead = findViewById(R.id.tv_mark_all_read);
        tvDelete = findViewById(R.id.tv_delete);
        bottomBar = findViewById(R.id.bottom_bar);
        cbSelectAll = findViewById(R.id.cb_select_all);
        tvSelectedCount = findViewById(R.id.tv_selected_count);
        btnDeleteSelected = findViewById(R.id.btn_delete_selected);

        adapter = new NotificationAdapter(notificationList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(this::loadNotifications);
        tvMarkAllRead.setOnClickListener(v -> markAllAsRead());
        tvDelete.setOnClickListener(v -> toggleSelectionMode());
        cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                adapter.selectAll();
            } else {
                adapter.clearSelection();
            }
        });
        btnDeleteSelected.setOnClickListener(v -> deleteSelectedNotifications());

        loadNotifications();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (adapter.isSelectionMode()) {
                exitSelectionMode();
                return true;
            }
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (adapter.isSelectionMode()) {
            exitSelectionMode();
        } else {
            super.onBackPressed();
        }
    }

    private void loadNotifications() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        RetrofitClient.getInstance().getApiService()
                .getNotifications()
                .enqueue(new Callback<ApiResponse<List<UserNotification>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<UserNotification>>> call,
                            Response<ApiResponse<List<UserNotification>>> response) {
                        progressBar.setVisibility(View.GONE);
                        swipeRefreshLayout.setRefreshing(false);

                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<List<UserNotification>> apiResponse = response.body();
                            if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                notificationList.clear();
                                notificationList.addAll(apiResponse.getData());
                                adapter.notifyDataSetChanged();

                                if (notificationList.isEmpty()) {
                                    tvEmpty.setVisibility(View.VISIBLE);
                                    tvDelete.setVisibility(View.GONE);
                                } else {
                                    tvDelete.setVisibility(View.VISIBLE);
                                }
                            } else {
                                Toast.makeText(NotificationActivity.this,
                                        apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(NotificationActivity.this,
                                    "加载失败", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<UserNotification>>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(NotificationActivity.this,
                                "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onItemClick(UserNotification notification, int position) {
        Intent intent = new Intent(this, NotificationDetailActivity.class);
        intent.putExtra(NotificationDetailActivity.EXTRA_NOTIFICATION_ID, notification.getId());
        startActivity(intent);
    }

    @Override
    public void onItemLongClick(UserNotification notification, int position) {
        enterSelectionMode();
        adapter.toggleSelection(position);
    }

    @Override
    public void onSelectionChanged(int selectedCount) {
        tvSelectedCount.setText("已选择 " + selectedCount + " 项");
        cbSelectAll.setOnCheckedChangeListener(null);
        cbSelectAll.setChecked(selectedCount == notificationList.size());
        cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                adapter.selectAll();
            } else {
                adapter.clearSelection();
            }
        });
    }

    private void toggleSelectionMode() {
        if (adapter.isSelectionMode()) {
            exitSelectionMode();
        } else {
            enterSelectionMode();
        }
    }

    private void enterSelectionMode() {
        adapter.setSelectionMode(true);
        bottomBar.setVisibility(View.VISIBLE);
        tvDelete.setText("取消");
        tvMarkAllRead.setVisibility(View.GONE);
        tvSelectedCount.setText("已选择 0 项");
    }

    private void exitSelectionMode() {
        adapter.setSelectionMode(false);
        bottomBar.setVisibility(View.GONE);
        tvDelete.setText("删除");
        tvMarkAllRead.setVisibility(View.VISIBLE);
        cbSelectAll.setChecked(false);
    }

    private void markAllAsRead() {
        RetrofitClient.getInstance().getApiService()
                .markAsRead(new HashMap<>())
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call,
                            Response<ApiResponse<Void>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) {
                            for (UserNotification notification : notificationList) {
                                notification.setIsRead(true);
                            }
                            adapter.notifyDataSetChanged();
                            Toast.makeText(NotificationActivity.this,
                                    "已全部标记为已读", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        Toast.makeText(NotificationActivity.this,
                                "操作失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteSelectedNotifications() {
        Set<Integer> selectedPositions = adapter.getSelectedPositions();
        if (selectedPositions.isEmpty()) {
            Toast.makeText(this, "请选择要删除的通知", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Integer> notificationIds = new ArrayList<>();
        for (int position : selectedPositions) {
            if (position < notificationList.size()) {
                notificationIds.add(notificationList.get(position).getNotificationId());
            }
        }

        Map<String, List<Integer>> body = new HashMap<>();
        body.put("notificationIds", notificationIds);

        RetrofitClient.getInstance().getApiService()
                .batchDeleteNotifications(body)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call,
                            Response<ApiResponse<Void>> response) {
                        android.util.Log.d("NotificationActivity", "response: " + response);
                        android.util.Log.d("NotificationActivity", "response.body(): " + response.body());

                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<Void> apiResponse = response.body();
                            android.util.Log.d("NotificationActivity", "success: " + apiResponse.isSuccess());
                            android.util.Log.d("NotificationActivity", "message: " + apiResponse.getMessage());

                            if (apiResponse.isSuccess()) {
                                List<UserNotification> toRemove = new ArrayList<>();
                                for (int position : selectedPositions) {
                                    if (position < notificationList.size()) {
                                        toRemove.add(notificationList.get(position));
                                    }
                                }
                                notificationList.removeAll(toRemove);
                                exitSelectionMode();
                                adapter.notifyDataSetChanged();

                                if (notificationList.isEmpty()) {
                                    tvEmpty.setVisibility(View.VISIBLE);
                                    tvDelete.setVisibility(View.GONE);
                                }
                                Toast.makeText(NotificationActivity.this,
                                        "已删除 " + toRemove.size() + " 条通知", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(NotificationActivity.this,
                                        "删除失败: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(NotificationActivity.this,
                                    "删除失败: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        android.util.Log.e("NotificationActivity", "onFailure: ", t);
                        Toast.makeText(NotificationActivity.this,
                                "删除失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
