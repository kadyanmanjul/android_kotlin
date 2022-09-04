package com.joshtalks.joshskills.ui.callWithExpert.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WalletCheckoutViewModel : ViewModel() {

    private val _amountAdded = MutableLiveData<String>()

    val amountAdded: LiveData<String>
        get() = _amountAdded

    fun checkout() {
        // TODO: Redirect to Razorpay.

    }

    fun updateAddedAmount(amount: String){
        _amountAdded.value = amount
    }
}