package com.joshtalks.joshskills.ui.callWithExpert.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.ui.callWithExpert.model.Amount
import com.joshtalks.joshskills.ui.callWithExpert.repository.ExpertListRepo
import com.joshtalks.joshskills.ui.callWithExpert.repository.FirstTimeAmount
import com.joshtalks.joshskills.ui.callWithExpert.repository.db.SkillsDatastore
import com.joshtalks.joshskills.ui.callWithExpert.utils.toRupees
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
                _creditsCount.postValue(it.toRupees())
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

    fun paymentSuccess(isSuccess: Boolean) {
        _paymentSuccessful.postValue(isSuccess)
    }

    fun syncCallDuration(){
        Log.d("durationsync", "syncCallDuration: ")
        expertListRepo.deductAmountAfterCall()
    }
}