package com.joshtalks.joshskills.ui.callWithExpert.viewModel

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.constants.EXPERT_UPGRADE_CLICK
import com.joshtalks.joshskills.constants.GET_UPGRADE_DETAILS
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.ui.callWithExpert.model.Amount
import com.joshtalks.joshskills.ui.callWithExpert.repository.ExpertListRepo
import com.joshtalks.joshskills.ui.callWithExpert.repository.FirstTimeAmount
import com.joshtalks.joshskills.ui.callWithExpert.repository.db.SkillsDatastore
import com.joshtalks.joshskills.util.showAppropriateMsg
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

    private val _creditsCount = MutableLiveData<String>("₹ 0")

    val creditsCount: LiveData<String>
        get() = _creditsCount

    private val _proceedPayment = MutableLiveData<Boolean>()

    val proceedPayment: LiveData<Boolean>
        get() = _proceedPayment

    private val _isFirstAmount = MutableLiveData<FirstTimeAmount>()

    val isFirstAmount: LiveData<FirstTimeAmount>
        get() = _isFirstAmount

    private val _paymentSuccessful = MutableLiveData<Boolean>()

    val paymentSuccessful: LiveData<Boolean>
        get() = _paymentSuccessful

    private var orderAmount: Int = 0
    private var orderTestId: Int = 0

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
            SkillsDatastore.walletCredits.collectLatest {
                _creditsCount.postValue(it.toString())
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

    fun paymentSuccess(isSuccess: Boolean) {
        _paymentSuccessful.postValue(isSuccess)
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
}