package com.example.android_client;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.example.android_client.network.RetrofitClient;
import com.example.android_client.util.TokenManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONObject;

/**
 * PDF文件预览Activity（方案二：服务端水印版）
 *
 * 核心设计：
 * 1. 审批未完成：调用服务端 /preview 接口 → 返回带水印的PDF → 客户端仅负责显示
 * 2. 审批已完成：调用服务端 /file 接口   → 返回原始PDF    → 正常预览+允许下载
 * 3. 所有水印逻辑在服务端完成，客户端零水印代码
 */
public class PdfViewerActivity extends AppCompatActivity {

    private static final String TAG = "PdfViewerActivity";

    // Intent参数常量
    public static final String EXTRA_FILE_URL = "file_url";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_RECEIPT_ID = "receipt_id";
    public static final String EXTRA_STAMP_APPROVAL_ID = "stamp_approval_id";
    public static final String EXTRA_APPROVAL_STATUS = "approval_status";

    private WebView webView;
    private ImageView imageView;
    private ProgressBar progressBar;
    private TextView pageIndicator;
    private Toolbar toolbar;
    private TokenManager tokenManager;

    // 参数
    private Integer receiptId;
    private Integer stampApprovalId;
    private String approvalStatus;
    private boolean isApprovalCompleted = false;

    // 临时PDF文件
    private File tempPdfFile;

    // PDF渲染器状态（用于页面导航）
    private PdfRenderer currentPdfRenderer;
    private int currentPageCount = 0;
    private int currentPageIndex = 0;
    private Bitmap currentBitmap; // 当前显示的Bitmap，用于回收

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        tokenManager = TokenManager.getInstance(this);

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        receiptId = getIntent().getIntExtra(EXTRA_RECEIPT_ID, -1);
        stampApprovalId = getIntent().getIntExtra(EXTRA_STAMP_APPROVAL_ID, -1);
        approvalStatus = getIntent().getStringExtra(EXTRA_APPROVAL_STATUS);

        initViews(title);

