package com.example.android_client;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.android_client.model.ApiResponse;
import com.example.android_client.model.StampApproval;
import com.example.android_client.network.RetrofitClient;
import com.example.android_client.util.TokenManager;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StampApprovalDetailActivity extends AppCompatActivity {

    private static final String TAG = "StampApprovalDetail";

    private TextView tvContractName;
    private TextView tvSealName;
    private TextView tvSealCode;
    private TextView tvSealPermission;
    private TextView tvProjectName;
    private TextView tvApplicantName;
    private TextView tvApplyTime;
    private TextView tvStatus;
    private TextView tvRejectReason;
    private LinearLayout layoutRejectReason;
    private LinearLayout layoutButtons;
    private Button btnApprove;
    private Button btnReject;
    private Button btnViewContract;
    private ProgressBar progressBar;

    private TokenManager tokenManager;
    private Integer approvalId;
    private StampApproval currentApproval;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stamp_approval_detail);

        tokenManager = TokenManager.getInstance(this);
        approvalId = getIntent().getIntExtra("approval_id", -1);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("审批详情");
        }

        initViews();

        if (!hasStampApprovalPermission()) {
            Toast.makeText(this, "权限不足，仅限经理和管理员进行盖章审批", Toast.LENGTH_LONG).show();
            layoutButtons.setVisibility(View.GONE);
        }

        loadApprovalDetail();
    }

    private boolean hasStampApprovalPermission() {
        String role = tokenManager.getRole();
        if (role == null)
            return false;
        String upperRole = role.toUpperCase();
        return "MANAGER".equals(upperRole) || "ADMIN".equals(upperRole);
    }

    private void initViews() {
        tvContractName = findViewById(R.id.tv_contract_name);
        tvSealName = findViewById(R.id.tv_seal_name);
        tvSealCode = findViewById(R.id.tv_seal_code);
        tvSealPermission = findViewById(R.id.tv_seal_permission);
        tvProjectName = findViewById(R.id.tv_project_name);
        tvApplicantName = findViewById(R.id.tv_applicant_name);
        tvApplyTime = findViewById(R.id.tv_apply_time);
        tvStatus = findViewById(R.id.tv_status);
        tvRejectReason = findViewById(R.id.tv_reject_reason);
        layoutRejectReason = findViewById(R.id.layout_reject_reason);
        layoutButtons = findViewById(R.id.layout_buttons);
        btnApprove = findViewById(R.id.btn_approve);
        btnReject = findViewById(R.id.btn_reject);
        btnViewContract = findViewById(R.id.btn_view_contract);
        progressBar = findViewById(R.id.progress_bar);

        btnApprove.setOnClickListener(v -> {
            if (!hasStampApprovalPermission()) {
                Toast.makeText(this, "权限不足，仅限经理和管理员进行盖章审批", Toast.LENGTH_SHORT).show();
                return;
            }
            showApproveConfirmDialog();
        });

        btnReject.setOnClickListener(v -> {
            if (!hasStampApprovalPermission()) {
                Toast.makeText(this, "权限不足，仅限经理和管理员进行盖章审批", Toast.LENGTH_SHORT).show();
                return;
            }
            showRejectDialog();
        });

        btnViewContract.setOnClickListener(v -> viewContract());
    }

    private void loadApprovalDetail() {
        progressBar.setVisibility(View.VISIBLE);

        RetrofitClient.getInstance().getApiService()
                .getStampApprovalDetail(approvalId)
                .enqueue(new Callback<ApiResponse<StampApproval>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<StampApproval>> call,
                            Response<ApiResponse<StampApproval>> response) {
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<StampApproval> apiResponse = response.body();
                            if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                currentApproval = apiResponse.getData();
                                displayApprovalDetail(currentApproval);
                            } else {
                                Toast.makeText(StampApprovalDetailActivity.this,
                                        apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<StampApproval>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(StampApprovalDetailActivity.this,
                                "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayApprovalDetail(StampApproval approval) {
        Log.d(TAG, "Displaying approval detail: " + new Gson().toJson(approval));

        tvContractName.setText(approval.getContractName() != null ? approval.getContractName() : "-");
        Log.d(TAG, "Contract name: " + approval.getContractName());

        tvSealName.setText(approval.getSealName() != null ? approval.getSealName() : "-");
        Log.d(TAG, "Seal name: " + approval.getSealName());

        tvSealCode.setText(approval.getSealCode() != null ? approval.getSealCode() : "-");
        Log.d(TAG, "Seal code: " + approval.getSealCode());

        tvSealPermission.setText(approval.getSealPermission() != null ? approval.getSealPermission() : "-");
        Log.d(TAG, "Seal permission: " + approval.getSealPermission());

        tvProjectName.setText(approval.getProjectName() != null ? approval.getProjectName() : "-");
        Log.d(TAG, "Project name: " + approval.getProjectName());

        tvApplicantName.setText(approval.getApplicantName() != null ? approval.getApplicantName() : "-");
        Log.d(TAG, "Applicant name: " + approval.getApplicantName());

        if (approval.getApplyTime() != null) {
            tvApplyTime.setText(dateFormat.format(approval.getApplyTime()));
            Log.d(TAG, "Apply time: " + dateFormat.format(approval.getApplyTime()));
        } else {
            tvApplyTime.setText("-");
            Log.d(TAG, "Apply time: null");
        }

        String statusText = approval.getStatusText();
        if (statusText == null) {
            statusText = "待审批";
        }
        tvStatus.setText(statusText);
        Log.d(TAG, "Status text: " + statusText);

        String status = approval.getStatus();
        Log.d(TAG, "Status: " + status);
        boolean isPending = status == null || "pending".equals(status);

        if (isPending) {
            tvStatus.setBackgroundResource(R.drawable.status_pending);
            if (hasStampApprovalPermission()) {
                layoutButtons.setVisibility(View.VISIBLE);
            } else {
                layoutButtons.setVisibility(View.GONE);
            }
        } else if ("approved".equals(status)) {
            tvStatus.setBackgroundResource(R.drawable.status_approved);
            layoutButtons.setVisibility(View.GONE);
        } else if ("rejected".equals(status)) {
            tvStatus.setBackgroundResource(R.drawable.status_rejected);
            layoutButtons.setVisibility(View.GONE);
            if (approval.getRejectReason() != null && !approval.getRejectReason().isEmpty()) {
                layoutRejectReason.setVisibility(View.VISIBLE);
                tvRejectReason.setText(approval.getRejectReason());
            }
        }

        // 总是显示预览按钮，因为可以通过API获取文件
        btnViewContract.setVisibility(View.VISIBLE);
    }

    private void viewContract() {
        if (currentApproval == null) {
            Toast.makeText(this, "合同文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String token = tokenManager.getToken();
            if (token == null) {
                Toast.makeText(this, "用户未登录", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 使用应用内PDF预览
            String fileUrl = "http://10.0.2.2:9095/api/mobile/stamp-approvals/" + approvalId + "/file?token=" + token;
            Log.d(TAG, "Opening contract file URL: " + fileUrl);

            Intent intent = new Intent(this, PdfViewerActivity.class);
            intent.putExtra(PdfViewerActivity.EXTRA_FILE_URL, fileUrl);
            intent.putExtra(PdfViewerActivity.EXTRA_TITLE, "合同预览");
            // 印章审批使用approvalId和status来判断
            intent.putExtra(PdfViewerActivity.EXTRA_STAMP_APPROVAL_ID, approvalId);
            intent.putExtra(PdfViewerActivity.EXTRA_APPROVAL_STATUS, currentApproval.getStatus());
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening contract file: " + e.getMessage(), e);
            Toast.makeText(this, "打开文件失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showApproveConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("确认审批")
                .setMessage("确定要通过该合同盖章申请吗？")
                .setPositiveButton("确定", (dialog, which) -> approveStamp())
                .setNegativeButton("取消", null)
                .show();
    }

    private void showRejectDialog() {
        EditText editText = new EditText(this);
        editText.setHint("请输入拒绝理由");
        editText.setMinLines(3);

        new AlertDialog.Builder(this)
                .setTitle("拒绝申请")
                .setView(editText)
                .setPositiveButton("确定", (dialog, which) -> {
                    String reason = editText.getText().toString().trim();
                    if (reason.isEmpty()) {
                        Toast.makeText(this, "请输入拒绝理由", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    rejectStamp(reason);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void approveStamp() {
        if (!hasStampApprovalPermission()) {
            Toast.makeText(this, "权限不足", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnApprove.setEnabled(false);
        btnReject.setEnabled(false);

        RetrofitClient.getInstance().getApiService()
                .approveStamp(approvalId)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call,
                            Response<ApiResponse<Void>> response) {
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<Void> apiResponse = response.body();
                            if (apiResponse.isSuccess()) {
                                Toast.makeText(StampApprovalDetailActivity.this,
                                        "审批通过", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            } else {
                                Toast.makeText(StampApprovalDetailActivity.this,
                                        apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                btnApprove.setEnabled(true);
                                btnReject.setEnabled(true);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        btnApprove.setEnabled(true);
                        btnReject.setEnabled(true);
                        Toast.makeText(StampApprovalDetailActivity.this,
                                "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void rejectStamp(String reason) {
        if (!hasStampApprovalPermission()) {
            Toast.makeText(this, "权限不足", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnApprove.setEnabled(false);
        btnReject.setEnabled(false);

        Map<String, String> body = new HashMap<>();
        body.put("reason", reason);

        RetrofitClient.getInstance().getApiService()
                .rejectStamp(approvalId, body)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call,
                            Response<ApiResponse<Void>> response) {
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<Void> apiResponse = response.body();
                            if (apiResponse.isSuccess()) {
                                Toast.makeText(StampApprovalDetailActivity.this,
                                        "已拒绝申请", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            } else {
                                Toast.makeText(StampApprovalDetailActivity.this,
                                        apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                btnApprove.setEnabled(true);
                                btnReject.setEnabled(true);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        btnApprove.setEnabled(true);
                        btnReject.setEnabled(true);
                        Toast.makeText(StampApprovalDetailActivity.this,
                                "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
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
