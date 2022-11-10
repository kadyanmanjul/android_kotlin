package com.joshtalks.joshskills.ui.callWithExpert.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.joshtalks.joshskills.ui.callWithExpert.repository.ExpertListRepo
import com.joshtalks.joshskills.ui.callWithExpert.repository.db.SkillsDatastore
import com.joshtalks.joshskills.ui.callWithExpert.utils.toRupees
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class WalletTransactionViewModel(private val app: Application) : AndroidViewModel(app) {
    private val repository by lazy {
        ExpertListRepo()
    }

    private val _availableBalance = MutableLiveData<String>("₹ 0")

    val availableBalance: LiveData<String>
        get() = _availableBalance

    val walletPaymentLogsList = flow {
        emitAll(repository.getWalletLogs().cachedIn(viewModelScope))
    }

    val walletTransactionList = flow {
        emitAll(repository.getWalletTransactions().cachedIn(viewModelScope))
    }
    init {
        getWalletAmount()
    }

    private fun getWalletAmount() {
        viewModelScope.launch {
            SkillsDatastore.walletAmount.collectLatest {
                _availableBalance.postValue(it.toRupees())
            }
        }
    }
}