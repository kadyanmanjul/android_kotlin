package com.joshtalks.joshskills.expertcall.viewModel

import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.common.base.BaseViewModel
import com.joshtalks.joshskills.common.constants.EXPERT_UPGRADE_CLICK
import com.joshtalks.joshskills.common.constants.GET_UPGRADE_DETAILS
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.EMPTY
import com.joshtalks.joshskills.common.core.showToast
import com.joshtalks.joshskills.common.repository.local.SkillsDatastore
import com.joshtalks.joshskills.expertcall.model.Amount
import com.joshtalks.joshskills.expertcall.repository.ExpertListRepo
import com.joshtalks.joshskills.expertcall.repository.FirstTimeAmount
import com.joshtalks.joshskills.common.util.showAppropriateMsg
import com.joshtalks.joshskills.expertcall.repository.ExpertNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

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
        eventId: String = EMPTY,
        previousPage: String = EMPTY
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestData = hashMapOf(
                    Pair("event_name", eventName),
                    Pair("expert_id", eventId),
                    Pair("previous_page", previousPage)
                )
                AppObjectController.commonNetworkService.saveMicroPaymentImpression(requestData)
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
    }

    fun upgradeExpertCall(view: View) {
        if (orderAmount != -1 && orderTestId != -1) {
            message.what = com.joshtalks.joshskills.common.constants.EXPERT_UPGRADE_CLICK
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
                        message.what = com.joshtalks.joshskills.common.constants.GET_UPGRADE_DETAILS
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
    fun saveBranchPaymentLog(orderInfoId:String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp = AppObjectController.commonNetworkService.savePaymentLog(orderInfoId)
            } catch (ex: Exception) {
                Log.e("sagar", "setSupportReason: ${ex.message}")
            }
        }
    }
}