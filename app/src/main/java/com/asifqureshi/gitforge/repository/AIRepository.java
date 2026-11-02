package com.asifqureshi.gitforge.repository;

import android.os.Handler;
import android.os.Looper;

import com.asifqureshi.gitforge.data.local.SecurePreferences;
import com.asifqureshi.gitforge.data.remote.AIApiService;
import com.asifqureshi.gitforge.data.remote.ApiClient;
import com.asifqureshi.gitforge.model.AIModels;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class AIRepository {
    private final SecurePreferences securePreferences;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public AIRepository(SecurePreferences securePreferences) {
        this.securePreferences = securePreferences;
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void generateReadme(final String projectSummary, final Callback<String> callback) {
        String prompt = "You are a principal software architect. Generate an exemplary, production-grade GitHub README.md in Markdown format for the following project details and file structure:\n\n"
                + projectSummary + "\n\n"
                + "Include:\n"
                + "1. Project Title & Tagline\n"
                + "2. Key Features (bulleted list)\n"
                + "3. Architecture & Tech Stack\n"
                + "4. Prerequisites & Installation Guide\n"
                + "5. Usage Examples\n"
                + "6. Environment Variables / Configuration\n"
                + "7. Contributing & License\n\n"
                + "Output ONLY the pristine Markdown content.";

        executeGeminiCall(prompt, callback);
    }

    public void reviewCode(final String codeSnippet, final Callback<String> callback) {
        String prompt = "You are a Staff Security and Quality Code Reviewer. Conduct an in-depth code review of the following source code:\n\n"
                + "```\n" + codeSnippet + "\n```\n\n"
                + "Provide actionable feedback divided into:\n"
                + "1. Executive Summary\n"
                + "2. Security Vulnerabilities & Edge Cases\n"
                + "3. Performance & Memory Bottlenecks\n"
                + "4. Code Smells & Architectural Best Practices\n"
                + "5. Refactored Code Snippet\n\n"
                + "Use clean Markdown with code blocks.";

        executeGeminiCall(prompt, callback);
    }

    public void explainError(final String errorLog, final Callback<String> callback) {
        String prompt = "You are an expert DevOps engineer and debugging assistant. Analyze this Git, build, or runtime error:\n\n"
                + "```\n" + errorLog + "\n```\n\n"
                + "Explain:\n"
                + "1. Root Cause Analysis (why did this happen?)\n"
                + "2. Exact Step-by-Step Fix (terminal commands or code adjustments)\n"
                + "3. Prevention Tips for the future.\n\n"
                + "Format clearly in Markdown.";

        executeGeminiCall(prompt, callback);
    }

    private void executeGeminiCall(final String prompt, final Callback<String> callback) {
        final String apiKey = securePreferences.getAiApiKey();
        final String model = securePreferences.getAiModel();
        final String endpoint = securePreferences.getAiEndpoint();

        // Enforce requirement: "AI features must not run without valid provider configuration. Never generate fake AI responses."
        if (apiKey == null || apiKey.trim().isEmpty()) {
            postError(callback, "AI Provider not configured. Please enter your Gemini API Key in Settings to use AI tools.\n(Note: For production, a secure backend proxy is recommended).");
            return;
        }

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    AIApiService service = ApiClient.getAIApiService(endpoint);
                    AIModels.GeminiRequest req = new AIModels.GeminiRequest(prompt);
                    Response<AIModels.GeminiResponse> resp = service.generateContent(model, apiKey, req).execute();

                    if (resp.isSuccessful() && resp.body() != null) {
                        String text = resp.body().getFirstCandidateText();
                        if (text != null && !text.isEmpty()) {
                            postSuccess(callback, text);
                        } else {
                            postError(callback, "AI returned an empty response. Check model filters.");
                        }
                    } else {
                        String err = "AI Request failed (" + resp.code() + "): " + resp.message();
                        if (resp.code() == 400 || resp.code() == 403) {
                            err = "Invalid API Key or unauthorized request to AI provider (" + resp.code() + "). Please verify in Settings.";
                        }
                        postError(callback, err);
                    }
                } catch (Exception e) {
                    postError(callback, "Network error calling AI service: " + e.getMessage());
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
