package com.asifqureshi.gitforge.repository;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.asifqureshi.gitforge.data.local.AppDatabase;
import com.asifqureshi.gitforge.data.local.SecurePreferences;
import com.asifqureshi.gitforge.data.local.UploadHistoryEntity;
import com.asifqureshi.gitforge.data.remote.ApiClient;
import com.asifqureshi.gitforge.data.remote.GitHubApiService;
import com.asifqureshi.gitforge.model.FileItem;
import com.asifqureshi.gitforge.model.GitDatabaseModels;
import com.asifqureshi.gitforge.model.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class UploadManager {
    private static final String TAG = "UploadManager";

    private final AppDatabase database;
    private final SecurePreferences securePreferences;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public interface UploadProgressListener {
        void onProgress(String currentStep, int currentItem, int totalItems, int percent);
        void onSuccess(String commitSha, String htmlUrl, int totalFiles);
        void onError(String errorMessage);
    }

    public UploadManager(AppDatabase database, SecurePreferences securePreferences) {
        this.database = database;
        this.securePreferences = securePreferences;
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void executeUpload(final Repository repo,
                              final String targetBranch,
                              final String commitMessage,
                              final List<FileItem> selectedFiles,
                              final boolean createBranchIfMissing,
                              final UploadProgressListener listener) {

        executor.execute(new Runnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                String owner = repo.getOwner() != null ? repo.getOwner().getLogin() : "";
                if (owner.isEmpty() && repo.getFullName().contains("/")) {
                    owner = repo.getFullName().split("/")[0];
                }
                String repoName = repo.getName();
                String token = securePreferences.getGitHubToken();
                GitHubApiService service = ApiClient.getGitHubApiService(token);

                try {
                    // Filter selected files
                    List<FileItem> filesToUpload = new ArrayList<>();
                    for (FileItem item : selectedFiles) {
                        if (item.isSelected() && !item.isDirectory()) {
                            filesToUpload.add(item);
                        }
                    }

                    if (filesToUpload.isEmpty()) {
                        postError(listener, "No files selected for commit.");
                        return;
                    }

                    postProgress(listener, "Resolving target branch '" + targetBranch + "'...", 0, filesToUpload.size(), 5);

                    // Step 1: Get latest commit SHA for target branch
                    Response<GitDatabaseModels.GitRef> branchRefResp = service.getBranchRef(owner, repoName, targetBranch).execute();
                    String latestCommitSha = null;

                    if (branchRefResp.isSuccessful() && branchRefResp.body() != null) {
                        latestCommitSha = branchRefResp.body().getSha();
                    } else if (createBranchIfMissing) {
                        // Try fallback to repo default branch to branch off of
                        String defaultBranch = repo.getDefaultBranch();
                        Response<GitDatabaseModels.GitRef> defaultRefResp = service.getBranchRef(owner, repoName, defaultBranch).execute();
                        if (defaultRefResp.isSuccessful() && defaultRefResp.body() != null) {
                            String baseSha = defaultRefResp.body().getSha();
                            Response<GitDatabaseModels.GitRef> createBranchResp = service.createBranchRef(
                                    owner, repoName,
                                    new GitDatabaseModels.CreateRefRequest("refs/heads/" + targetBranch, baseSha)
                            ).execute();
                            if (createBranchResp.isSuccessful()) {
                                latestCommitSha = baseSha;
                            } else {
                                postError(listener, "Could not create branch '" + targetBranch + "': " + createBranchResp.message());
                                return;
                            }
                        }
                    }

                    if (latestCommitSha == null || latestCommitSha.isEmpty()) {
                        postError(listener, "Branch '" + targetBranch + "' not found on remote repository.");
                        return;
                    }

                    // Step 2: Get base tree SHA from base commit
                    postProgress(listener, "Fetching base tree...", 0, filesToUpload.size(), 15);
                    Response<GitDatabaseModels.CommitObject> commitResp = service.getCommit(owner, repoName, latestCommitSha).execute();
                    if (!commitResp.isSuccessful() || commitResp.body() == null) {
                        postError(listener, "Failed to retrieve base commit: " + commitResp.message());
                        return;
                    }
                    String baseTreeSha = commitResp.body().getTreeSha();

                    // Step 3: Create Blobs for each file
                    List<GitDatabaseModels.TreeEntry> treeEntries = new ArrayList<>();
                    int total = filesToUpload.size();
                    for (int i = 0; i < total; i++) {
                        FileItem file = filesToUpload.get(i);
                        int percent = 20 + (int) (((i + 1) / (float) total) * 50);
                        postProgress(listener, "Creating Git blob: " + file.getRelativePath(), i + 1, total, percent);

                        byte[] content = file.getData() != null ? file.getData() : new byte[0];
                        String base64Content = Base64.encodeToString(content, Base64.NO_WRAP);

                        GitDatabaseModels.CreateBlobRequest blobReq = new GitDatabaseModels.CreateBlobRequest(base64Content, "base64");
                        Response<GitDatabaseModels.CreateBlobResponse> blobResp = service.createBlob(owner, repoName, blobReq).execute();

                        if (!blobResp.isSuccessful() || blobResp.body() == null) {
                            String err = "Failed to upload file blob for '" + file.getRelativePath() + "': " + blobResp.message();
                            recordHistory(repo.getFullName(), targetBranch, commitMessage, "FAILED", "", total, err);
                            postError(listener, err);
                            return;
                        }

                        String blobSha = blobResp.body().getSha();
                        treeEntries.add(new GitDatabaseModels.TreeEntry(file.getRelativePath(), "100644", "blob", blobSha));
                    }

                    // Step 4: Create new Git Tree
                    postProgress(listener, "Constructing Git tree...", total, total, 75);
                    GitDatabaseModels.CreateTreeRequest treeReq = new GitDatabaseModels.CreateTreeRequest(baseTreeSha, treeEntries);
                    Response<GitDatabaseModels.CreateTreeResponse> treeResp = service.createTree(owner, repoName, treeReq).execute();

                    if (!treeResp.isSuccessful() || treeResp.body() == null) {
                        String err = "Failed to create tree: " + treeResp.message();
                        recordHistory(repo.getFullName(), targetBranch, commitMessage, "FAILED", "", total, err);
                        postError(listener, err);
                        return;
                    }
                    String newTreeSha = treeResp.body().getSha();

                    // Step 5: Create Git Commit
                    postProgress(listener, "Creating commit object...", total, total, 85);
                    GitDatabaseModels.CreateCommitRequest commitReq = new GitDatabaseModels.CreateCommitRequest(
                            commitMessage,
                            newTreeSha,
                            Collections.singletonList(latestCommitSha)
                    );
                    Response<GitDatabaseModels.CreateCommitResponse> newCommitResp = service.createCommit(owner, repoName, commitReq).execute();

                    if (!newCommitResp.isSuccessful() || newCommitResp.body() == null) {
                        String err = "Failed to create commit: " + newCommitResp.message();
                        recordHistory(repo.getFullName(), targetBranch, commitMessage, "FAILED", "", total, err);
                        postError(listener, err);
                        return;
                    }
                    String newCommitSha = newCommitResp.body().getSha();

                    // Step 6: Update Branch Reference
                    postProgress(listener, "Updating branch reference...", total, total, 95);
                    GitDatabaseModels.UpdateRefRequest updateRefReq = new GitDatabaseModels.UpdateRefRequest(newCommitSha, false);
                    Response<GitDatabaseModels.GitRef> updateRefResp = service.updateBranchRef(owner, repoName, targetBranch, updateRefReq).execute();

                    if (!updateRefResp.isSuccessful()) {
                        String err = "Failed to update branch reference: " + updateRefResp.message();
                        recordHistory(repo.getFullName(), targetBranch, commitMessage, "FAILED", newCommitSha, total, err);
                        postError(listener, err);
                        return;
                    }

                    String htmlUrl = "https://github.com/" + owner + "/" + repoName + "/commit/" + newCommitSha;
                    recordHistory(repo.getFullName(), targetBranch, commitMessage, "SUCCESS", newCommitSha, total, null);

                    postProgress(listener, "Pushed successfully!", total, total, 100);
                    postSuccess(listener, newCommitSha, htmlUrl, total);

                } catch (Exception e) {
                    Log.e(TAG, "Upload failed with exception", e);
                    String err = e.getMessage() != null ? e.getMessage() : "Unknown upload error";
                    recordHistory(repo.getFullName(), targetBranch, commitMessage, "FAILED", "", selectedFiles.size(), err);
                    postError(listener, "Upload error: " + err);
                }
            }
        });
    }

    private void recordHistory(String repo, String branch, String msg, String status, String sha, int count, String error) {
        try {
            UploadHistoryEntity entity = new UploadHistoryEntity(
                    repo, branch, msg, System.currentTimeMillis(), status, sha, count, error
            );
            database.uploadHistoryDao().insert(entity);
        } catch (Exception e) {
            Log.e(TAG, "Failed to record upload history", e);
        }
    }

    private void postProgress(final UploadProgressListener listener, final String step, final int current, final int total, final int percent) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) listener.onProgress(step, current, total, percent);
            }
        });
    }

    private void postSuccess(final UploadProgressListener listener, final String commitSha, final String htmlUrl, final int totalFiles) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) listener.onSuccess(commitSha, htmlUrl, totalFiles);
            }
        });
    }

    private void postError(final UploadProgressListener listener, final String message) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) listener.onError(message);
            }
        });
    }
}
