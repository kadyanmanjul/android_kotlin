package com.joshtalks.joshskills.ui.payment.viewModel

import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.constants.PAYMENT_FAILED
import com.joshtalks.joshskills.constants.PAYMENT_SUCCESS
import com.joshtalks.joshskills.ui.payment.PaymentRepository
import com.joshtalks.joshskills.ui.payment.model.PaymentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PaymentInProcessViewModel : BaseViewModel() {

    private val TIMER_DURATION = 30000L
    private val LOOP_INTERVAL = 4000L

    private val repository by lazy {
        PaymentRepository()
    }

    fun verifyPayment(orderId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (orderId == null) {
                delay(2000)
                sendFailed()
                return@launch
            }
            var timer = 0L
            while (timer < TIMER_DURATION) {
                try {
                    val response = repository.verifyPayment(orderId).message
                    when (response) {
                        PaymentStatus.SUCCESS.type -> {
                            sendSuccess()
                            return@launch
                        }
                        PaymentStatus.PENDING.type -> continue
                        PaymentStatus.FAILED.type -> {
                            sendFailed()
                            return@launch
                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
                timer += LOOP_INTERVAL
                delay(LOOP_INTERVAL)
            }
            sendFailed()
        }
    }

    private fun sendSuccess() {
        message.what = PAYMENT_SUCCESS
        singleLiveEvent.value = message
    }

    private fun sendFailed() {
        message.what = PAYMENT_FAILED
        singleLiveEvent.value = message
    }
}