package com.joshtalks.joshskills.ui.paymentManager

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.CourseData
import com.joshtalks.joshskills.repository.server.JuspayPayLoad
import com.joshtalks.joshskills.ui.callWithExpert.model.Amount
import com.joshtalks.joshskills.ui.inbox.payment_verify.Payment
import com.joshtalks.joshskills.ui.inbox.payment_verify.PaymentStatus
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.repo.BuyPageRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class PaymentManager(
    private val context: AppCompatActivity,
    private val coroutineScope: CoroutineScope,
    private val paymentGatewayListener: PaymentGatewayListener? = null
) {

    private lateinit var paymentGatewayManager: PaymentGatewayManager

    private val paymentRepository by lazy {
        BuyPageRepo()
    }

    fun initializePaymentGateway() {
        paymentGatewayManager = PaymentGatewayManager(context, paymentGatewayListener)
        paymentGatewayManager.initPaymentGateway()
    }

    fun createOrder(testId: String, mobileNumber: String, encryptedText: String) {
        coroutineScope.launch(Dispatchers.IO) {
            paymentGatewayListener?.onProcessStart()
            try {
                val data = mutableMapOf(
                    "encrypted_text" to encryptedText,
                    "gaid" to PrefManager.getStringValue(USER_UNIQUE_ID, false),
                    "mobile" to mobileNumber,
                    "test_id" to testId,
                    "mentor_id" to Mentor.getInstance().getId()
                )

                val orderDetailsResponse: Response<JuspayPayLoad> =
                    AppObjectController.signUpNetworkService.createPaymentOrderV3(data).await()
                if (orderDetailsResponse.code() == 201) {
                    val response: JuspayPayLoad = orderDetailsResponse.body()!!
                    MarketingAnalytics.initPurchaseEvent(data, response.amount, response.currency)
                    addPaymentEntry(response)
                    withContext(Dispatchers.Main) {
                        paymentGatewayManager.openPaymentGateway(response)
                    }
                } else {
                    showToast(AppObjectController.joshApplication.getString(R.string.something_went_wrong))
                }
            } catch (ex: Exception) {
                when (ex) {
                    is HttpException -> {
                        showToast(AppObjectController.joshApplication.getString(R.string.something_went_wrong))
                    }
                    is SocketTimeoutException, is UnknownHostException -> {
                        showToast(AppObjectController.joshApplication.getString(R.string.internet_not_available_msz))
                    }
                    else -> {
                        try {
                            FirebaseCrashlytics.getInstance().recordException(ex)
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    fun createForWallet(courseData: CourseData,selectedAmount: Amount) {
        coroutineScope.launch {
            try {
//                paymentGatewayListener?.onProcessStart()
                val data = mutableMapOf(
                    "encrypted_text" to (courseData.encryptedText ?: ""),
                    "gaid" to PrefManager.getStringValue(USER_UNIQUE_ID, false),
                    "mobile" to getPhoneNumberOrDefault(),
                    "test_id" to courseData.testId,
                    "mentor_id" to Mentor.getInstance().getId(),
                    "is_micro_payment" to true.toString(),
                    "wallet_amount" to selectedAmount.amount.toString()
                )

                val orderDetailsResponse: Response<JuspayPayLoad> =
                    AppObjectController.signUpNetworkService.createPaymentOrderV3(data).await()
                Log.e("sagar", "getOrderDetails: ${orderDetailsResponse.code()}")
                if (orderDetailsResponse.code() == 201) {
                    val response: JuspayPayLoad = orderDetailsResponse.body()!!
                    withContext(Dispatchers.Main) {
                        paymentGatewayManager.openPaymentGateway(response)
                    }
                    //                    MarketingAnalytics.initPurchaseEvent(data, response)
                } else {
                    paymentGatewayListener?.onWarmUpEnded(context.getString(R.string.something_went_wrong))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                paymentGatewayListener?.onWarmUpEnded(context.getString(R.string.something_went_wrong))
            }
        }
    }

    private fun addPaymentEntry(response: JuspayPayLoad) {
        CoroutineScope(Dispatchers.IO).launch {
            AppObjectController.appDatabase.paymentDao().inertPaymentEntry(
                Payment(
                    response.amount,
                    response.joshtalksOrderId,
                    response.juspayOrderId,
                    response.payload?.orderId ?: EMPTY,
                    PaymentStatus.CREATED
                )
            )
        }
    }

    fun getJustPayOrderId() = paymentGatewayManager.juspayOrderId

    fun getJuspayBackPress() = paymentGatewayManager.onBackPressHandle()

    fun getJoshTalksId() = paymentGatewayManager.joshTalksId

    fun getAmount() = paymentGatewayManager.amount

    fun getJuspayPayload() = paymentGatewayManager.juspayPayLoad

    fun makePaymentForTryAgain(orderDetails: JuspayPayLoad) = paymentGatewayManager.openPaymentGateway(orderDetails)

}