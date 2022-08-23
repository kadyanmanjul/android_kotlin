package com.joshtalks.joshskills.ui.callWithExpert.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WalletViewModel : ViewModel() {

    private val _availableBalance = MutableLiveData<String>()

    val availableBalance: LiveData<String>
        get() = _availableBalance

}