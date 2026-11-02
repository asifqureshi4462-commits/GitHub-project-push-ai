package com.asifqureshi.gitforge.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.asifqureshi.gitforge.data.local.AppDatabase;
import com.asifqureshi.gitforge.data.local.SecurePreferences;
import com.asifqureshi.gitforge.data.local.UploadHistoryEntity;
import com.asifqureshi.gitforge.model.Branch;
import com.asifqureshi.gitforge.model.DeviceCodeResponse;
import com.asifqureshi.gitforge.model.FileItem;
import com.asifqureshi.gitforge.model.Repository;
import com.asifqureshi.gitforge.model.User;
import com.asifqureshi.gitforge.repository.AIRepository;
import com.asifqureshi.gitforge.repository.GitHubRepository;
import com.asifqureshi.gitforge.repository.UploadManager;

import java.util.ArrayList;
import java.util.List;

public class MainViewModel extends AndroidViewModel {

    public static class UploadProgressState {
        public String step = "";
        public int currentItem = 0;
        public int totalItems = 0;
        public int percent = 0;
        public boolean isRunning = false;

        public UploadProgressState(String step, int currentItem, int totalItems, int percent, boolean isRunning) {
            this.step = step;
            this.currentItem = currentItem;
            this.totalItems = totalItems;
            this.percent = percent;
            this.isRunning = isRunning;
        }
    }

    public static class UploadResultState {
        public boolean isSuccess;
        public String commitSha;
        public String htmlUrl;
        public int totalFiles;
        public String errorMessage;

        public UploadResultState(boolean isSuccess, String commitSha, String htmlUrl, int totalFiles, String errorMessage) {
            this.isSuccess = isSuccess;
            this.commitSha = commitSha;
            this.htmlUrl = htmlUrl;
            this.totalFiles = totalFiles;
            this.errorMessage = errorMessage;
        }
    }

    public static class AiResultState {
        public boolean isLoading;
        public String output;
        public String error;
        public int toolType; // 1: Readme, 2: Review, 3: Explainer

        public AiResultState(boolean isLoading, String output, String error, int toolType) {
            this.isLoading = isLoading;
            this.output = output;
            this.error = error;
            this.toolType = toolType;
        }
    }

    private final SecurePreferences securePreferences;
    private final AppDatabase database;
    private final GitHubRepository gitHubRepository;
    private final UploadManager uploadManager;
    private final AIRepository aiRepository;

    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private final MutableLiveData<List<Repository>> repositories = new MutableLiveData<>();
    private final MutableLiveData<List<Repository>> filteredRepositories = new MutableLiveData<>();
    private final MutableLiveData<List<Branch>> branches = new MutableLiveData<>();
    private final MutableLiveData<Repository> selectedRepository = new MutableLiveData<>();
    private final MutableLiveData<String> selectedBranch = new MutableLiveData<>();
    private final MutableLiveData<List<FileItem>> selectedFiles = new MutableLiveData<>(new ArrayList<FileItem>());
    private final MutableLiveData<UploadProgressState> uploadProgress = new MutableLiveData<>();
    private final MutableLiveData<UploadResultState> uploadResult = new MutableLiveData<>();
    private final MutableLiveData<AiResultState> aiResult = new MutableLiveData<>();
    private final MutableLiveData<DeviceCodeResponse> deviceCode = new MutableLiveData<>();
    private final MutableLiveData<String> authError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public MainViewModel(@NonNull Application application) {
        super(application);
        this.securePreferences = new SecurePreferences(application);
        this.database = AppDatabase.getInstance(application);
        this.gitHubRepository = new GitHubRepository(securePreferences);
        this.uploadManager = new UploadManager(database, securePreferences);
        this.aiRepository = new AIRepository(securePreferences);

        if (securePreferences.hasValidToken()) {
            currentUser.setValue(new User(
                    securePreferences.getUserLogin(),
                    securePreferences.getUserName(),
                    securePreferences.getUserAvatar(),
                    0, 0
            ));
            refreshUser();
        }
    }

    public SecurePreferences getSecurePreferences() { return securePreferences; }

