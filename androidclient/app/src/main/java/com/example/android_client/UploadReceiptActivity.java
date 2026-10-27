package com.example.android_client;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android_client.model.ApiResponse;
import com.example.android_client.model.Project;
import com.example.android_client.model.Receipt;
import com.example.android_client.network.RetrofitClient;
import com.example.android_client.util.TokenManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadReceiptActivity extends AppCompatActivity {

    private static final String TAG = "UploadReceiptActivity";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAPTURE_IMAGE_REQUEST = 2;

    private ImageView ivReceipt;
    private EditText etAmount;
    private EditText etDescription;
    private EditText etReporterName;
    private EditText etProjectSearch;
    private ImageView ivProjectDropdown;
    private TextView tvSelectedProject;
    private Spinner spReceiptType;
    private Button btnSelectImage;
    private Button btnCaptureImage;
    private Button btnUpload;
    private ProgressBar progressBar;
    private TextView tvSelectedDate;
    private TextView tvFileName;

    private TokenManager tokenManager;
    private File selectedFile;
    private Calendar selectedDate;
    private SimpleDateFormat dateFormat;

    private Project selectedProject;
    private Dialog projectSelectorDialog;
    private ProjectAdapter projectAdapter;
    private List<Project> allProjects;
    private boolean isSettingProjectText = false;

    private final String[] receiptTypes = { "材料费", "人工费", "设备费", "其他" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_receipt);

        tokenManager = TokenManager.getInstance(this);

        if (!tokenManager.isLoggedIn()) {
            startLoginActivity();
            finish();
            return;
        }

        initViews();
        setupListeners();

        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        selectedDate = Calendar.getInstance();
        updateDateDisplay();
    }

    private void initViews() {
        ivReceipt = findViewById(R.id.iv_receipt);
        etAmount = findViewById(R.id.et_amount);
        etDescription = findViewById(R.id.et_description);
        etReporterName = findViewById(R.id.et_reporter_name);
        etProjectSearch = findViewById(R.id.et_project_search);
        ivProjectDropdown = findViewById(R.id.iv_project_dropdown);
        tvSelectedProject = findViewById(R.id.tv_selected_project);
        spReceiptType = findViewById(R.id.sp_receipt_type);
        btnSelectImage = findViewById(R.id.btn_select_image);
        btnCaptureImage = findViewById(R.id.btn_capture_image);
        btnUpload = findViewById(R.id.btn_upload);
        progressBar = findViewById(R.id.progress_bar);
        tvSelectedDate = findViewById(R.id.tv_selected_date);
        tvFileName = findViewById(R.id.tv_file_name);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, receiptTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spReceiptType.setAdapter(adapter);

        projectAdapter = new ProjectAdapter();
    }

    private void setupListeners() {
        btnSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });

        btnCaptureImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureImage();
            }
        });

        tvSelectedDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadReceipt();
            }
        });

        ivProjectDropdown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProjectSelector();
            }
        });

        etProjectSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isSettingProjectText) {
                    return;
                }
                String keyword = s.toString().trim();
                if (!keyword.isEmpty() && allProjects != null) {
                    showProjectSelectorWithKeyword(keyword);
                }
            }
        });

        etProjectSearch.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    showProjectSelector();
                }
            }
        });
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "选择票据图片"), PICK_IMAGE_REQUEST);
    }

    private void captureImage() {
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, CAPTURE_IMAGE_REQUEST);
        } else {
            Toast.makeText(this, "没有可用的相机应用", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null) {
                Uri imageUri = data.getData();
                if (imageUri != null) {
                    processSelectedImage(imageUri);
                }
            } else if (requestCode == CAPTURE_IMAGE_REQUEST && data != null) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    if (imageBitmap != null) {
                        saveBitmapToFile(imageBitmap);
                    }
                }
            }
        }
    }

    private void processSelectedImage(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream != null) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
                saveBitmapToFile(bitmap);
            }
        } catch (IOException e) {
            Log.e(TAG, "处理图片失败", e);
            Toast.makeText(this, "处理图片失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveBitmapToFile(Bitmap bitmap) {
        try {
            File cacheDir = getCacheDir();
            selectedFile = new File(cacheDir, "receipt_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(selectedFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();

            ivReceipt.setImageBitmap(bitmap);
            tvFileName.setText("已选择: " + selectedFile.getName());
            tvFileName.setVisibility(View.VISIBLE);
        } catch (IOException e) {
            Log.e(TAG, "保存图片失败", e);
            Toast.makeText(this, "保存图片失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(android.widget.DatePicker view, int year, int month, int dayOfMonth) {
                        selectedDate.set(year, month, dayOfMonth);
                        updateDateDisplay();
                    }
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void updateDateDisplay() {
        tvSelectedDate.setText(dateFormat.format(selectedDate.getTime()));
    }

    private void showProjectSelector() {
        if (projectSelectorDialog == null) {
            projectSelectorDialog = new Dialog(this);
            projectSelectorDialog.setContentView(R.layout.dialog_project_selector);
            projectSelectorDialog.setTitle("选择项目");
            projectSelectorDialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);

            RecyclerView rvProjects = projectSelectorDialog.findViewById(R.id.rv_projects);
            EditText etSearch = projectSelectorDialog.findViewById(R.id.et_search);
            Button btnSearch = projectSelectorDialog.findViewById(R.id.btn_search);
            TextView tvEmpty = projectSelectorDialog.findViewById(R.id.tv_empty);
            ProgressBar dialogProgressBar = projectSelectorDialog.findViewById(R.id.progress_bar);

            rvProjects.setLayoutManager(new LinearLayoutManager(this));
            rvProjects.setAdapter(projectAdapter);

            projectAdapter.setOnProjectClickListener(new ProjectAdapter.OnProjectClickListener() {
                @Override
                public void onProjectClick(Project project) {
                    selectedProject = project;
                    isSettingProjectText = true;
                    etProjectSearch.setText(project.getProjectName());
                    isSettingProjectText = false;
                    tvSelectedProject.setText("已选择: " + project.getProjectName());
                    tvSelectedProject.setVisibility(View.VISIBLE);
                    projectSelectorDialog.dismiss();
                }
            });

            btnSearch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String keyword = etSearch.getText().toString().trim();
                    loadProjects(keyword, tvEmpty, dialogProgressBar, rvProjects);
                }
            });

            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    String keyword = s.toString().trim();
                    filterProjects(keyword, tvEmpty, rvProjects);
                }
            });
        }

        projectSelectorDialog.show();
        loadProjects(null,
                projectSelectorDialog.findViewById(R.id.tv_empty),
                projectSelectorDialog.findViewById(R.id.progress_bar),
                projectSelectorDialog.findViewById(R.id.rv_projects));
    }

    private void showProjectSelectorWithKeyword(String keyword) {
        if (projectSelectorDialog != null && projectSelectorDialog.isShowing()) {
            TextView tvEmpty = projectSelectorDialog.findViewById(R.id.tv_empty);
            RecyclerView rvProjects = projectSelectorDialog.findViewById(R.id.rv_projects);
            filterProjects(keyword, tvEmpty, rvProjects);
        } else {
            showProjectSelector();
        }
    }

    private void loadProjects(String keyword, TextView tvEmpty, ProgressBar progressBar, RecyclerView rvProjects) {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        rvProjects.setVisibility(View.GONE);

        RetrofitClient.getInstance().getApiService().getProjects(keyword)
                .enqueue(new Callback<ApiResponse<List<Project>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<Project>>> call,
                            Response<ApiResponse<List<Project>>> response) {
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<List<Project>> apiResponse = response.body();
                            if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                allProjects = apiResponse.getData();
                                projectAdapter.setProjects(allProjects);

                                if (allProjects.isEmpty()) {
                                    tvEmpty.setVisibility(View.VISIBLE);
                                    rvProjects.setVisibility(View.GONE);
                                } else {
                                    tvEmpty.setVisibility(View.GONE);
                                    rvProjects.setVisibility(View.VISIBLE);
                                }
                            } else {
                                tvEmpty.setText("加载失败: " + apiResponse.getMessage());
                                tvEmpty.setVisibility(View.VISIBLE);
                                rvProjects.setVisibility(View.GONE);
                            }
                        } else {
                            tvEmpty.setText("加载失败，请重试");
                            tvEmpty.setVisibility(View.VISIBLE);
                            rvProjects.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<Project>>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        tvEmpty.setText("网络错误: " + t.getMessage());
                        tvEmpty.setVisibility(View.VISIBLE);
                        rvProjects.setVisibility(View.GONE);
                        Log.e(TAG, "加载项目列表失败", t);
                    }
                });
    }

    private void filterProjects(String keyword, TextView tvEmpty, RecyclerView rvProjects) {
        if (allProjects == null || allProjects.isEmpty()) {
            return;
        }

        if (keyword.isEmpty()) {
            projectAdapter.setProjects(allProjects);
            tvEmpty.setVisibility(View.GONE);
            rvProjects.setVisibility(View.VISIBLE);
            return;
        }

        java.util.List<Project> filteredList = new java.util.ArrayList<>();
        for (Project project : allProjects) {
            if (project.getProjectName() != null &&
                    project.getProjectName().toLowerCase().contains(keyword.toLowerCase())) {
                filteredList.add(project);
            } else if (project.getClientName() != null &&
                    project.getClientName().toLowerCase().contains(keyword.toLowerCase())) {
                filteredList.add(project);
            }
        }

        projectAdapter.setProjects(filteredList);
        if (filteredList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvProjects.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvProjects.setVisibility(View.VISIBLE);
        }
    }

    private void uploadReceipt() {
        if (selectedFile == null || !selectedFile.exists()) {
            Toast.makeText(this, "请先选择票据图片", Toast.LENGTH_SHORT).show();
            return;
        }

        String amountStr = etAmount.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String reporterName = etReporterName.getText().toString().trim();
        String receiptType = receiptTypes[spReceiptType.getSelectedItemPosition()];

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "请输入金额", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "金额格式不正确", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnUpload.setEnabled(false);

        RequestBody fileBody = RequestBody.create(MediaType.parse("image/*"), selectedFile);
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", selectedFile.getName(), fileBody);

        RequestBody typeBody = RequestBody.create(MediaType.parse("text/plain"), receiptType);

        Integer projectId = selectedProject != null ? selectedProject.getId() : null;

        RetrofitClient.getInstance().getApiService().uploadReceipt(
                filePart,
                receiptType,
                projectId,
                null,
                reporterName,
                selectedDate.getTimeInMillis(),
                String.valueOf(amount),
                description).enqueue(new Callback<Receipt>() {
                    @Override
                    public void onResponse(Call<Receipt> call, Response<Receipt> response) {
                        progressBar.setVisibility(View.GONE);
                        btnUpload.setEnabled(true);

                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(UploadReceiptActivity.this, "票据上传成功！", Toast.LENGTH_SHORT).show();
                            clearForm();
                        } else {
                            String errorMsg = "上传失败: " + response.code();
                            try {
                                if (response.errorBody() != null) {
                                    errorMsg = response.errorBody().string();
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "读取错误信息失败", e);
                            }
                            Toast.makeText(UploadReceiptActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Receipt> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        btnUpload.setEnabled(true);
                        Log.e(TAG, "上传失败", t);
                        Toast.makeText(UploadReceiptActivity.this, "上传失败: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void clearForm() {
        selectedFile = null;
        selectedProject = null;
        ivReceipt.setImageResource(R.drawable.ic_receipt_placeholder);
        etAmount.setText("");
        etDescription.setText("");
        etReporterName.setText("");
        isSettingProjectText = true;
        etProjectSearch.setText("");
        isSettingProjectText = false;
        tvSelectedProject.setVisibility(View.GONE);
        spReceiptType.setSelection(0);
        selectedDate = Calendar.getInstance();
        updateDateDisplay();
        tvFileName.setVisibility(View.GONE);
    }

    private void startLoginActivity() {
        Intent intent = new Intent(UploadReceiptActivity.this, LoginActivity.class);
        startActivity(intent);
    }
}
