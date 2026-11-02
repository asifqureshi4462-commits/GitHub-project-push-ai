package com.asifqureshi.gitforge.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.asifqureshi.gitforge.model.Branch;
import com.example.R;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class BranchAdapter extends RecyclerView.Adapter<BranchAdapter.BranchViewHolder> {

    public interface OnBranchClickListener {
        void onBranchClick(Branch branch);
    }

    private List<Branch> branches = new ArrayList<>();
    private String selectedBranchName = "";
    private OnBranchClickListener listener;

    public void setBranches(List<Branch> list, String selected) {
        this.branches = list != null ? list : new ArrayList<Branch>();
        this.selectedBranchName = selected != null ? selected : "";
        notifyDataSetChanged();
    }

    public void setOnBranchClickListener(OnBranchClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public BranchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_branch, parent, false);
        return new BranchViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BranchViewHolder holder, int position) {
        final Branch branch = branches.get(position);
        holder.tvBranchName.setText(branch.getName());

        String sha = branch.getSha();
        if (sha != null && sha.length() >= 7) {
            holder.tvBranchSha.setText("commit " + sha.substring(0, 7));
        } else {
            holder.tvBranchSha.setText("commit (latest)");
        }

        boolean isSelected = branch.getName().equalsIgnoreCase(selectedBranchName);
        holder.rbBranchSelect.setChecked(isSelected);

        if (isSelected) {
            holder.cardBranch.setStrokeColor(0xFF6C5CE7);
            holder.cardBranch.setStrokeWidth(3);
        } else {
            holder.cardBranch.setStrokeColor(0xFF2B3D72);
            holder.cardBranch.setStrokeWidth(1);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedBranchName = branch.getName();
                notifyDataSetChanged();
                if (listener != null) listener.onBranchClick(branch);
            }
        });
    }

    @Override
    public int getItemCount() {
        return branches.size();
    }

    static class BranchViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardBranch;
        TextView tvBranchName, tvBranchSha;
        RadioButton rbBranchSelect;

        public BranchViewHolder(@NonNull View itemView) {
            super(itemView);
            cardBranch = itemView.findViewById(R.id.cardBranch);
            tvBranchName = itemView.findViewById(R.id.tvBranchName);
            tvBranchSha = itemView.findViewById(R.id.tvBranchSha);
            rbBranchSelect = itemView.findViewById(R.id.rbBranchSelect);
        }
    }
}
