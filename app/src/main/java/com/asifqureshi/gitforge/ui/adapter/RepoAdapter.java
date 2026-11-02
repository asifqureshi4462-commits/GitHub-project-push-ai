package com.asifqureshi.gitforge.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.asifqureshi.gitforge.model.Repository;
import com.example.R;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class RepoAdapter extends RecyclerView.Adapter<RepoAdapter.RepoViewHolder> {

    public interface OnRepoClickListener {
        void onRepoClick(Repository repository);
    }

    private List<Repository> repositories = new ArrayList<>();
    private OnRepoClickListener listener;
    private long selectedRepoId = -1;

    public void setRepositories(List<Repository> list) {
        this.repositories = list != null ? list : new ArrayList<Repository>();
        notifyDataSetChanged();
    }

    public void setOnRepoClickListener(OnRepoClickListener listener) {
        this.listener = listener;
    }

    public void setSelectedRepoId(long id) {
        this.selectedRepoId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RepoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_repository, parent, false);
        return new RepoViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RepoViewHolder holder, int position) {
        final Repository repo = repositories.get(position);
        holder.tvRepoName.setText(repo.getFullName());
        holder.tvRepoDescription.setText(repo.getDescription());
        holder.tvRepoDefaultBranch.setText(repo.getDefaultBranch());
        holder.tvRepoStars.setText("★ " + repo.getStargazersCount());

        if (repo.isPrivate()) {
            holder.tvRepoVisibility.setText("Private");
            holder.tvRepoVisibility.setTextColor(0xFFEF4444);
        } else {
            holder.tvRepoVisibility.setText("Public");
            holder.tvRepoVisibility.setTextColor(0xFF00CEC9);
        }

        if (repo.getId() == selectedRepoId) {
            holder.cardRepo.setStrokeColor(0xFF6C5CE7);
            holder.cardRepo.setStrokeWidth(4);
        } else {
            holder.cardRepo.setStrokeColor(0xFF2B3D72);
            holder.cardRepo.setStrokeWidth(2);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onRepoClick(repo);
            }
        });
    }

    @Override
    public int getItemCount() {
        return repositories.size();
    }

    static class RepoViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardRepo;
        TextView tvRepoName, tvRepoDescription, tvRepoVisibility, tvRepoDefaultBranch, tvRepoStars;

        public RepoViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRepo = itemView.findViewById(R.id.cardRepo);
            tvRepoName = itemView.findViewById(R.id.tvRepoName);
            tvRepoDescription = itemView.findViewById(R.id.tvRepoDescription);
            tvRepoVisibility = itemView.findViewById(R.id.tvRepoVisibility);
            tvRepoDefaultBranch = itemView.findViewById(R.id.tvRepoDefaultBranch);
            tvRepoStars = itemView.findViewById(R.id.tvRepoStars);
        }
    }
}
