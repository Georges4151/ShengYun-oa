package com.example.android_client;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.android_client.model.LoginResponse;
import com.example.android_client.network.RetrofitClient;
import com.example.android_client.util.TokenManager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;

    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化RetrofitClient
        RetrofitClient.initialize(getApplicationContext());

        setContentView(R.layout.activity_login);

        tokenManager = TokenManager.getInstance(this);

        if (tokenManager.isLoggedIn()) {
            startMainActivity();
            finish();
            return;
        }

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progress_bar);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
    }

    private void login() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            etUsername.setError("请输入用户名");
            etUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("请输入密码");
            etPassword.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", username);
        credentials.put("password", password);

        RetrofitClient.getInstance().getApiService()
                .login(credentials)
                .enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                        progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);

                        if (response.isSuccessful() && response.body() != null) {
                            LoginResponse loginResponse = response.body();
                            if (loginResponse.isSuccess() && loginResponse.getToken() != null) {
                                tokenManager.saveLoginResponse(loginResponse);
                                startMainActivity();
                                finish();
                            } else {
                                String msg = loginResponse.getMessage() != null ? loginResponse.getMessage() : "登录失败";
                                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "登录失败，请检查用户名和密码", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<LoginResponse> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);

                        Log.e(TAG, "Login failed", t);
                        Log.e(TAG, "Error type: " + t.getClass().getSimpleName());
                        Log.e(TAG, "Error message: " + t.getMessage());

                        String errorMessage = "网络连接失败，请检查网络";

                        if (t instanceof java.net.UnknownHostException) {
                            errorMessage = "无法连接到服务器，请检查服务器地址";
                        } else if (t instanceof java.net.SocketTimeoutException) {
                            errorMessage = "连接超时，请检查网络连接";
                        } else if (t instanceof java.net.ConnectException) {
                            errorMessage = "服务器连接被拒绝，请检查服务器是否运行";
                        } else if (t instanceof javax.net.ssl.SSLException) {
                            errorMessage = "SSL连接失败，请检查网络安全配置";
                        }

                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
