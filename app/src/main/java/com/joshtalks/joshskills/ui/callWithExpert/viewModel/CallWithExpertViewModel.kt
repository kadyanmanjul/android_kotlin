package com.joshtalks.joshskills.ui.callWithExpert.viewModel

import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.constants.EXPERT_UPGRADE_CLICK
import com.joshtalks.joshskills.constants.GET_UPGRADE_DETAILS
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.callWithExpert.model.Amount
import com.joshtalks.joshskills.ui.callWithExpert.repository.ExpertListRepo
import com.joshtalks.joshskills.ui.callWithExpert.repository.FirstTimeAmount
import com.joshtalks.joshskills.ui.callWithExpert.repository.db.SkillsDatastore
import com.joshtalks.joshskills.util.showAppropriateMsg
import io.branch.referral.util.CurrencyType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.math.BigDecimal
import java.util.HashMap
import org.json.JSONObject

class CallWithExpertViewModel : BaseViewModel() {

    private val expertListRepo by lazy {
        ExpertListRepo()
    }

    var addedAmount: Amount? = null

    private val _walletAmount = MutableLiveData<Int>(0)

    val walletAmount: LiveData<Int>
        get() = _walletAmount

    private val _creditCount = MutableLiveData<Int>(0)

    val creditCount: LiveData<Int>
        get() = _creditCount

    private val _proceedPayment = MutableLiveData<Boolean>()

    val proceedPayment: LiveData<Boolean>
        get() = _proceedPayment

    private val _isFirstAmount = MutableLiveData<FirstTimeAmount>()

    val isFirstAmount: LiveData<FirstTimeAmount>
        get() = _isFirstAmount

    private var orderAmount: Int = 0
    private var orderTestId: Int = 0

    var isPaymentInitiated = false

    init {
        getWalletCredits()
        getWalletCreditsFromNetwork()
    }

    fun getWalletCreditsFromNetwork() {
        viewModelScope.launch {
            val firstTimeAmount = expertListRepo.updateWalletBalance()
            if (firstTimeAmount.isFirstTime) {
                _isFirstAmount.postValue(firstTimeAmount)
            }
        }
    }

    fun proceedPayment() {
        _proceedPayment.value = true
    }

    fun updateAmount(amount: Amount) {
        addedAmount = amount
    }

    private fun getWalletCredits() {
        viewModelScope.launch {
            launch {
                SkillsDatastore.walletAmount.collectLatest {
                    _walletAmount.postValue(it)
                }
            }

            launch {
                SkillsDatastore.expertCredits.collectLatest {
                    _creditCount.postValue(it)
                }
            }
        }
    }

    fun saveMicroPaymentImpression(
        eventName: String,
        expert_id: String = EMPTY,
        previousPage: String = EMPTY
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestData = hashMapOf(
                    Pair("event_name", eventName),
                    Pair("expert_id", expert_id),
                    Pair("previous_page", previousPage)
                )
                AppObjectController.commonNetworkService.saveMicroPaymentImpression(requestData)
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
    }

    fun upgradeExpertCall(view: View) {
        saveMicroPaymentImpression(eventName = CLICK_UPGRADE_BUTTON, previousPage = UPGRADE_PAGE)
        if (orderAmount != -1 && orderTestId != -1) {
            message.what = EXPERT_UPGRADE_CLICK
            message.arg1 = orderAmount
            message.arg2 = orderTestId
            singleLiveEvent.value = message
        } else {
            showToast("Oops! Something went wrong")
        }
    }

    fun getExpertUpgradeDetails() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = expertListRepo.getUpgradeDetails()
                if (res.isSuccessful && res.body() != null) {
                    orderAmount = res.body()?.amount ?: -1
                    orderTestId =  res.body()?.testId ?: -1

                    withContext(Dispatchers.Main) {
                        message.what = GET_UPGRADE_DETAILS
                        message.obj = res.body()
                        singleLiveEvent.value = message
                    }
                }
            } catch (e: Exception) {
                e.showAppropriateMsg()
            }
        }
    }

    fun syncCallDuration() {
        expertListRepo.deductAmountAfterCall()
    }

    fun saveImpressionForPayment(event: String, eventData: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                expertListRepo.saveBuyPageImpression(
                    mapOf(
                        "event_name" to event,
                        "event_data" to eventData
                    )
                )
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
            }
        }
    }
    fun removeEntryFromPaymentTable(razorpayOrderId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            AppObjectController.appDatabase.paymentDao().deletePaymentEntry(razorpayOrderId)
        }
    }

    fun logPaymentEvent(data: JSONObject) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (AppObjectController.getFirebaseRemoteConfig()
                        .getBoolean(FirebaseRemoteConfigKey.TRACK_JUSPAY_LOG)
                )
                    AppObjectController.commonNetworkService.saveJuspayPaymentLog(
                        mapOf(
                            "mentor_id" to Mentor.getInstance().getId(),
                            "json" to data
                        )
                    )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun saveBranchPaymentLog(orderInfoId:String,
                             amount: BigDecimal?,
                             testId: Int = 0,
                             courseName: String = EMPTY) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val extras: HashMap<String, Any> = HashMap()
                extras["test_id"] = testId
                extras["orderinfo_id"] = orderInfoId
                extras["currency"] = CurrencyType.INR.name
                extras["amount"] = amount ?: 0.0
                extras["course_name"] = courseName
                extras["device_id"] = Utils.getDeviceId()
                extras["guest_mentor_id"] = Mentor.getInstance().getId()
                extras["payment_done_from"] = "Expert Screen"
                val resp = AppObjectController.commonNetworkService.savePaymentLog(extras)
            } catch (ex: Exception) {
                Log.e("sagar", "setSupportReason: ${ex.message}")
            }
        }
    }
}