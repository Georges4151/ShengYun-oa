package com.example.android_client.network;

import android.content.Context;

import com.example.android_client.util.TokenManager;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit客户端单例类
 * 管理API服务的创建和配置
 */
public class RetrofitClient {

    // 服务器基础URL - 需要根据实际部署环境修改
    // Android模拟器使用10.0.2.2访问宿主机，真机使用实际IP地址
    // 如果需要修改为实际服务器IP，请更改下方BASE_URL的值
    // 例如：private static final String BASE_URL = "http://192.168.1.100:9095";
    private static String BASE_URL = "http://10.0.2.2:9095";

    private static RetrofitClient instance;
    private final ApiService apiService;
    private static Context appContext;

    private RetrofitClient() {
        // 创建日志拦截器
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        if (BuildConfig.DEBUG) {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
        }

        // 创建认证拦截器
        Interceptor authInterceptor = chain -> {
            Request original = chain.request();

            // 登录接口不需要添加Authorization头
            if (original.url().encodedPath().contains("/api/mobile/login")) {
                return chain.proceed(original);
            }

            // 如果请求已经有Authorization头，直接使用
            if (original.header("Authorization") != null) {
                return chain.proceed(original);
            }

            // 获取Token
            String authHeader = null;
            if (appContext != null) {
                try {
                    authHeader = TokenManager.getInstance(appContext).getAuthorizationHeader();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Request.Builder builder = original.newBuilder();
            if (authHeader != null && !authHeader.isEmpty()) {
                builder.header("Authorization", authHeader);
            }

            return chain.proceed(builder.build());
        };

        // 创建 OkHttp 客户端，禁用自动重定向
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(authInterceptor)
                .followRedirects(false)
                .followSslRedirects(false)
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        // 创建 Retrofit 实例
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // 创建 API 服务
        apiService = retrofit.create(ApiService.class);
    }

    /**
     * 初始化RetrofitClient
     * 必须在使用前调用此方法设置ApplicationContext
     */
    public static void initialize(Context context) {
        appContext = context.getApplicationContext();
    }

    /**
     * 获取RetrofitClient单例实例
     */
    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }

    /**
     * 获取API服务
     */
    public ApiService getApiService() {
        return apiService;
    }

    /**
     * 更新服务器地址（用于动态配置）
     * 
     * @param newBaseUrl 新的服务器地址，例如：http://192.168.1.100:9095
     */
    public static void updateBaseUrl(String newBaseUrl) {
        BASE_URL = newBaseUrl;
        // 清除现有实例，下次获取时会使用新地址创建
        instance = null;
        android.util.Log.d("RetrofitClient", "服务器地址已更新为: " + newBaseUrl);
    }

    /**
     * 获取当前服务器基础URL
     */
    public static String getBaseUrl() {
        return BASE_URL;
    }
}
