package com.joshtalks.joshskills.ui.paymentManager

import `in`.juspay.hypersdk.data.JuspayResponseHandler
import `in`.juspay.hypersdk.ui.HyperPaymentsCallbackAdapter
import `in`.juspay.services.HyperServices
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.repository.server.JuspayPayLoad
import org.json.JSONObject
import java.util.*

class PaymentGatewayManager(
    private val context: AppCompatActivity,
    private val paymentGatewayListener: PaymentGatewayListener? = null
) {

    val hyperInstance by lazy { HyperServices(context) }

    var juspayOrderId = EMPTY
    var backPressHandled = false
    var joshTalksId = 0
    var amount : Double = 0.0

    fun initPaymentGateway() {
        val payload = createInitiatePayload()
        Log.e("sagar", "initPaymentGateway:$payload ", )
        hyperInstance.initiate(payload, object : HyperPaymentsCallbackAdapter() {
            override fun onEvent(data: JSONObject, handler: JuspayResponseHandler?) {
                Log.e("sagar", "onEvent1122: $data")
                try {
                    when (data.getString("event")) {
                        "show_loader" -> {

                        }
                        "hide_loader" -> {
                            paymentGatewayListener?.onProcessStop()
                        }
                        "process_result" -> {
                            val response = data.optJSONObject("payload") as JSONObject
                            val error = data.getBoolean("error")
                            val status = response.getString("status")

                            Log.e("sagar", "onEvent: 123 $error $status")
                            if (!error) {
                                when (status) {
                                    "charged" -> {
                                        Log.e("sagar", "onEvent11: $status $response")
                                        paymentGatewayListener?.onPaymentSuccess()
                                    }
                                    "cod_initiated" -> {
                                        Log.e("sagar", "onEvent: $status")
                                    }
                                }
                            } else {
                                when (status) {
                                    "backpressed" -> {
                                        paymentGatewayListener?.onPaymentError(status)
                                    }
                                    "user_aborted" -> {
                                        paymentGatewayListener?.onPaymentError(status)
                                    }
                                    "pending_vbv" -> {
                                       paymentGatewayListener?.onPaymentError(status)
                                    }
                                    "authorizing" -> {

                                    }
                                    "authorization_failed" -> {
                                        paymentGatewayListener?.onPaymentError(status)
                                    }
                                    "authentication_failed" -> {
                                        paymentGatewayListener?.onPaymentError(status)
                                    }
                                    "api_failure" -> {
                                        paymentGatewayListener?.onPaymentError(status)
                                    }
                                    "no internet" -> {
                                        paymentGatewayListener?.onPaymentError(status)
                                    }
                                }
                            }
                            // block:end:handle-process-result
                        }
                    }
                } catch (e: Exception) {
                    // merchant code...
                    e.printStackTrace()
                }
            }
        })
    }

    private fun createInitiatePayload(): JSONObject {
        val sdkPayload = JSONObject()
        val innerPayload = JSONObject()
        try {
            // generating inner payload
            innerPayload.put("action", "initiate")
            innerPayload.put("merchantId", "joshtalks")   //Your Merchant ID here
            innerPayload.put("clientId", "joshtalks")       //Your Client ID here
            innerPayload.put("environment", "production")
            sdkPayload.put("requestId", "" + UUID.randomUUID())
            sdkPayload.put("service", "in.juspay.hyperpay")
            sdkPayload.put("payload", innerPayload)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return sdkPayload
    }

    fun openPaymentGateway(orderDetails: JuspayPayLoad) {
        Log.e("sagar", "initializeJuspayPayment:1 $orderDetails")
        try {
            val payload = JSONObject()
            val sdkPayload = JSONObject()
            sdkPayload.put("action", orderDetails.payload?.action)
            sdkPayload.put("amount", orderDetails.payload?.amount)
            sdkPayload.put("orderId", orderDetails.payload?.orderId)
            sdkPayload.put("customerId", orderDetails.payload?.customerId)
            sdkPayload.put("customerEmail", orderDetails.payload?.customerEmail)
            sdkPayload.put("currency", orderDetails.payload?.currency)
            sdkPayload.put("environment", orderDetails.payload?.environment)
            sdkPayload.put("merchantId", orderDetails.payload?.merchantId)
            sdkPayload.put("clientId", orderDetails.payload?.clientId)
            sdkPayload.put("clientAuthToken", orderDetails.payload?.clientAuthToken)
            sdkPayload.put("clientAuthTokenExpiry", orderDetails.payload?.clientAuthTokenExpiry)
            sdkPayload.put("customerPhone", orderDetails.payload?.customerPhone)

            payload.put("requestId", orderDetails.requestId)
            payload.put("service", orderDetails.service)
            payload.put("payload", sdkPayload)

            Log.e("sagar", "openPaymentGateway: $payload" )

            juspayOrderId = orderDetails.payload?.orderId ?: EMPTY
            joshTalksId = orderDetails.joshtalksOrderId
            amount = orderDetails.amount
//            paymentGatewayListener?.onWarmUpEnded()
            hyperInstance.process(payload)
        } catch (e: Exception) {
            Log.e("sagar", "initializJuspayPayment:2 ${e.message}")
            e.printStackTrace()
        }
    }

    fun onBackPressHandle() =  hyperInstance.onBackPressed()

}

interface PaymentGatewayListener {
    fun onPaymentError(errorMsg: String)
    fun onWarmUpEnded(error: String? = null)
    fun onPaymentSuccess()
    fun onProcessStart()
    fun onProcessStop()
    fun onPaymentFinished(isPaymentSuccessful: Boolean)
}