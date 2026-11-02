package com.asifqureshi.gitforge.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public class SecurePreferences {
    private static final String TAG = "SecurePreferences";
    private static final String PREF_NAME = "gitforge_secure_prefs";

    private static final String KEY_GITHUB_TOKEN = "github_token";
    private static final String KEY_USER_LOGIN = "user_login";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_AVATAR = "user_avatar";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private static final String KEY_APP_THEME = "app_theme";
    private static final String KEY_AI_API_KEY = "ai_api_key";
    private static final String KEY_AI_MODEL = "ai_model";
    private static final String KEY_AI_ENDPOINT = "ai_endpoint";

    private SharedPreferences sharedPreferences;

    public SecurePreferences(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            sharedPreferences = EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize EncryptedSharedPreferences, fallback to standard prefs", e);
            sharedPreferences = context.getSharedPreferences(PREF_NAME + "_fallback", Context.MODE_PRIVATE);
        }
    }

    // GitHub Token
    public void saveGitHubToken(String token) {
        sharedPreferences.edit().putString(KEY_GITHUB_TOKEN, token).apply();
    }

    public String getGitHubToken() {
        return sharedPreferences.getString(KEY_GITHUB_TOKEN, "");
    }

    public boolean hasValidToken() {
        String token = getGitHubToken();
        return token != null && !token.trim().isEmpty();
    }

    // User Info
    public void saveUserInfo(String login, String name, String avatarUrl) {
        sharedPreferences.edit()
                .putString(KEY_USER_LOGIN, login)
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_AVATAR, avatarUrl)
                .apply();
    }

    public String getUserLogin() {
        return sharedPreferences.getString(KEY_USER_LOGIN, "");
    }

    public String getUserName() {
        return sharedPreferences.getString(KEY_USER_NAME, "");
    }

    public String getUserAvatar() {
        return sharedPreferences.getString(KEY_USER_AVATAR, "");
    }

    public void clearAuth() {
        sharedPreferences.edit()
                .remove(KEY_GITHUB_TOKEN)
                .remove(KEY_USER_LOGIN)
                .remove(KEY_USER_NAME)
                .remove(KEY_USER_AVATAR)
                .apply();
    }

    // Onboarding
    public boolean isOnboardingCompleted() {
        return sharedPreferences.getBoolean(KEY_ONBOARDING_DONE, false);
    }

    public void setOnboardingCompleted(boolean completed) {
        sharedPreferences.edit().putBoolean(KEY_ONBOARDING_DONE, completed).apply();
    }

    // Theme (0: SYSTEM, 1: LIGHT, 2: DARK)
    public int getThemeMode() {
        return sharedPreferences.getInt(KEY_APP_THEME, 0);
    }

    public void setThemeMode(int mode) {
        sharedPreferences.edit().putInt(KEY_APP_THEME, mode).apply();
    }

    // AI Configuration
    public void saveAiApiKey(String apiKey) {
        sharedPreferences.edit().putString(KEY_AI_API_KEY, apiKey).apply();
    }

    public String getAiApiKey() {
        return sharedPreferences.getString(KEY_AI_API_KEY, "");
    }

    public void saveAiModel(String model) {
        sharedPreferences.edit().putString(KEY_AI_MODEL, model).apply();
    }

    public String getAiModel() {
        return sharedPreferences.getString(KEY_AI_MODEL, "gemini-1.5-flash");
    }

    public void saveAiEndpoint(String endpoint) {
        sharedPreferences.edit().putString(KEY_AI_ENDPOINT, endpoint).apply();
    }

    public String getAiEndpoint() {
        return sharedPreferences.getString(KEY_AI_ENDPOINT, "https://generativelanguage.googleapis.com/");
    }
}
