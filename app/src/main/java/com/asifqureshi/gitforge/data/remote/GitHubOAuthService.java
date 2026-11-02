package com.asifqureshi.gitforge.data.remote;

import com.asifqureshi.gitforge.model.AccessTokenResponse;
import com.asifqureshi.gitforge.model.DeviceCodeResponse;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface GitHubOAuthService {

    @FormUrlEncoded
    @Headers({"Accept: application/json"})
    @POST("login/device/code")
    Call<DeviceCodeResponse> getDeviceCode(
            @Field("client_id") String clientId,
            @Field("scope") String scope
    );

    @FormUrlEncoded
    @Headers({"Accept: application/json"})
    @POST("login/oauth/access_token")
    Call<AccessTokenResponse> pollAccessToken(
            @Field("client_id") String clientId,
            @Field("device_code") String deviceCode,
            @Field("grant_type") String grantType
    );
}
