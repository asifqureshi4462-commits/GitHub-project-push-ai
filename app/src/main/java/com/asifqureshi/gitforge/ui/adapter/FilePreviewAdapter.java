package com.asifqureshi.gitforge.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.asifqureshi.gitforge.model.FileItem;
import com.example.R;

import java.util.ArrayList;
import java.util.List;

public class FilePreviewAdapter extends RecyclerView.Adapter<FilePreviewAdapter.FileViewHolder> {

    public interface OnFileItemActionListener {
        void onFilePreviewRequested(FileItem item);
        void onFileSelectionChanged(int position, boolean isSelected);
    }

    private List<FileItem> fileItems = new ArrayList<>();
    private OnFileItemActionListener listener;

    public void setFiles(List<FileItem> list) {
        this.fileItems = list != null ? list : new ArrayList<FileItem>();
        notifyDataSetChanged();
    }

    public void setListener(OnFileItemActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_preview, parent, false);
        return new FileViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final FileViewHolder holder, final int position) {
        final FileItem item = fileItems.get(position);
        holder.tvFilePath.setText(item.getRelativePath());
        holder.tvFileSize.setText(item.getFormattedSize());

        holder.cbFileSelect.setOnCheckedChangeListener(null);
        holder.cbFileSelect.setChecked(item.isSelected());

        holder.cbFileSelect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                item.setSelected(isChecked);
                if (listener != null) listener.onFileSelectionChanged(holder.getAdapterPosition(), isChecked);
            }
        });

        holder.btnFilePreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onFilePreviewRequested(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return fileItems.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbFileSelect;
        TextView tvFilePath, tvFileSize, btnFilePreview;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            cbFileSelect = itemView.findViewById(R.id.cbFileSelect);
            tvFilePath = itemView.findViewById(R.id.tvFilePath);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            btnFilePreview = itemView.findViewById(R.id.btnFilePreview);
        }
    }
}
