package com.asifqureshi.gitforge.model;

import com.google.gson.annotations.SerializedName;

public class AccessTokenResponse {
    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("token_type")
    private String tokenType;

    @SerializedName("scope")
    private String scope;

    @SerializedName("error")
    private String error;

    @SerializedName("error_description")
    private String errorDescription;

    public String getAccessToken() { return accessToken; }
    public String getTokenType() { return tokenType; }
    public String getScope() { return scope; }
    public String getError() { return error; }
    public String getErrorDescription() { return errorDescription; }
}
