package com.asifqureshi.gitforge.data.remote;

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String GITHUB_API_BASE_URL = "https://api.github.com/";
    private static final String GITHUB_OAUTH_BASE_URL = "https://github.com/";
    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/";

    private static GitHubApiService gitHubApiService;
    private static String cachedToken = "";

    public static synchronized GitHubApiService getGitHubApiService(final String token) {
        if (gitHubApiService == null || !cachedToken.equals(token)) {
            cachedToken = token != null ? token : "";

            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS);

            // Auth interceptor: add Bearer or token header
            clientBuilder.addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws java.io.IOException {
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder()
                            .header("Accept", "application/vnd.github.v3+json")
                            .header("User-Agent", "GitForge-Pusher-Android");

                    if (!cachedToken.isEmpty()) {
                        String authHeader = cachedToken.startsWith("ghp_") || cachedToken.startsWith("github_pat_")
                                ? "token " + cachedToken
                                : "Bearer " + cachedToken;
                        requestBuilder.header("Authorization", authHeader);
                    }

                    return chain.proceed(requestBuilder.build());
                }
            });

            // Secure Logging Interceptor (redact headers)
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
            clientBuilder.addInterceptor(logging);

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(GITHUB_API_BASE_URL)
                    .client(clientBuilder.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            gitHubApiService = retrofit.create(GitHubApiService.class);
        }
        return gitHubApiService;
    }

    public static GitHubOAuthService getOAuthService() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(GITHUB_OAUTH_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(GitHubOAuthService.class);
    }

    public static AIApiService getAIApiService(String customEndpoint) {
        String baseUrl = (customEndpoint != null && !customEndpoint.trim().isEmpty())
                ? (customEndpoint.endsWith("/") ? customEndpoint : customEndpoint + "/")
                : GEMINI_API_BASE_URL;

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(45, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(AIApiService.class);
    }
}
