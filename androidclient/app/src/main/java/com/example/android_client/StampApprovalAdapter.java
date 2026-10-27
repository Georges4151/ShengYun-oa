package com.example.android_client;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android_client.model.StampApproval;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class StampApprovalAdapter extends RecyclerView.Adapter<StampApprovalAdapter.ViewHolder> {

    private List<StampApproval> approvalList;
    private OnItemClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public interface OnItemClickListener {
        void onItemClick(StampApproval approval);
    }

    public StampApprovalAdapter(List<StampApproval> approvalList, OnItemClickListener listener) {
        this.approvalList = approvalList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stamp_approval, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StampApproval approval = approvalList.get(position);
        holder.bind(approval, listener);
    }

    @Override
    public int getItemCount() {
        return approvalList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvContractName;
        TextView tvSealName;
        TextView tvApplicantName;
        TextView tvProjectName;
        TextView tvApplyTime;
        TextView tvStatus;

        ViewHolder(View itemView) {
            super(itemView);
            tvContractName = itemView.findViewById(R.id.tv_contract_name);
            tvSealName = itemView.findViewById(R.id.tv_seal_name);
            tvApplicantName = itemView.findViewById(R.id.tv_applicant_name);
            tvProjectName = itemView.findViewById(R.id.tv_project_name);
            tvApplyTime = itemView.findViewById(R.id.tv_apply_time);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }

        void bind(StampApproval approval, OnItemClickListener listener) {
            tvContractName.setText(approval.getContractName() != null ? approval.getContractName() : "-");
            tvSealName.setText(approval.getSealName() != null ? approval.getSealName() : "-");
            tvApplicantName.setText(approval.getApplicantName() != null ? approval.getApplicantName() : "-");
            tvProjectName.setText(approval.getProjectName() != null ? approval.getProjectName() : "-");

            if (approval.getApplyTime() != null) {
                tvApplyTime.setText(dateFormat.format(approval.getApplyTime()));
            } else {
                tvApplyTime.setText("-");
            }

            String statusText = approval.getStatusText();
            if (statusText == null) {
                statusText = "待审批";
            }
            tvStatus.setText(statusText);

            String status = approval.getStatus();
            if (status == null || "pending".equals(status)) {
                tvStatus.setBackgroundResource(R.drawable.status_pending);
                tvStatus.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_orange_dark));
            } else if ("approved".equals(status)) {
                tvStatus.setBackgroundResource(R.drawable.status_approved);
                tvStatus.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
            } else if ("rejected".equals(status)) {
                tvStatus.setBackgroundResource(R.drawable.status_rejected);
                tvStatus.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(approval);
                }
            });
        }
    }
}