        // 检查审批状态决定使用哪个接口
        if (receiptId != -1) {
            checkReceiptApprovalStatus();
        } else if (stampApprovalId != -1 && approvalStatus != null) {
            checkStampApprovalStatus();
        } else {
            // 无审批信息，默认使用原始文件接口
            isApprovalCompleted = true;
            buildAndLoadPreviewUrl();
        }
    }

    /**
     * 检查票据的审批状态
     */
    private void checkReceiptApprovalStatus() {
        progressBar.setVisibility(View.VISIBLE);

        RetrofitClient.getInstance().getApiService()
                .getReceiptDetail(receiptId)
                .enqueue(new retrofit2.Callback<com.example.android_client.model.ApiResponse<com.example.android_client.model.Receipt>>() {
                    @Override
                    public void onResponse(
                            retrofit2.Call<com.example.android_client.model.ApiResponse<com.example.android_client.model.Receipt>> call,
                            retrofit2.Response<com.example.android_client.model.ApiResponse<com.example.android_client.model.Receipt>> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null &&
                                response.body().isSuccess() && response.body().getData() != null) {
                            com.example.android_client.model.Receipt receipt = response.body().getData();
                            Integer currentLevel = receipt.getCurrentApprovalLevel();
                            isApprovalCompleted = (currentLevel != null && currentLevel == 3);

                            if (!isApprovalCompleted) {
                                Toast.makeText(PdfViewerActivity.this,
                                        "审批未完成，文件带有安全水印", Toast.LENGTH_LONG).show();
                            }

                            buildAndLoadPreviewUrl();
                        } else {
                            Toast.makeText(PdfViewerActivity.this,
                                    "获取审批状态失败", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(
                            retrofit2.Call<com.example.android_client.model.ApiResponse<com.example.android_client.model.Receipt>> call,
                            Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Check approval failed: " + t.getMessage());
                        Toast.makeText(PdfViewerActivity.this,
                                "网络连接失败", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    /**
     * 检查印章审批状态
     */
    private void checkStampApprovalStatus() {
        isApprovalCompleted = "approved".equals(approvalStatus);

        if (!isApprovalCompleted) {
            Toast.makeText(this,
                    "审批未完成，文件带有安全水印", Toast.LENGTH_LONG).show();
        }

        buildAndLoadPreviewUrl();
    }

    private void initViews(String title) {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(
                    (title != null && !title.isEmpty()) ? title : "文件预览");
        }

        webView = findViewById(R.id.web_view);
        progressBar = findViewById(R.id.progress_bar);
        imageView = findViewById(R.id.pdf_image_view);
        pageIndicator = findViewById(R.id.page_indicator);

        // 隐藏不再需要的水印层
        View watermarkLayer = findViewById(R.id.watermark_layer);
        if (watermarkLayer != null) {
            watermarkLayer.setVisibility(View.GONE);
        }

        // 配置WebView
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String scheme = request.getUrl().getScheme();
                if ("file".equals(scheme)) {
                    return false;
                }
                Log.d(TAG, "External URL blocked: " + request.getUrl());
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                Log.d(TAG, "onPageFinished: " + url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Log.e(TAG, "WebView error: code=" + errorCode + ", desc=" + description);
                progressBar.setVisibility(View.GONE);
                runOnUiThread(() -> Toast.makeText(PdfViewerActivity.this,
                        "加载文件失败: " + description, Toast.LENGTH_SHORT).show());
            }
        });

        configureSecuritySettings();
    }

    /**
     * 构建并加载预览URL
     * 
     * 关键逻辑：
     * - 未审批完成 → 使用 /preview 接口（服务端返回带水印PDF）
     * - 已审批完成 → 使用 /file 接口（返回原始无水印PDF）
     */
    private void buildAndLoadPreviewUrl() {
        try {
            String baseUrl = getBaseUrl();
            StringBuilder urlBuilder = new StringBuilder(baseUrl);

            if (receiptId != -1) {
                // 票据预览
                if (isApprovalCompleted) {
                    urlBuilder.append("/api/mobile/receipts/").append(receiptId).append("/file");
                } else {
                    urlBuilder.append("/api/mobile/receipts/").append(receiptId).append("/preview");
                }
            } else if (stampApprovalId != -1) {
                // 印章审批合同预览
                if (isApprovalCompleted) {
                    urlBuilder.append("/api/mobile/stamp-approvals/").append(stampApprovalId).append("/file");
                } else {
                    urlBuilder.append("/api/mobile/stamp-approvals/").append(stampApprovalId).append("/preview");
                }
            } else {
                // 回退：使用原始传入的URL（兼容旧调用方式）
                String fileUrl = getIntent().getStringExtra(EXTRA_FILE_URL);
                if (!TextUtils.isEmpty(fileUrl)) {
                    loadPdfFromServer(fileUrl);
                    return;
                } else {
                    Toast.makeText(this, "无法确定文件来源", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            }

            // 追加Token认证参数
            String token = tokenManager.getToken();
            if (token != null && !token.isEmpty()) {
                urlBuilder.append("?token=").append(token);
            }

            String finalUrl = urlBuilder.toString();
            Log.d(TAG, "Loading URL: " + finalUrl + " [watermarked=" + !isApprovalCompleted + "]");

            Toast.makeText(this,
                    isApprovalCompleted ? "正在加载文件..." : "正在生成安全预览...",
                    Toast.LENGTH_SHORT).show();

            loadPdfFromServer(finalUrl);

        } catch (Exception e) {
            Log.e(TAG, "Build URL failed", e);
            Toast.makeText(this, "URL构建失败", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * 从服务器下载PDF到本地缓存并加载
     * 包含重试机制，网络不稳定时自动重试
     */
    private void loadPdfFromServer(String url) {
        Log.d(TAG, "开始下载PDF: " + url);
        loadPdfFromServerWithRetry(url, 0);
    }
    
    private static final int MAX_RETRIES = 2;  // 最大重试次数
    private static final int RETRY_DELAY_MS = 1500;  // 重试间隔时间
    
    private void loadPdfFromServerWithRetry(String url, int retryCount) {
        progressBar.setVisibility(View.VISIBLE);
        
        if (retryCount > 0) {
            runOnUiThread(() -> {
                Toast.makeText(this, "正在重试... (" + retryCount + "/" + MAX_RETRIES + ")", Toast.LENGTH_SHORT).show();
            });
        }

        new Thread(() -> {
            try {
                String token = tokenManager.getToken();
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                        .build();

                Request.Builder requestBuilder = new Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer " + token);

                Request request = requestBuilder.build();
                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    // 验证Content-Type是否为PDF
                    String contentType = response.header("Content-Type", "");
                    MediaType mediaType = response.body().contentType();
                    
                    if (mediaType == null || !mediaType.toString().contains("application/pdf")) {
                        // 如果不是PDF，尝试读取错误信息
                        String errorBody = response.body().string();
                        Log.e(TAG, "Invalid Content-Type: " + contentType + ", Response: " + errorBody);
                        
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            // 尝试解析JSON错误信息
                            try {
                                JSONObject json = new JSONObject(errorBody);
                                String message = json.optString("message", "服务器返回了无效的响应");
                                Toast.makeText(PdfViewerActivity.this, message, Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Toast.makeText(PdfViewerActivity.this, 
                                        "获取文件失败：服务器返回了无效的响应", Toast.LENGTH_LONG).show();
                            }
                        });
                        return;
                    }
                    
                    InputStream inputStream = response.body().byteStream();

                    File cacheDir = getExternalCacheDir();
                    if (cacheDir == null) cacheDir = getCacheDir();

                    tempPdfFile = new File(cacheDir, "preview_" + System.currentTimeMillis() + ".pdf");
                    FileOutputStream fos = new FileOutputStream(tempPdfFile);

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                    fos.close();
                    inputStream.close();

                    long fileSize = tempPdfFile.length();
                    Log.d(TAG, "PDF downloaded: size=" + fileSize + " bytes, watermarked=" + !isApprovalCompleted);

                    // 检查文件是否为空
                    if (fileSize == 0) {
                        handleDownloadFailure(url, retryCount, "服务器返回了空文件");
                        return;
                    }

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        try {
                            // 使用FileProvider获取PDF的content:// URI
                            Uri pdfUri = FileProvider.getUriForFile(PdfViewerActivity.this,
                                    getApplicationContext().getPackageName() + ".fileprovider",
                                    tempPdfFile);
                            Log.d(TAG, "Opening PDF with Intent: " + pdfUri.toString());

                            // 方法1：尝试使用本地PDF查看器
                            Intent localIntent = new Intent(Intent.ACTION_VIEW);
                            localIntent.setDataAndType(pdfUri, "application/pdf");
                            localIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            if (localIntent.resolveActivity(getPackageManager()) != null) {
                                startActivity(localIntent);
                                Log.d(TAG, "PDF viewer launched successfully");
                                return;
                            }

                            // 方法2：尝试使用Google PDF Viewer
                            Log.d(TAG, "No local PDF viewer, trying Google PDF Viewer");
                            Intent googleIntent = new Intent(Intent.ACTION_VIEW);
                            googleIntent.setDataAndType(pdfUri, "application/pdf");
                            googleIntent.setPackage("com.google.android.apps.pdfviewer");
                            googleIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            if (googleIntent.resolveActivity(getPackageManager()) != null) {
                                startActivity(googleIntent);
                                Log.d(TAG, "Google PDF Viewer launched successfully");
                                return;
                            }

                            // 方法3：使用内置PdfRenderer显示PDF
                            Log.d(TAG, "Using built-in PdfRenderer to display PDF");
                            displayPdfWithRenderer(tempPdfFile);

                        } catch (Exception e) {
                            Log.e(TAG, "Failed to open PDF", e);
                            Toast.makeText(PdfViewerActivity.this,
                                    "打开文件失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    String errorMsg = "HTTP " + response.code();
                    handleDownloadFailure(url, retryCount, errorMsg);
                }
            } catch (IOException e) {
                Log.e(TAG, "Download PDF failed", e);
                handleDownloadFailure(url, retryCount, e.getMessage());
            }
        }).start();
    }
    
    /**
     * 处理下载失败，包含重试逻辑
     */
    private void handleDownloadFailure(String url, int retryCount, String errorMsg) {
        if (retryCount < MAX_RETRIES) {
            // 还有重试次数，延迟后重试
            Log.d(TAG, "Download failed, retrying in " + RETRY_DELAY_MS + "ms... (" + (retryCount + 1) + "/" + MAX_RETRIES + ")");
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                loadPdfFromServerWithRetry(url, retryCount + 1);
            }, RETRY_DELAY_MS);
        } else {
            // 重试次数用完，显示错误
            Log.e(TAG, "Download failed after " + MAX_RETRIES + " retries");
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(PdfViewerActivity.this,
                        "下载失败: " + errorMsg + "（请检查网络连接后重试）", 
                        Toast.LENGTH_LONG).show();
            });
        }
    }

    /**
     * 配置WebView安全设置
     */
    private void configureSecuritySettings() {
        if (!isApprovalCompleted) {
            // 审批未完成：禁止下载
            webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
                Toast.makeText(PdfViewerActivity.this,
                        "审批未完成，禁止下载文件。水印已嵌入文件中。",
                        Toast.LENGTH_LONG).show();
                Log.d(TAG, "Download blocked: approval not completed");
            });
        } else {
            // 审批已完成：允许下载
            webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
                Toast.makeText(PdfViewerActivity.this,
                                "开始下载文件", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Download allowed");
            });
        }
    }

    /**
     * 从配置或当前连接提取服务器基础URL
     */
    private String getBaseUrl() {
        // 尝试从RetrofitClient获取基础URL
        try {
            String apiUrl = com.example.android_client.network.RetrofitClient.getBaseUrl();
            if (!TextUtils.isEmpty(apiUrl)) return apiUrl;
        } catch (Exception ignored) {}

        // 默认值
        return "http://10.0.2.2:9095";
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        // 清理临时文件
        if (tempPdfFile != null && tempPdfFile.exists()) {
            try {
                if (tempPdfFile.delete()) {
                    Log.d(TAG, "Temp PDF deleted: " + tempPdfFile.getName());
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to delete temp file: " + e.getMessage());
            }
        }

        if (webView != null) {
            webView.destroy();
        }

        // 回收Bitmap
        if (currentBitmap != null && !currentBitmap.isRecycled()) {
            currentBitmap.recycle();
            currentBitmap = null;
        }

        super.onDestroy();
    }

    /**
     * 使用WebView显示PDF（备选方案）
     * 通过Google Docs Viewer在线显示PDF
     */
    private void displayPdfInWebView(File pdfFile) {
        try {
            // 获取PDF的FileProvider URI
            Uri pdfUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", pdfFile);

            // 使用Google Docs Viewer在线显示PDF
            // 需要将content:// URI转换为可访问的URL
            String pdfUrl = "https://docs.google.com/gview?embedded=1&url="
                    + URLEncoder.encode(pdfUri.toString(), "UTF-8");

            Log.d(TAG, "Loading PDF via Google Docs Viewer: " + pdfUrl);
            progressBar.setVisibility(View.VISIBLE);
            webView.loadUrl(pdfUrl);
        } catch (Exception e) {
            Log.e(TAG, "Failed to display PDF in WebView", e);
            Toast.makeText(this,
                    "无法显示PDF文件: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 使用内置PdfRenderer显示PDF（主要方案）
     * 不依赖任何外部应用或网络连接
     */
    private void displayPdfWithRenderer(File pdfFile) {
        try {
            Log.d(TAG, "Opening PDF with PdfRenderer: " + pdfFile.getAbsolutePath());

            // 使用PdfRenderer打开PDF
            ParcelFileDescriptor fd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer pdfRenderer = new PdfRenderer(fd);

            int pageCount = pdfRenderer.getPageCount();
            Log.d(TAG, "PDF has " + pageCount + " pages");

            // 获取第一页
            PdfRenderer.Page firstPage = pdfRenderer.openPage(0);

            // 获取页面尺寸
            int width = firstPage.getWidth() * 2; // 2x缩放以提高清晰度
            int height = firstPage.getHeight() * 2;

            // 创建Bitmap
            Bitmap pageBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            pageBitmap.eraseColor(Color.WHITE);

            // 渲染第一页
            firstPage.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            firstPage.close();

            // 隐藏WebView，显示ImageView
            webView.setVisibility(View.GONE);
            imageView.setVisibility(View.VISIBLE);

            // 设置Bitmap到ImageView
            imageView.setImageBitmap(pageBitmap);
            currentBitmap = pageBitmap;

            // 更新页码指示器
            pageIndicator.setVisibility(View.VISIBLE);
            pageIndicator.setText(String.format("第 1 / %d 页", pageCount));

            // 设置点击监听器用于翻页
            setupPageNavigation(pdfRenderer, pageCount);

            Log.d(TAG, "PDF rendered successfully with built-in PdfRenderer");

        } catch (Exception e) {
            Log.e(TAG, "Failed to render PDF with PdfRenderer", e);
            Toast.makeText(this,
                    "显示PDF失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 设置页面导航功能
     */
    private void setupPageNavigation(final PdfRenderer pdfRenderer, final int pageCount) {
        currentPdfRenderer = pdfRenderer;
        currentPageCount = pageCount;
        currentPageIndex = 0;

        // 点击屏幕左侧翻到上一页，右侧翻到下一页
        imageView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                float x = event.getX();
                float width = imageView.getWidth();

                if (x < width / 3) {
                    // 左侧1/3区域：上一页
                    navigateToPage(currentPageIndex - 1);
                } else if (x > width * 2 / 3) {
                    // 右侧1/3区域：下一页
                    navigateToPage(currentPageIndex + 1);
                }
                return true;
            }
            return false;
        });
    }

    /**
     * 导航到指定页面
     */
    private void navigateToPage(int pageIndex) {
        if (currentPdfRenderer == null) return;

        // 边界检查
        if (pageIndex < 0) pageIndex = 0;
        if (pageIndex >= currentPageCount) pageIndex = currentPageCount - 1;

        if (pageIndex == currentPageIndex) return;

        try {
            // 渲染新页面
            PdfRenderer.Page page = currentPdfRenderer.openPage(pageIndex);
            int width = page.getWidth() * 2;
            int height = page.getHeight() * 2;

            Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            newBitmap.eraseColor(Color.WHITE);
            page.render(newBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();

            // 回收旧Bitmap（避免内存泄漏）
            if (currentBitmap != null && currentBitmap != newBitmap) {
                currentBitmap.recycle();
            }
            imageView.setImageBitmap(newBitmap);
            currentBitmap = newBitmap;

            currentPageIndex = pageIndex;
            pageIndicator.setText(String.format("第 %d / %d 页", pageIndex + 1, currentPageCount));

            Log.d(TAG, "Navigated to page " + (pageIndex + 1));

        } catch (Exception e) {
            Log.e(TAG, "Failed to navigate to page " + pageIndex, e);
        }
    }
}
