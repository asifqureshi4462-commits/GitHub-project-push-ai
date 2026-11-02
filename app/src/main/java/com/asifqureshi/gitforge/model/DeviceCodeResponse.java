package com.asifqureshi.gitforge.model;

import com.google.gson.annotations.SerializedName;

public class DeviceCodeResponse {
    @SerializedName("device_code")
    private String deviceCode;

    @SerializedName("user_code")
    private String userCode;

    @SerializedName("verification_uri")
    private String verificationUri;

    @SerializedName("expires_in")
    private int expiresIn;

    @SerializedName("interval")
    private int interval;

    public String getDeviceCode() { return deviceCode != null ? deviceCode : ""; }
    public String getUserCode() { return userCode != null ? userCode : ""; }
    public String getVerificationUri() {
        return (verificationUri != null && !verificationUri.isEmpty()) ? verificationUri : "https://github.com/login/device";
    }
    public int getExpiresIn() { return expiresIn; }
    public int getInterval() { return interval > 0 ? interval : 5; }
}
