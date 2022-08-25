package com.joshtalks.joshskills.ui.callWithExpert.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.ui.callWithExpert.model.Amount
import com.joshtalks.joshskills.ui.callWithExpert.repository.ExpertListRepo
import com.joshtalks.joshskills.ui.callWithExpert.repository.db.SkillsDatastore
import com.joshtalks.joshskills.ui.callWithExpert.utils.toRupees
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

class CallWithExpertViewModel : ViewModel() {

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

    init {
        getWalletCredits()
        expertListRepo.updateWalletBalance()
    }

    fun proceedPayment(){
        _proceedPayment.value = true
    }

    fun updateAmount(amount: Amount){
        addedAmount = amount
    }

    private fun getWalletCredits() {
        viewModelScope.launch {
            SkillsDatastore.walletCredits.collectLatest {
                _creditsCount.postValue(it.toRupees())
            }
        }
    }

    fun saveMicroPaymentImpression(eventName: String, eventId:String = EMPTY, previousPage:String = EMPTY) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestData = hashMapOf(
                    Pair("event_name",eventName),
                    Pair("expert_id", eventId),
                    Pair("previous_page",previousPage)
                )
                AppObjectController.commonNetworkService.saveMicroPaymentImpression(requestData)
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
    }
}