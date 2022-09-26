package com.joshtalks.joshskills.repository.server

import com.google.gson.annotations.SerializedName

data class JuspayData(
    @SerializedName("juspay_sdk_payload")
    val juspaySdkPayload: OrderDetailResponseV3?
)

data class OrderDetailResponseV3(
    @SerializedName("requestId")
    val requestId: String,
    @SerializedName("service")
    val service: String,
    @SerializedName("payload")
    val payload: Payload,
)

data class Payload(
    @SerializedName("clientId")
    val clientId: String,
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("merchantId")
    val merchantId: String,
    @SerializedName("clientAuthToken")
    val clientAuthToken: String,
    @SerializedName("clientAuthTokenExpiry")
    val clientAuthTokenExpiry: String,
    @SerializedName("environment")
    val environment: String,
    @SerializedName("action")
    val action: String,
    @SerializedName("customerId")
    val customerId: String,
    @SerializedName("currency")
    val currency: String,
    @SerializedName("customerPhone")
    val customerPhone: String,
    @SerializedName("customerEmail")
    val customerEmail: String,
    @SerializedName("orderId")
    val orderId: String,
)