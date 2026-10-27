package com.example.android_client;

import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.example.android_client.util.TokenManager;
import com.example.android_client.view.PdfPageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 原生PDF查看器Activity
 * 使用Android内置的PdfRenderer API，不依赖任何外部应用
 */
public class NativePdfViewerActivity extends AppCompatActivity {

    private static final String TAG = "NativePdfViewer";

    public static final String EXTRA_PDF_URL = "pdf_url";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_STAMP_APPROVAL_ID = "stamp_approval_id";
    public static final String EXTRA_RECEIPT_ID = "receipt_id";

    private PdfPageView pdfPageView;
    private ProgressBar progressBar;
    private TextView pageIndicator;
    private Toolbar toolbar;

    private String pdfUrl;
    private String title;
    private int stampApprovalId = -1;
    private int receiptId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_native_viewer);

        // 获取Intent参数
        pdfUrl = getIntent().getStringExtra(EXTRA_PDF_URL);
        title = getIntent().getStringExtra(EXTRA_TITLE);
        stampApprovalId = getIntent().getIntExtra(EXTRA_STAMP_APPROVAL_ID, -1);
        receiptId = getIntent().getIntExtra(EXTRA_RECEIPT_ID, -1);

        // 初始化视图
        initViews();

        // 下载并显示PDF
        downloadAndDisplayPdf();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(title != null ? title : "PDF预览");
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        pdfPageView = findViewById(R.id.pdf_page_view);
        progressBar = findViewById(R.id.progress_bar);
        pageIndicator = findViewById(R.id.tv_page_indicator);

        // 设置页面变化监听器
        pdfPageView.setOnPageChangeListener((pageIndex, totalPages) -> {
            pageIndicator.setText(String.format("第 %d / %d 页", pageIndex + 1, totalPages));
        });
    }

    private void downloadAndDisplayPdf() {
        if (pdfUrl == null || pdfUrl.isEmpty()) {
            Toast.makeText(this, "无效的文件地址", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Downloading PDF from: " + pdfUrl);

        // 显示加载进度
        runOnUiThread(() -> {
            progressBar.setVisibility(View.VISIBLE);
            pageIndicator.setText("正在加载...");
        });

        // 在后台线程下载PDF
        new Thread(() -> {
            try {
                // 获取Token
                String token = TokenManager.getInstance(this).getToken();
                if (token == null || token.isEmpty()) {
                    throw new Exception("未登录或Token已过期");
                }

                // 构建完整URL（如果是相对路径）
                String fullUrl = pdfUrl;
                if (!pdfUrl.startsWith("http")) {
                    fullUrl = "http://10.0.2.2:9095" + pdfUrl;
                }

                // 添加Token
                String separator = fullUrl.contains("?") ? "&" : "?";
                fullUrl = fullUrl + separator + "token=" + token;

                Log.d(TAG, "Full URL: " + fullUrl);

                // 下载PDF
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .build();

                Request request = new Request.Builder()
                        .url(fullUrl)
                        .header("Authorization", "Bearer " + token)
                        .build();

                Response response = client.newCall(request).execute();

                if (!response.isSuccessful() || response.body() == null) {
                    throw new IOException("HTTP " + response.code());
                }

                // 验证Content-Type
                MediaType mediaType = response.body().contentType();
                if (mediaType == null || !mediaType.toString().contains("application/pdf")) {
                    String errorBody = response.body().string();
                    Log.e(TAG, "Invalid Content-Type: " + (mediaType != null ? mediaType.toString() : "null"));
                    throw new Exception("服务器返回了无效的响应");
                }

                // 保存到临时文件
                File cacheDir = getExternalCacheDir();
                if (cacheDir == null) cacheDir = getCacheDir();

                File tempPdfFile = new File(cacheDir, "preview_" + System.currentTimeMillis() + ".pdf");
                FileOutputStream fos = new FileOutputStream(tempPdfFile);

                InputStream inputStream = response.body().byteStream();
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                fos.close();
                inputStream.close();

                long fileSize = tempPdfFile.length();
                Log.d(TAG, "PDF downloaded: size=" + fileSize + " bytes");

                if (fileSize == 0) {
                    throw new Exception("服务器返回了空文件");
                }

                // 使用PdfRenderer显示PDF
                displayPdf(tempPdfFile);

            } catch (Exception e) {
                Log.e(TAG, "Failed to download PDF", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(NativePdfViewerActivity.this,
                            "加载失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void displayPdf(File pdfFile) {
        try {
            // 使用PdfRenderer打开PDF
            ParcelFileDescriptor fd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer pdfRenderer = new PdfRenderer(fd);

            // 设置到View
            pdfPageView.setPdfRenderer(pdfRenderer);

            // 更新UI
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                pageIndicator.setVisibility(View.VISIBLE);
                pageIndicator.setText(String.format("第 1 / %d 页", pdfRenderer.getPageCount()));
                Log.d(TAG, "PDF displayed successfully with " + pdfRenderer.getPageCount() + " pages");
            });

            // 清理临时文件（在Activity关闭时）
            pdfFile.deleteOnExit();

        } catch (Exception e) {
            Log.e(TAG, "Failed to display PDF", e);
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(NativePdfViewerActivity.this,
                        "显示PDF失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pdfPageView != null) {
            pdfPageView.close();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
