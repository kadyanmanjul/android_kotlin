package com.joshtalks.joshskills.ui.callWithExpert.viewModel

import android.app.Application
import androidx.lifecycle.*
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.ui.callWithExpert.repository.db.SkillsDatastore
import com.joshtalks.joshskills.ui.callWithExpert.utils.toRupees
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WalletViewModel(application: Application) : AndroidViewModel(application) {

    private val _availableBalance = MutableLiveData<String>("₹ 0")

    val availableBalance: LiveData<String>
        get() = _availableBalance

    private val _addedAmount = MutableLiveData(application.getString(R.string.enter_amount_in_inr))

    val addedAmount: LiveData<String>
        get() = _addedAmount

    init {
        getWalletCredits()
    }

    private fun getWalletCredits() {
        viewModelScope.launch {
            SkillsDatastore.walletCredits.collectLatest {
                _availableBalance.postValue(it.toRupees())
            }
        }
    }

    fun updateAddedAmount(amount: String){
        _addedAmount.value = amount
    }

}