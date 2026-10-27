package com.ceams.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 远程API客户端工具类
 * 用于JavaFX客户端调用后端API
 * 
 * 功能特性：
 * - 支持 GET/POST/PUT/DELETE 请求
 * - JWT令牌管理（自动添加Authorization头）
 * - 文件上传（multipart/form-data）
 * - 文件下载
 * - 超时配置（连接30秒，读取30秒）
 * - 错误处理（401、403、404、500等）
 * - 日志记录
 * - 单例模式实现
 * 
 * @author Soko Coder
 * @version 1.0
 */
public class RemoteApiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(RemoteApiClient.class);
    
    /**
     * 默认服务器地址
     */
    private static final String DEFAULT_BASE_URL = "https://api.yourcompany.com";
    
    /**
     * 连接超时时间（秒）
     */
    private static final int CONNECT_TIMEOUT = 30;
    
    /**
     * 读取超时时间（秒）
     */
    private static final int READ_TIMEOUT = 30;
    
    /**
     * 写入超时时间（秒）
     */
    private static final int WRITE_TIMEOUT = 30;
    
    /**
     * 单例实例
     */
    private static volatile RemoteApiClient instance;
    
    /**
     * OkHttp客户端
     */
    private final OkHttpClient httpClient;
    
    /**
     * Gson实例
     */
    private final Gson gson;
    
    /**
     * 服务器基础URL
     */
    private String baseUrl;
    
    /**
     * JWT令牌
     */
    private String jwtToken;
    
    /**
     * 私有构造函数，防止外部实例化
     */
    private RemoteApiClient() {
        this(DEFAULT_BASE_URL);
    }
    
    /**
     * 私有构造函数，支持自定义服务器地址
     * 
     * @param baseUrl 服务器基础URL
     */
    private RemoteApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        
        // 创建日志拦截器
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(
            message -> logger.debug("HTTP: {}", message)
        );
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        // 创建认证拦截器
        Interceptor authInterceptor = chain -> {
            Request original = chain.request();
            
            // 登录接口不需要添加Authorization头
            if (original.url().encodedPath().contains("/api/auth/login")) {
                return chain.proceed(original);
            }
            
            // 如果请求已经有Authorization头，直接使用
            if (original.header("Authorization") != null) {
                return chain.proceed(original);
            }
            
            Request.Builder builder = original.newBuilder();
            if (jwtToken != null && !jwtToken.isEmpty()) {
                builder.header("Authorization", "Bearer " + jwtToken);
            }
            
            return chain.proceed(builder.build());
        };
        
        // 创建OkHttpClient
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build();
        
        // 创建Gson实例
        this.gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();
        
        logger.info("RemoteApiClient initialized with base URL: {}", baseUrl);
    }
    
    /**
     * 获取单例实例
     * 
     * @return RemoteApiClient实例
     */
    public static RemoteApiClient getInstance() {
        if (instance == null) {
            synchronized (RemoteApiClient.class) {
                if (instance == null) {
                    instance = new RemoteApiClient();
                }
            }
        }
        return instance;
    }
    
    /**
     * 获取单例实例（自定义服务器地址）
     * 
     * @param baseUrl 服务器基础URL
     * @return RemoteApiClient实例
     */
    public static RemoteApiClient getInstance(String baseUrl) {
        if (instance == null) {
            synchronized (RemoteApiClient.class) {
                if (instance == null) {
                    instance = new RemoteApiClient(baseUrl);
                }
            }
        }
        return instance;
    }
    
    /**
     * 设置JWT令牌
     * 
     * @param token JWT令牌
     */
    public void setJwtToken(String token) {
        this.jwtToken = token;
        logger.info("JWT token updated");
    }
    
    /**
     * 获取JWT令牌
     * 
     * @return JWT令牌
     */
    public String getJwtToken() {
        return jwtToken;
    }
    
    /**
     * 清除JWT令牌
     */
    public void clearJwtToken() {
        this.jwtToken = null;
        logger.info("JWT token cleared");
    }
    
    /**
     * 设置服务器基础URL
     * 
     * @param baseUrl 服务器基础URL
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        logger.info("Base URL updated to: {}", baseUrl);
    }
    
    /**
     * 获取服务器基础URL
     * 
     * @return 服务器基础URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }
    
    /**
     * 发送GET请求
     * 
     * @param url 请求URL（相对于baseUrl）
     * @param responseType 响应类型
     * @param <T> 响应类型泛型
     * @return 响应对象
     * @throws IOException 请求异常
     */
    public <T> T get(String url, Class<T> responseType) throws IOException {
        return get(url, null, responseType);
    }
    
    /**
     * 发送GET请求（带查询参数）
     * 
     * @param url 请求URL（相对于baseUrl）
     * @param queryParams 查询参数
     * @param responseType 响应类型
     * @param <T> 响应类型泛型
     * @return 响应对象
     * @throws IOException 请求异常
     */
    public <T> T get(String url, Map<String, String> queryParams, Class<T> responseType) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + url).newBuilder();
        if (queryParams != null) {
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
            }
        }
        
        Request request = new Request.Builder()
            .url(urlBuilder.build())
            .get()
            .build();
        
        return executeRequest(request, responseType);
    }
    
    /**
     * 发送POST请求（JSON格式）
     * 
     * @param url 请求URL（相对于baseUrl）
     * @param body 请求体对象
     * @param responseType 响应类型
     * @param <T> 响应类型泛型
     * @return 响应对象
     * @throws IOException 请求异常
     */
    public <T> T post(String url, Object body, Class<T> responseType) throws IOException {
        String json = gson.toJson(body);
        RequestBody requestBody = RequestBody.create(
            json, 
            MediaType.parse("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
            .url(baseUrl + url)
            .post(requestBody)
            .build();
        
        return executeRequest(request, responseType);
    }
    
    /**
     * 发送POST请求（表单格式）
     * 
     * @param url 请求URL（相对于baseUrl）
     * @param formData 表单数据
     * @param responseType 响应类型
     * @param <T> 响应类型泛型
     * @return 响应对象
     * @throws IOException 请求异常
     */
    public <T> T postForm(String url, Map<String, String> formData, Class<T> responseType) throws IOException {
        FormBody.Builder formBuilder = new FormBody.Builder();
        if (formData != null) {
            for (Map.Entry<String, String> entry : formData.entrySet()) {
                formBuilder.add(entry.getKey(), entry.getValue());
            }
        }
        
        Request request = new Request.Builder()
            .url(baseUrl + url)
            .post(formBuilder.build())
            .build();
        
        return executeRequest(request, responseType);
    }
    
    /**
     * 发送PUT请求
     * 
     * @param url 请求URL（相对于baseUrl）
     * @param body 请求体对象
     * @param responseType 响应类型
     * @param <T> 响应类型泛型
     * @return 响应对象
     * @throws IOException 请求异常
     */
    public <T> T put(String url, Object body, Class<T> responseType) throws IOException {
        String json = gson.toJson(body);
        RequestBody requestBody = RequestBody.create(
            json, 
            MediaType.parse("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
            .url(baseUrl + url)
            .put(requestBody)
            .build();
        
        return executeRequest(request, responseType);
    }
    
    /**
     * 发送DELETE请求
     * 
     * @param url 请求URL（相对于baseUrl）
     * @param responseType 响应类型
     * @param <T> 响应类型泛型
     * @return 响应对象
     * @throws IOException 请求异常
     */
    public <T> T delete(String url, Class<T> responseType) throws IOException {
        Request request = new Request.Builder()
            .url(baseUrl + url)
            .delete()
            .build();
        
        return executeRequest(request, responseType);
    }
    
    /**
     * 上传文件（multipart/form-data）
     * 
     * @param url 请求URL（相对于baseUrl）
     * @param file 要上传的文件
     * @param fileFieldName 文件字段名
     * @param additionalParams 额外的表单参数
     * @param responseType 响应类型
     * @param <T> 响应类型泛型
     * @return 响应对象
     * @throws IOException 请求异常
     */
    public <T> T uploadFile(String url, File file, String fileFieldName, 
                           Map<String, String> additionalParams, Class<T> responseType) throws IOException {
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
            .setType(MultipartBody.FORM);
        
        // 添加文件
        RequestBody fileBody = RequestBody.create(
            file, 
            MediaType.parse("application/octet-stream")
        );
        multipartBuilder.addFormDataPart(fileFieldName, file.getName(), fileBody);
        
        // 添加额外参数
        if (additionalParams != null) {
            for (Map.Entry<String, String> entry : additionalParams.entrySet()) {
                multipartBuilder.addFormDataPart(entry.getKey(), entry.getValue());
            }
        }
        
        Request request = new Request.Builder()
            .url(baseUrl + url)
            .post(multipartBuilder.build())
            .build();
        
        return executeRequest(request, responseType);
    }
    
    /**
     * 下载文件
     * 
     * @param url 请求URL（相对于baseUrl）
     * @param destinationFile 目标文件
     * @return 下载的文件
     * @throws IOException 请求异常
     */
    public File downloadFile(String url, File destinationFile) throws IOException {
        Request request = new Request.Builder()
            .url(baseUrl + url)
            .get()
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                handleErrorResponse(response);
            }
            
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("Response body is null");
            }
            
            try (InputStream inputStream = responseBody.byteStream();
                 FileOutputStream outputStream = new FileOutputStream(destinationFile)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesRead = 0;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }
                
                logger.info("File downloaded successfully: {} ({} bytes)", 
                    destinationFile.getAbsolutePath(), totalBytesRead);
            }
        }
        
        return destinationFile;
    }
    
    /**
     * 执行HTTP请求
     * 
     * @param request 请求对象
     * @param responseType 响应类型
     * @param <T> 响应类型泛型
     * @return 响应对象
     * @throws IOException 请求异常
     */
    private <T> T executeRequest(Request request, Class<T> responseType) throws IOException {
        logger.debug("Executing {} request to: {}", request.method(), request.url());
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                handleErrorResponse(response);
            }
            
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("Response body is null");
            }
            
            String responseString = responseBody.string();
            logger.debug("Response: {}", responseString);
            
            return gson.fromJson(responseString, responseType);
        } catch (Exception e) {
            logger.error("Request failed: {}", e.getMessage(), e);
            throw new IOException("Request failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * 处理错误响应
     * 
     * @param response HTTP响应
     * @throws IOException 请求异常
     */
    private void handleErrorResponse(Response response) throws IOException {
        int code = response.code();
        String message = response.message();
        ResponseBody responseBody = response.body();
        String errorBody = responseBody != null ? responseBody.string() : "";
        
        logger.error("HTTP error {} {}: {}", code, message, errorBody);
        
        switch (code) {
            case 401:
                throw new IOException("Unauthorized (401): Authentication required or token expired");
            case 403:
                throw new IOException("Forbidden (403): Access denied");
            case 404:
                throw new IOException("Not Found (404): Resource not found");
            case 500:
                throw new IOException("Internal Server Error (500): " + errorBody);
            case 502:
                throw new IOException("Bad Gateway (502): Server error");
            case 503:
                throw new IOException("Service Unavailable (503): Server temporarily unavailable");
            default:
                throw new IOException("HTTP error " + code + ": " + message);
        }
    }
    
    /**
     * 获取Gson实例
     * 
     * @return Gson实例
     */
    public Gson getGson() {
        return gson;
    }
    
    /**
     * 关闭客户端（释放资源）
     */
    public void close() {
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
            logger.info("RemoteApiClient closed");
        }
    }
}
