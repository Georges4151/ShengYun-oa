package com.example.android_client.network;

import com.example.android_client.model.ApiResponse;
import com.example.android_client.model.ApprovalRecord;
import com.example.android_client.model.ApprovalRequest;
import com.example.android_client.model.LoginResponse;
import com.example.android_client.model.Project;
import com.example.android_client.model.Receipt;
import com.example.android_client.model.UserNotification;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

        @POST("api/mobile/login")
        Call<LoginResponse> login(@Body Map<String, String> credentials);

        @GET("api/mobile/receipts/pending")
        Call<ApiResponse<Map<String, Object>>> getPendingApprovals();

        @Multipart
        @POST("api/approval/upload")
        Call<Receipt> uploadReceipt(
                        @Part MultipartBody.Part file,
                        @Query("type") String type,
                        @Query("projectId") Integer projectId,
                        @Query("reporterId") Integer reporterId,
                        @Query("reporterName") String reporterName,
                        @Query("receiptDate") Long receiptDate,
                        @Query("amount") String amount,
                        @Query("description") String description);

        @GET("api/mobile/stamp-approvals/pending")
        Call<ApiResponse<Map<String, Object>>> getPendingStampApprovals();

        @GET("api/mobile/stamp-approvals/{id}")
        Call<ApiResponse<com.example.android_client.model.StampApproval>> getStampApprovalDetail(
                        @Path("id") Integer id);

        @POST("api/mobile/stamp-approvals/{id}/approve")
        Call<ApiResponse<Void>> approveStamp(@Path("id") Integer id);

        @POST("api/mobile/stamp-approvals/{id}/reject")
        Call<ApiResponse<Void>> rejectStamp(
                        @Path("id") Integer id,
                        @Body Map<String, String> body);

        @GET("api/mobile/receipts/{id}")
        Call<ApiResponse<Receipt>> getReceiptDetail(@Path("id") Integer id);

        @GET("api/mobile/receipts/{receiptId}/history")
        Call<ApiResponse<List<ApprovalRecord>>> getApprovalHistory(@Path("receiptId") Integer receiptId);

        @POST("api/mobile/receipts/approve")
        Call<ApiResponse<Void>> submitApproval(@Body ApprovalRequest request);

        @GET("api/mobile/dashboard/stats")
        Call<ApiResponse<Map<String, Object>>> getDashboardStats();

        @GET("api/mobile/notifications")
        Call<ApiResponse<List<UserNotification>>> getNotifications();

        @GET("api/mobile/notifications/{id}")
        Call<ApiResponse<UserNotification>> getNotificationDetail(@Path("id") Integer id);

        @GET("api/mobile/notifications/unread-count")
        Call<ApiResponse<Map<String, Object>>> getUnreadCount();

        @POST("api/mobile/notifications/read")
        Call<ApiResponse<Void>> markAsRead(@Body Map<String, Integer> body);

        @POST("api/mobile/notifications/delete")
        Call<ApiResponse<Void>> deleteNotification(@Body Map<String, Integer> body);

        @POST("api/mobile/notifications/batch-delete")
        Call<ApiResponse<Void>> batchDeleteNotifications(@Body Map<String, List<Integer>> body);

        @GET("api/mobile/projects")
        Call<ApiResponse<List<Project>>> getProjects(@Query("keyword") String keyword);

        @GET("api/mobile/stamp-approvals/{id}/file")
        Call<okhttp3.ResponseBody> getStampApprovalFile(@Path("id") Integer id);

        @GET("api/mobile/receipts/{id}/file")
        Call<okhttp3.ResponseBody> getReceiptFile(@Path("id") Integer id);
}
