package com.example.android_client;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android_client.model.Receipt;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ApprovalTodoAdapter extends RecyclerView.Adapter<ApprovalTodoAdapter.ViewHolder> {

    private Context context;
    private List<Receipt> receipts;
    private OnItemClickListener listener;
    private SimpleDateFormat dateFormatter;

    public interface OnItemClickListener {
        void onItemClick(Receipt receipt);
    }

    public ApprovalTodoAdapter(Context context, List<Receipt> receipts, OnItemClickListener listener) {
        this.context = context;
        this.receipts = receipts;
        this.listener = listener;
        this.dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_approval_todo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (receipts == null || position >= receipts.size()) {
            return;
        }

        Receipt receipt = receipts.get(position);
        holder.bind(receipt);
    }

    @Override
    public int getItemCount() {
        return receipts != null ? receipts.size() : 0;
    }

    public void setReceipts(List<Receipt> receipts) {
        this.receipts = receipts;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvReceiptNumber;
        private TextView tvProjectName;
        private TextView tvAmount;
        private TextView tvSubmitTime;
        private TextView tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReceiptNumber = itemView.findViewById(R.id.tv_receipt_number);
            tvProjectName = itemView.findViewById(R.id.tv_project_name);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvSubmitTime = itemView.findViewById(R.id.tv_submit_time);
            tvStatus = itemView.findViewById(R.id.tv_status);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null && receipts != null) {
                        listener.onItemClick(receipts.get(position));
                    }
                }
            });
        }

        public void bind(Receipt receipt) {
            tvReceiptNumber.setText("票据ID：" + receipt.getId());

            String projectName = receipt.getProjectName();
            if (projectName == null || projectName.isEmpty()) {
                projectName = "未指定项目";
            }
            tvProjectName.setText("项目：" + projectName);

            if (receipt.getAmount() != null) {
                tvAmount.setText("金额：¥" + receipt.getAmount().toString());
            } else {
                tvAmount.setText("金额：未知");
            }

            tvStatus.setText(receipt.getStatusDisplay());

            Date createTime = receipt.getCreateTime();
            if (createTime != null) {
                tvSubmitTime.setText("提交时间：" + dateFormatter.format(createTime));
            } else {
                tvSubmitTime.setText("提交时间：未知");
            }
        }
    }
}
