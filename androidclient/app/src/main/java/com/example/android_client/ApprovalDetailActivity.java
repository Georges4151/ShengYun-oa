package com.example.android_client;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android_client.model.ApiResponse;
import com.example.android_client.model.ApprovalRecord;
import com.example.android_client.model.ApprovalRequest;
import com.example.android_client.model.Receipt;
import com.example.android_client.network.RetrofitClient;
import com.example.android_client.util.TokenManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ApprovalDetailActivity extends AppCompatActivity {

    private static final String TAG = "ApprovalDetailActivity";

    private TextView tvReceiptNumber;
    private TextView tvProjectName;
    private TextView tvCategory;
    private TextView tvAmount;
    private TextView tvDescription;
    private TextView tvSubmitter;
    private TextView tvSubmitTime;
    private RecyclerView rvApprovalHistory;
    private EditText etComment;
    private Button btnApprove;
    private Button btnReject;
    private Button btnViewReceipt;
    private ProgressBar progressBar;
    private Toolbar toolbar;

    private TokenManager tokenManager;
    private ApprovalHistoryAdapter historyAdapter;
    private Integer receiptId;
    private SimpleDateFormat dateFormatter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_approval_detail);

        tokenManager = TokenManager.getInstance(this);
        dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        receiptId = getIntent().getIntExtra("receiptId", -1);
        if (receiptId == -1) {
            Toast.makeText(this, "票据ID无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadReceiptDetail();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        tvReceiptNumber = findViewById(R.id.tv_receipt_number);
        tvProjectName = findViewById(R.id.tv_project_name);
        tvCategory = findViewById(R.id.tv_category);
        tvAmount = findViewById(R.id.tv_amount);
        tvDescription = findViewById(R.id.tv_description);
        tvSubmitter = findViewById(R.id.tv_submitter);
        tvSubmitTime = findViewById(R.id.tv_submit_time);
        rvApprovalHistory = findViewById(R.id.rv_approval_history);
        etComment = findViewById(R.id.et_comment);
        btnApprove = findViewById(R.id.btn_approve);
        btnReject = findViewById(R.id.btn_reject);
        btnViewReceipt = findViewById(R.id.btn_view_receipt);
        progressBar = findViewById(R.id.progress_bar);

        rvApprovalHistory.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new ApprovalHistoryAdapter(this, new ArrayList<>());
        rvApprovalHistory.setAdapter(historyAdapter);

        btnApprove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitApproval("approved");
            }
        });

        btnReject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitApproval("rejected");
            }
        });

        btnViewReceipt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewReceipt();
            }
        });
    }

    private void loadReceiptDetail() {
        progressBar.setVisibility(View.VISIBLE);

        RetrofitClient.getInstance().getApiService()
                .getReceiptDetail(receiptId)
                .enqueue(new Callback<ApiResponse<Receipt>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Receipt>> call,
                            Response<ApiResponse<Receipt>> response) {
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<Receipt> apiResponse = response.body();
                            if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                displayReceiptDetail(apiResponse.getData());
                            } else {
                                String msg = apiResponse.getMessage() != null ? apiResponse.getMessage() : "加载失败";
                                Toast.makeText(ApprovalDetailActivity.this, msg, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(ApprovalDetailActivity.this,
                                    "加载失败: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Receipt>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Load receipt detail failed: " + t.getMessage());
                        Toast.makeText(ApprovalDetailActivity.this,
                                "网络连接失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayReceiptDetail(Receipt receipt) {
        tvReceiptNumber.setText(
                "票据编号：" + (receipt.getReceiptNumber() != null ? receipt.getReceiptNumber() : "RC-" + receipt.getId()));

        String projectName = receipt.getProjectName();
        if (projectName == null || projectName.isEmpty()) {
            projectName = "未指定项目";
        }
        tvProjectName.setText("项目：" + projectName);

        String type = receipt.getType();
        if (type == null || type.isEmpty()) {
            type = "其他";
        }
        tvCategory.setText("类别：" + type);

        if (receipt.getAmount() != null) {
            tvAmount.setText("金额：¥" + receipt.getAmount().toString());
        } else {
            tvAmount.setText("金额：未填写");
        }

        String description = receipt.getDescription();
        if (description == null || description.isEmpty()) {
            description = "无描述";
        }
        tvDescription.setText("描述：" + description);

        String reporterName = receipt.getReporterName();
        if (reporterName == null || reporterName.isEmpty()) {
            reporterName = "未知";
        }
        tvSubmitter.setText("提交人：" + reporterName);

        Date createTime = receipt.getCreateTime();
        if (createTime != null) {
            tvSubmitTime.setText("提交时间：" + dateFormatter.format(createTime));
        } else {
            tvSubmitTime.setText("提交时间：未知");
        }

        loadApprovalHistory(receipt.getId());
    }

    private void loadApprovalHistory(Integer receiptId) {
        RetrofitClient.getInstance().getApiService()
                .getApprovalHistory(receiptId)
                .enqueue(new Callback<ApiResponse<List<ApprovalRecord>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<ApprovalRecord>>> call,
                            Response<ApiResponse<List<ApprovalRecord>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<List<ApprovalRecord>> apiResponse = response.body();
                            if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                historyAdapter.setRecords(apiResponse.getData());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<ApprovalRecord>>> call, Throwable t) {
                        Log.e(TAG, "Load approval history failed: " + t.getMessage());
                    }
                });
    }

    private void submitApproval(String status) {
        String comment = etComment.getText().toString().trim();

        progressBar.setVisibility(View.VISIBLE);
        btnApprove.setEnabled(false);
        btnReject.setEnabled(false);

        ApprovalRequest request = new ApprovalRequest(receiptId, status, comment);

        RetrofitClient.getInstance().getApiService()
                .submitApproval(request)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call,
                            Response<ApiResponse<Void>> response) {
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<Void> apiResponse = response.body();
                            if (apiResponse.isSuccess()) {
                                String message = "approved".equals(status) ? "审批通过" : "已拒绝";
                                Toast.makeText(ApprovalDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            } else {
                                String msg = apiResponse.getMessage() != null ? apiResponse.getMessage() : "操作失败";
                                Toast.makeText(ApprovalDetailActivity.this, msg, Toast.LENGTH_SHORT).show();
                                btnApprove.setEnabled(true);
                                btnReject.setEnabled(true);
                            }
                        } else {
                            Toast.makeText(ApprovalDetailActivity.this,
                                    "操作失败: " + response.code(), Toast.LENGTH_SHORT).show();
                            btnApprove.setEnabled(true);
                            btnReject.setEnabled(true);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Submit approval failed: " + t.getMessage());
                        Toast.makeText(ApprovalDetailActivity.this,
                                "网络连接失败", Toast.LENGTH_SHORT).show();
                        btnApprove.setEnabled(true);
                        btnReject.setEnabled(true);
                    }
                });
    }

    private void viewReceipt() {
        try {
            String token = tokenManager.getToken();
            if (token == null) {
                Toast.makeText(this, "用户未登录", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 使用应用内PDF预览
            String fileUrl = RetrofitClient.getInstance().getApiService().getReceiptFile(receiptId).request().url().toString();
            Log.d(TAG, "Opening receipt file URL: " + fileUrl);

            Intent intent = new Intent(this, PdfViewerActivity.class);
            intent.putExtra(PdfViewerActivity.EXTRA_FILE_URL, fileUrl);
            intent.putExtra(PdfViewerActivity.EXTRA_TITLE, "票据预览");
            intent.putExtra(PdfViewerActivity.EXTRA_RECEIPT_ID, receiptId);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening receipt file: " + e.getMessage(), e);
            Toast.makeText(this, "打开文件失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
