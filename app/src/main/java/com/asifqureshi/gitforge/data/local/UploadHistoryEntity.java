package com.asifqureshi.gitforge.data.local;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "upload_history")
public class UploadHistoryEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "repo_name")
    private String repoName;

    @ColumnInfo(name = "branch")
    private String branch;

    @ColumnInfo(name = "commit_message")
    private String commitMessage;

    @ColumnInfo(name = "timestamp")
    private long timestamp;

    @ColumnInfo(name = "status")
    private String status; // "SUCCESS", "FAILED"

    @ColumnInfo(name = "commit_sha")
    private String commitSha;

    @ColumnInfo(name = "file_count")
    private int fileCount;

    @ColumnInfo(name = "error_message")
    private String errorMessage;

    public UploadHistoryEntity(String repoName, String branch, String commitMessage,
                               long timestamp, String status, String commitSha,
                               int fileCount, String errorMessage) {
        this.repoName = repoName;
        this.branch = branch;
        this.commitMessage = commitMessage;
        this.timestamp = timestamp;
        this.status = status;
        this.commitSha = commitSha;
        this.fileCount = fileCount;
        this.errorMessage = errorMessage;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getRepoName() { return repoName != null ? repoName : ""; }
    public void setRepoName(String repoName) { this.repoName = repoName; }

    public String getBranch() { return branch != null ? branch : ""; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getCommitMessage() { return commitMessage != null ? commitMessage : ""; }
    public void setCommitMessage(String commitMessage) { this.commitMessage = commitMessage; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status != null ? status : ""; }
    public void setStatus(String status) { this.status = status; }

    public String getCommitSha() { return commitSha != null ? commitSha : ""; }
    public void setCommitSha(String commitSha) { this.commitSha = commitSha; }

    public int getFileCount() { return fileCount; }
    public void setFileCount(int fileCount) { this.fileCount = fileCount; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
