package com.joshtalks.joshskills.premium.repository.server;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class BaseResponse<T> {
    @SerializedName("message")
    @Expose
    private String message;
    @SerializedName("response_data")
    @Expose
    private T responseData = null;
    @SerializedName("Success")
    @Expose
    private Boolean success;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getResponseData() {
        return responseData;
    }

    public void setResponseData(T responseData) {
        this.responseData = responseData;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }
}
