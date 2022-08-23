package com.joshtalks.joshskills.ui.callWithExpert.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.ui.callWithExpert.repository.db.SkillsDatastore
import com.joshtalks.joshskills.ui.callWithExpert.utils.toRupees
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CallWithExpertViewModel : ViewModel() {

    private val _creditsCount = MutableLiveData<String>("₹ 0")

    val creditsCount: LiveData<String>
        get() = _creditsCount

    init {
        getWalletCredits()
    }

    private fun getWalletCredits() {
        viewModelScope.launch {
            SkillsDatastore.walletCredits.collectLatest {
                _creditsCount.postValue(it.toRupees())
            }
        }
    }

}