package com.joshtalks.joshskills.ui.payment.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.ui.payment.PaymentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PaymentInProcessViewModel: ViewModel() {

    private val repository by lazy {
        PaymentRepository()
    }

    init {
        verifyPayment()
    }

    private fun verifyPayment() {
        viewModelScope.launch(Dispatchers.IO) {

        }
    }
}