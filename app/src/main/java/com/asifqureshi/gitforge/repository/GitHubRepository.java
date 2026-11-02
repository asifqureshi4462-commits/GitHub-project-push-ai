package com.asifqureshi.gitforge.repository;

import android.os.Handler;
import android.os.Looper;

import com.asifqureshi.gitforge.data.local.SecurePreferences;
import com.asifqureshi.gitforge.data.remote.ApiClient;
import com.asifqureshi.gitforge.data.remote.GitHubApiService;
import com.asifqureshi.gitforge.data.remote.GitHubOAuthService;
import com.asifqureshi.gitforge.model.AccessTokenResponse;
import com.asifqureshi.gitforge.model.Branch;
import com.asifqureshi.gitforge.model.DeviceCodeResponse;
import com.asifqureshi.gitforge.model.Repository;
import com.asifqureshi.gitforge.model.User;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class GitHubRepository {
    private final SecurePreferences securePreferences;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public GitHubRepository(SecurePreferences securePreferences) {
        this.securePreferences = securePreferences;
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    private GitHubApiService getService() {
        return ApiClient.getGitHubApiService(securePreferences.getGitHubToken());
    }

    public void validateAndSaveToken(final String token, final Callback<User> callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    GitHubApiService service = ApiClient.getGitHubApiService(token);
                    Response<User> response = service.getAuthenticatedUser().execute();
                    if (response.isSuccessful() && response.body() != null) {
                        final User user = response.body();
                        securePreferences.saveGitHubToken(token);
                        securePreferences.saveUserInfo(user.getLogin(), user.getName(), user.getAvatarUrl());
                        postSuccess(callback, user);
                    } else {
                        String errMsg = "Authentication failed: " + response.code() + " " + response.message();
                        if (response.code() == 401) {
                            errMsg = "Invalid GitHub token. Please ensure it has not expired and has 'repo' permissions.";
                        }
                        postError(callback, errMsg);
                    }
                } catch (Exception e) {
                    postError(callback, "Connection error: " + e.getMessage());
                }
            }
        });
    }

    public void fetchCurrentUser(final Callback<User> callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Response<User> response = getService().getAuthenticatedUser().execute();
                    if (response.isSuccessful() && response.body() != null) {
                        User user = response.body();
                        securePreferences.saveUserInfo(user.getLogin(), user.getName(), user.getAvatarUrl());
                        postSuccess(callback, user);
                    } else {
                        postError(callback, "Failed to load user: " + response.message());
                    }
                } catch (Exception e) {
                    postError(callback, "Error fetching user profile: " + e.getMessage());
                }
            }
        });
    }

    public void fetchRepositories(final Callback<List<Repository>> callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Response<List<Repository>> response = getService().getUserRepositories("updated", 100, 1).execute();
                    if (response.isSuccessful() && response.body() != null) {
                        postSuccess(callback, response.body());
                    } else {
                        postError(callback, "Failed to load repositories: " + response.message());
                    }
                } catch (Exception e) {
                    postError(callback, "Error: " + e.getMessage());
                }
            }
        });
    }

    public void fetchBranches(final String owner, final String repo, final Callback<List<Branch>> callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Response<List<Branch>> response = getService().getBranches(owner, repo, 100).execute();
                    if (response.isSuccessful() && response.body() != null) {
                        postSuccess(callback, response.body());
                    } else {
                        postError(callback, "Failed to load branches: " + response.message());
                    }
                } catch (Exception e) {
                    postError(callback, "Error fetching branches: " + e.getMessage());
                }
            }
        });
    }

    public void startDeviceOAuth(final String clientId, final Callback<DeviceCodeResponse> callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    GitHubOAuthService service = ApiClient.getOAuthService();
                    Response<DeviceCodeResponse> response = service.getDeviceCode(clientId, "repo,read:user").execute();
                    if (response.isSuccessful() && response.body() != null) {
                        postSuccess(callback, response.body());
                    } else {
                        postError(callback, "Device flow error: " + response.message());
                    }
                } catch (Exception e) {
                    postError(callback, "OAuth error: " + e.getMessage());
                }
            }
        });
    }

    public void pollDeviceToken(final String clientId, final String deviceCode, final Callback<AccessTokenResponse> callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    GitHubOAuthService service = ApiClient.getOAuthService();
                    Response<AccessTokenResponse> response = service.pollAccessToken(clientId, deviceCode, "urn:ietf:params:oauth:grant-type:device_code").execute();
                    if (response.isSuccessful() && response.body() != null) {
                        postSuccess(callback, response.body());
                    } else {
                        postError(callback, "Polling error: " + response.message());
                    }
                } catch (Exception e) {
                    postError(callback, "Polling error: " + e.getMessage());
                }
            }
        });
    }

    private <T> void postSuccess(final Callback<T> callback, final T result) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) callback.onSuccess(result);
            }
        });
    }

    private <T> void postError(final Callback<T> callback, final String message) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) callback.onError(message);
            }
        });
    }
}
