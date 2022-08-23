package com.joshtalks.joshskills.ui.callWithExpert.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.joshtalks.joshskills.R

class WalletViewModel(application: Application) : AndroidViewModel(application) {

    private val _availableBalance = MutableLiveData<String>()

    val availableBalance: LiveData<String>
        get() = _availableBalance

    private val _addedAmount = MutableLiveData(application.getString(R.string.enter_amount_in_inr))

    val addedAmount: LiveData<String>
        get() = _addedAmount



}