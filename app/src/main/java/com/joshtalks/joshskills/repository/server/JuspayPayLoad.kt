package com.joshtalks.joshskills.repository.server

import com.google.gson.annotations.SerializedName


data class JuspayPayLoad(
    @SerializedName("juspay_order_id") var juspayOrderId: String? = null,
    @SerializedName("requestId") var requestId: String? = null,
    @SerializedName("service") var service: String? = null,
    @SerializedName("payload") var payload: PayloadData? = null,
    @SerializedName("joshtalks_order_id") var joshtalksOrderId: Int? = null,
    @SerializedName("amount") var amount: Int? = null,
    @SerializedName("currency") var currency: String? = null,
    @SerializedName("email") var email: String? = null
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