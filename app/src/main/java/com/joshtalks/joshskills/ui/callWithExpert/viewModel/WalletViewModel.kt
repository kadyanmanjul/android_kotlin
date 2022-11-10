package com.joshtalks.joshskills.ui.callWithExpert.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.ui.callWithExpert.model.Amount
import com.joshtalks.joshskills.ui.callWithExpert.repository.ExpertListRepo
import com.joshtalks.joshskills.ui.callWithExpert.repository.db.SkillsDatastore
import com.joshtalks.joshskills.ui.callWithExpert.utils.removeRupees
import com.joshtalks.joshskills.ui.callWithExpert.utils.toRupees
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WalletViewModel(private val app: Application) : AndroidViewModel(app) {

    private val repository by lazy {
        ExpertListRepo()
    }

    private val _availableBalance = MutableLiveData<String>("₹ 0")

     var commonTestId: Int = 0

    val availableBalance: LiveData<String>
        get() = _availableBalance

     val addedAmount = MutableLiveData<String>()

    private val _availableAmount = MutableLiveData<List<Amount>>()

    val availableAmount: LiveData<List<Amount>>
        get() = _availableAmount

    private val _loading = MutableLiveData<Boolean>()

    val loading: LiveData<Boolean>
        get() = _loading

    init {
        getWalletCredits()
        getAvailableAmounts()
    }

    private fun getWalletCredits() {
        viewModelScope.launch {
            SkillsDatastore.walletAmount.collectLatest {
                _availableBalance.postValue(it.toRupees())
            }
        }
    }

    private fun getAvailableAmounts() {
        _loading.postValue(true)
        viewModelScope.launch {
            repository.walletAmounts
                .catch {
                    _loading.postValue(false)
                    showToast(app.getString(R.string.something_went_wrong))
                }
                .collectLatest {
                    _loading.postValue(false)
                    _availableAmount.postValue(it.amount_list)
                    commonTestId = it.commonTestId
                }
        }
    }

    fun updateAddedAmount(amount: String) {
        addedAmount.value = amount
    }

    fun getAmountId(): Int {
        val foundAmount = _availableAmount.value?.find { amount ->
            amount.amount == addedAmount.value!!.removeRupees().toInt()
        }
        return foundAmount?.id ?: commonTestId
    }

}