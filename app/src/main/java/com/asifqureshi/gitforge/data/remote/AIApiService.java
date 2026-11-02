package com.asifqureshi.gitforge.data.remote;

import com.asifqureshi.gitforge.model.AIModels;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface AIApiService {

    @POST("v1beta/models/{model}:generateContent")
    Call<AIModels.GeminiResponse> generateContent(
            @Path("model") String model,
            @Query("key") String apiKey,
            @Body AIModels.GeminiRequest request
    );
}
