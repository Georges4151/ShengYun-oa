package com.example.android_client;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android_client.model.UserNotification;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<UserNotification> notificationList;
    private OnItemClickListener listener;
    private boolean isSelectionMode = false;
    private Set<Integer> selectedPositions = new HashSet<>();

    public interface OnItemClickListener {
        void onItemClick(UserNotification notification, int position);

        void onItemLongClick(UserNotification notification, int position);

        void onSelectionChanged(int selectedCount);
    }

    public NotificationAdapter(List<UserNotification> notificationList, OnItemClickListener listener) {
        this.notificationList = notificationList;
        this.listener = listener;
    }

    public void setSelectionMode(boolean selectionMode) {
        this.isSelectionMode = selectionMode;
        if (!selectionMode) {
            selectedPositions.clear();
        }
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    public void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
        } else {
            selectedPositions.add(position);
        }
        notifyItemChanged(position);
        if (listener != null) {
            listener.onSelectionChanged(selectedPositions.size());
        }
    }

    public void selectAll() {
        selectedPositions.clear();
        for (int i = 0; i < notificationList.size(); i++) {
            selectedPositions.add(i);
        }
        notifyDataSetChanged();
        if (listener != null) {
            listener.onSelectionChanged(selectedPositions.size());
        }
    }

    public void clearSelection() {
        selectedPositions.clear();
        notifyDataSetChanged();
        if (listener != null) {
            listener.onSelectionChanged(0);
        }
    }

    public Set<Integer> getSelectedPositions() {
        return selectedPositions;
    }

    public int getSelectedCount() {
        return selectedPositions.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserNotification notification = notificationList.get(position);
        holder.bind(notification, position, listener);
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        View viewPriority;
        TextView tvTitle;
        TextView tvContent;
        TextView tvSender;
        TextView tvTime;
        View viewUnread;
        CheckBox checkBox;

        ViewHolder(View itemView) {
            super(itemView);
            viewPriority = itemView.findViewById(R.id.view_priority);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvSender = itemView.findViewById(R.id.tv_sender);
            tvTime = itemView.findViewById(R.id.tv_time);
            viewUnread = itemView.findViewById(R.id.view_unread);
            checkBox = itemView.findViewById(R.id.checkbox);
        }

        void bind(UserNotification notification, int position, OnItemClickListener listener) {
            tvTitle.setText(notification.getDisplayTitle());
            tvContent.setText(notification.getDisplayContent());
            tvTime.setText(notification.getDisplayTime());

            String senderName = notification.getNotification() != null
                    ? notification.getNotification().getSenderName()
                    : null;
            tvSender.setText(senderName != null ? "发送者: " + senderName : "");

            int priorityColor = notification.getPriorityColor();
            viewPriority.setBackgroundColor(priorityColor);

            boolean isUnread = notification.isUnread();
            viewUnread.setVisibility(isUnread ? View.VISIBLE : View.GONE);

            if (isUnread) {
                itemView.setBackgroundColor(Color.parseColor("#E3F2FD"));
            } else {
                itemView.setBackgroundColor(Color.WHITE);
            }

            if (isSelectionMode) {
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setChecked(selectedPositions.contains(position));
                checkBox.setOnClickListener(v -> {
                    toggleSelection(position);
                });
            } else {
                checkBox.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    if (isSelectionMode) {
                        toggleSelection(position);
                    } else {
                        listener.onItemClick(notification, position);
                    }
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null && !isSelectionMode) {
                    listener.onItemLongClick(notification, position);
                }
                return true;
            });
        }
    }
}
