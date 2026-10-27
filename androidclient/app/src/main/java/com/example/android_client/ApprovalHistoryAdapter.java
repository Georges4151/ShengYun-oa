package com.example.android_client;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android_client.model.ApprovalRecord;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ApprovalHistoryAdapter extends RecyclerView.Adapter<ApprovalHistoryAdapter.ViewHolder> {

    private Context context;
    private List<ApprovalRecord> records;
    private SimpleDateFormat dateFormatter;

    public ApprovalHistoryAdapter(Context context, List<ApprovalRecord> records) {
        this.context = context;
        this.records = records;
        this.dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_approval_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (records == null || position >= records.size()) {
            return;
        }

        ApprovalRecord record = records.get(position);
        holder.bind(record);
    }

    @Override
    public int getItemCount() {
        return records != null ? records.size() : 0;
    }

    public void setRecords(List<ApprovalRecord> records) {
        this.records = records;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvApprover;
        private TextView tvAction;
        private TextView tvComment;
        private TextView tvTimestamp;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvApprover = itemView.findViewById(R.id.tv_approver);
            tvAction = itemView.findViewById(R.id.tv_action);
            tvComment = itemView.findViewById(R.id.tv_comment);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
        }

        public void bind(ApprovalRecord record) {
            String approverName = record.getApproverName();
            if (approverName == null || approverName.isEmpty()) {
                approverName = "未知";
            }
            tvApprover.setText("审批人：" + approverName + " (" + record.getApprovalLevelText() + ")");
            tvAction.setText(record.getStatusText());

            if (record.getComment() != null && !record.getComment().isEmpty()) {
                tvComment.setText("意见：" + record.getComment());
                tvComment.setVisibility(View.VISIBLE);
            } else {
                tvComment.setVisibility(View.GONE);
            }

            Date approvalTime = record.getApprovalTime();
            if (approvalTime != null) {
                tvTimestamp.setText(dateFormatter.format(approvalTime));
            } else {
                tvTimestamp.setText("未知时间");
            }
        }
    }
}
