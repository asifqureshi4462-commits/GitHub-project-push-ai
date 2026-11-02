package com.asifqureshi.gitforge.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.asifqureshi.gitforge.data.local.UploadHistoryEntity;
import com.example.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    public interface OnHistoryItemClickListener {
        void onHistoryClick(UploadHistoryEntity history);
    }

    private List<UploadHistoryEntity> historyList = new ArrayList<>();
    private OnHistoryItemClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault());

    public void setHistoryList(List<UploadHistoryEntity> list) {
        this.historyList = list != null ? list : new ArrayList<UploadHistoryEntity>();
        notifyDataSetChanged();
    }

    public void setListener(OnHistoryItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        final UploadHistoryEntity item = historyList.get(position);
        holder.tvHistoryRepo.setText(item.getRepoName());
        holder.tvHistoryBranch.setText(item.getBranch());
        holder.tvHistoryCommitMsg.setText(item.getCommitMessage());
        holder.tvHistoryTime.setText(dateFormat.format(new Date(item.getTimestamp())));
        holder.tvHistoryFileCount.setText("• " + item.getFileCount() + " files");

        if ("SUCCESS".equalsIgnoreCase(item.getStatus())) {
            holder.tvHistoryStatus.setText("SUCCESS");
            holder.tvHistoryStatus.setTextColor(0xFF10B981);
        } else {
            holder.tvHistoryStatus.setText("FAILED");
            holder.tvHistoryStatus.setTextColor(0xFFEF4444);
        }

        String sha = item.getCommitSha();
        if (sha != null && !sha.isEmpty()) {
            String shortSha = sha.length() > 8 ? sha.substring(0, 8) : sha;
            holder.tvHistorySha.setText("SHA: " + shortSha);
            holder.tvHistorySha.setVisibility(View.VISIBLE);
        } else if (item.getErrorMessage() != null && !item.getErrorMessage().isEmpty()) {
            holder.tvHistorySha.setText("Err: " + item.getErrorMessage());
            holder.tvHistorySha.setVisibility(View.VISIBLE);
        } else {
            holder.tvHistorySha.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onHistoryClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvHistoryStatus, tvHistoryTime, tvHistoryRepo, tvHistoryBranch, tvHistoryFileCount, tvHistoryCommitMsg, tvHistorySha;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHistoryStatus = itemView.findViewById(R.id.tvHistoryStatus);
            tvHistoryTime = itemView.findViewById(R.id.tvHistoryTime);
            tvHistoryRepo = itemView.findViewById(R.id.tvHistoryRepo);
            tvHistoryBranch = itemView.findViewById(R.id.tvHistoryBranch);
            tvHistoryFileCount = itemView.findViewById(R.id.tvHistoryFileCount);
            tvHistoryCommitMsg = itemView.findViewById(R.id.tvHistoryCommitMsg);
            tvHistorySha = itemView.findViewById(R.id.tvHistorySha);
        }
    }
}
