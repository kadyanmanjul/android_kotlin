package com.joshtalks.joshskills.ui.callWithExpert.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.ui.callWithExpert.model.Transaction
import com.joshtalks.joshskills.ui.callWithExpert.model.WalletLogs
import com.joshtalks.joshskills.ui.callWithExpert.repository.ExpertListRepo
import com.joshtalks.joshskills.ui.callWithExpert.repository.db.SkillsDatastore
import com.joshtalks.joshskills.ui.callWithExpert.utils.toRupees
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WalletTransactionViewModel(private val app: Application) : AndroidViewModel(app) {
    private val repository by lazy {
        ExpertListRepo()
    }

    private val _availableBalance = MutableLiveData<String>("₹ 0")

    val availableBalance: LiveData<String>
        get() = _availableBalance

    private val _walletTransactions = MutableLiveData<List<Transaction>>()

    val walletTransactions : LiveData<List<Transaction>>
        get() = _walletTransactions

    private val _paymentHistory = MutableLiveData<List<WalletLogs>>()

    val paymentHistory : LiveData<List<WalletLogs>>
        get() = _paymentHistory
    init {
        getWalletCredits()
        getWalletTransactions()
        getPaymentTransactions()
    }

    private fun getPaymentTransactions() {
        viewModelScope.launch {
            repository.paymentHistory
                .catch {
                    showToast(app.getString(R.string.something_went_wrong))
                }
                .collectLatest {
                    _paymentHistory.postValue(it)
                }
        }
    }

    private fun getWalletTransactions() {
        viewModelScope.launch {
            repository.walletTransaction
                .catch {
                    showToast(app.getString(R.string.something_went_wrong))
                }
                .collectLatest {
                    _walletTransactions.postValue(it)
                }
        }
    }

    private fun getWalletCredits() {
        viewModelScope.launch {
            SkillsDatastore.walletCredits.collectLatest {
                _availableBalance.postValue(it.toRupees())
            }
        }
    }
}