    public LiveData<User> getCurrentUser() { return currentUser; }
    public LiveData<List<Repository>> getRepositories() { return filteredRepositories; }
    public LiveData<List<Branch>> getBranches() { return branches; }
    public LiveData<Repository> getSelectedRepository() { return selectedRepository; }
    public LiveData<String> getSelectedBranch() { return selectedBranch; }
    public LiveData<List<FileItem>> getSelectedFiles() { return selectedFiles; }
    public LiveData<UploadProgressState> getUploadProgress() { return uploadProgress; }
    public LiveData<UploadResultState> getUploadResult() { return uploadResult; }
    public LiveData<AiResultState> getAiResult() { return aiResult; }
    public LiveData<DeviceCodeResponse> getDeviceCode() { return deviceCode; }
    public LiveData<String> getAuthError() { return authError; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    // History LiveData from Room
    public LiveData<List<UploadHistoryEntity>> getAllHistory() {
        return database.uploadHistoryDao().getAllHistory();
    }
    public LiveData<List<UploadHistoryEntity>> getRecentHistory() {
        return database.uploadHistoryDao().getRecentHistory();
    }
    public LiveData<Integer> getTotalUploads() {
        return database.uploadHistoryDao().getTotalUploadCount();
    }
    public LiveData<Integer> getSuccessUploads() {
        return database.uploadHistoryDao().getSuccessfulUploadCount();
    }

    public void loginWithToken(String token) {
        isLoading.setValue(true);
        authError.setValue(null);
        gitHubRepository.validateAndSaveToken(token, new GitHubRepository.Callback<User>() {
            @Override
            public void onSuccess(User result) {
                isLoading.setValue(false);
                currentUser.setValue(result);
                loadRepositories();
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                authError.setValue(error);
            }
        });
    }

    public void startDeviceOAuth(String clientId) {
        isLoading.setValue(true);
        gitHubRepository.startDeviceOAuth(clientId, new GitHubRepository.Callback<DeviceCodeResponse>() {
            @Override
            public void onSuccess(DeviceCodeResponse result) {
                isLoading.setValue(false);
                deviceCode.setValue(result);
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                authError.setValue(error);
            }
        });
    }

    public void pollDeviceToken(String clientId, String code) {
        isLoading.setValue(true);
        gitHubRepository.pollDeviceToken(clientId, code, new GitHubRepository.Callback<com.asifqureshi.gitforge.model.AccessTokenResponse>() {
            @Override
            public void onSuccess(com.asifqureshi.gitforge.model.AccessTokenResponse result) {
                if (result.getAccessToken() != null && !result.getAccessToken().isEmpty()) {
                    loginWithToken(result.getAccessToken());
                } else if (result.getError() != null) {
                    isLoading.setValue(false);
                    authError.setValue("OAuth status: " + result.getErrorDescription());
                }
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                authError.setValue(error);
            }
        });
    }

    public void refreshUser() {
        gitHubRepository.fetchCurrentUser(new GitHubRepository.Callback<User>() {
            @Override
            public void onSuccess(User result) {
                currentUser.setValue(result);
            }
            @Override
            public void onError(String error) {}
        });
    }

    public void logout() {
        securePreferences.clearAuth();
        currentUser.setValue(null);
        repositories.setValue(new ArrayList<Repository>());
        filteredRepositories.setValue(new ArrayList<Repository>());
        selectedRepository.setValue(null);
        selectedBranch.setValue(null);
        selectedFiles.setValue(new ArrayList<FileItem>());
    }

    public void loadRepositories() {
        if (!securePreferences.hasValidToken()) return;
        isLoading.setValue(true);
        gitHubRepository.fetchRepositories(new GitHubRepository.Callback<List<Repository>>() {
            @Override
            public void onSuccess(List<Repository> result) {
                isLoading.setValue(false);
                repositories.setValue(result);
                filteredRepositories.setValue(result);
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
            }
        });
    }

    public void searchRepositories(String query) {
        List<Repository> all = repositories.getValue();
        if (all == null) return;
        if (query == null || query.trim().isEmpty()) {
            filteredRepositories.setValue(all);
            return;
        }
        String q = query.toLowerCase().trim();
        List<Repository> filtered = new ArrayList<>();
        for (Repository r : all) {
            if (r.getName().toLowerCase().contains(q) || r.getFullName().toLowerCase().contains(q)) {
                filtered.add(r);
            }
        }
        filteredRepositories.setValue(filtered);
    }

    public void selectRepository(Repository repo) {
        selectedRepository.setValue(repo);
        if (repo != null) {
            selectedBranch.setValue(repo.getDefaultBranch());
            loadBranches(repo);
        }
    }

    public void loadBranches(Repository repo) {
        String owner = repo.getOwner() != null ? repo.getOwner().getLogin() : "";
        if (owner.isEmpty() && repo.getFullName().contains("/")) {
            owner = repo.getFullName().split("/")[0];
        }
        gitHubRepository.fetchBranches(owner, repo.getName(), new GitHubRepository.Callback<List<Branch>>() {
            @Override
            public void onSuccess(List<Branch> result) {
                branches.setValue(result);
            }

            @Override
            public void onError(String error) {}
        });
    }

    public void selectBranch(String branchName) {
        selectedBranch.setValue(branchName);
    }

    public void setSelectedFiles(List<FileItem> files) {
        selectedFiles.setValue(files);
    }

    public void toggleFileSelection(int index) {
        List<FileItem> current = selectedFiles.getValue();
        if (current != null && index >= 0 && index < current.size()) {
            FileItem item = current.get(index);
            item.setSelected(!item.isSelected());
            selectedFiles.setValue(new ArrayList<>(current));
        }
    }

    public void startUpload(String commitMessage, boolean createBranchIfMissing) {
        Repository repo = selectedRepository.getValue();
        String branch = selectedBranch.getValue();
        List<FileItem> files = selectedFiles.getValue();

        if (repo == null || branch == null || files == null || files.isEmpty()) {
            uploadResult.setValue(new UploadResultState(false, "", "", 0, "Missing destination repository, branch, or project files."));
            return;
        }

        uploadProgress.setValue(new UploadProgressState("Initializing upload...", 0, files.size(), 0, true));
        uploadResult.setValue(null);

        uploadManager.executeUpload(repo, branch, commitMessage, files, createBranchIfMissing, new UploadManager.UploadProgressListener() {
            @Override
            public void onProgress(String currentStep, int currentItem, int totalItems, int percent) {
                uploadProgress.setValue(new UploadProgressState(currentStep, currentItem, totalItems, percent, true));
            }

            @Override
            public void onSuccess(String commitSha, String htmlUrl, int totalFiles) {
                uploadProgress.setValue(new UploadProgressState("Upload complete!", totalFiles, totalFiles, 100, false));
                uploadResult.setValue(new UploadResultState(true, commitSha, htmlUrl, totalFiles, null));
            }

            @Override
            public void onError(String errorMessage) {
                uploadProgress.setValue(new UploadProgressState("Failed", 0, 0, 0, false));
                uploadResult.setValue(new UploadResultState(false, "", "", 0, errorMessage));
            }
        });
    }

    public void runAiReadme(String projectDescription) {
        aiResult.setValue(new AiResultState(true, null, null, 1));
        aiRepository.generateReadme(projectDescription, new AIRepository.Callback<String>() {
            @Override
            public void onSuccess(String result) {
                aiResult.setValue(new AiResultState(false, result, null, 1));
            }

            @Override
            public void onError(String error) {
                aiResult.setValue(new AiResultState(false, null, error, 1));
            }
        });
    }

    public void runAiReview(String codeSnippet) {
        aiResult.setValue(new AiResultState(true, null, null, 2));
        aiRepository.reviewCode(codeSnippet, new AIRepository.Callback<String>() {
            @Override
            public void onSuccess(String result) {
                aiResult.setValue(new AiResultState(false, result, null, 2));
            }

            @Override
            public void onError(String error) {
                aiResult.setValue(new AiResultState(false, null, error, 2));
            }
        });
    }

    public void runAiErrorExplain(String errorLog) {
        aiResult.setValue(new AiResultState(true, null, null, 3));
        aiRepository.explainError(errorLog, new AIRepository.Callback<String>() {
            @Override
            public void onSuccess(String result) {
                aiResult.setValue(new AiResultState(false, result, null, 3));
            }

            @Override
            public void onError(String error) {
                aiResult.setValue(new AiResultState(false, null, error, 3));
            }
        });
    }

    public void clearHistory() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                database.uploadHistoryDao().clearAll();
            }
        }).start();
    }
}
