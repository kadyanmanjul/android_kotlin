package com.joshtalks.joshskills.common.repository.server

import com.google.gson.annotations.SerializedName


data class JuspayPayLoad(
    @SerializedName("juspay_order_id") val juspayOrderId: String,
    @SerializedName("requestId") val requestId: String? = null,
    @SerializedName("service") val service: String? = null,
    @SerializedName("payload") val payload: PayloadData? = null,
    @SerializedName("joshtalks_order_id") val joshtalksOrderId: Int,
    @SerializedName("amount") val amount: Double,
    @SerializedName("currency") val currency: String,
    @SerializedName("email") val email: String
)

data class PayloadData(
    @SerializedName("clientId") var clientId: String? = null,
    @SerializedName("amount") var amount: String? = null,
    @SerializedName("merchantId") var merchantId: String? = null,
    @SerializedName("clientAuthToken") var clientAuthToken: String? = null,
    @SerializedName("clientAuthTokenExpiry") var clientAuthTokenExpiry: String? = null,
    @SerializedName("environment") var environment: String? = null,
    @SerializedName("action") var action: String? = null,
    @SerializedName("customerId") var customerId: String? = null,
    @SerializedName("currency") var currency: String? = null,
    @SerializedName("customerPhone") var customerPhone: String? = null,
    @SerializedName("customerEmail") var customerEmail: String? = null,
    @SerializedName("orderId") var orderId: String? = null
)