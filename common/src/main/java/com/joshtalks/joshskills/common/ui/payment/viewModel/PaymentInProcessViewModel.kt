package com.joshtalks.joshskills.common.ui.payment.viewModel

import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.common.ui.payment.PaymentRepository
import com.joshtalks.joshskills.common.ui.payment.model.PaymentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PaymentInProcessViewModel : com.joshtalks.joshskills.common.base.BaseViewModel() {

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
            delay(2000)
            timer += 2000
            while (timer < TIMER_DURATION) {
                try {
                    val response = repository.verifyPayment(orderId).message
                    when (response) {
                        PaymentStatus.SUCCESS.type -> {
                            sendSuccess()
                            return@launch
                        }
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
            sendPending()
        }
    }

    private suspend fun sendSuccess() {
        withContext(Dispatchers.Main) {
            message.what = com.joshtalks.joshskills.common.constants.PAYMENT_SUCCESS
            singleLiveEvent.value = message
        }
    }

    private suspend fun sendPending() {
        withContext(Dispatchers.Main) {
            message.what = com.joshtalks.joshskills.common.constants.PAYMENT_PENDING
            singleLiveEvent.value = message
        }
    }

    private suspend fun sendFailed() {
        withContext(Dispatchers.Main) {
            message.what = com.joshtalks.joshskills.common.constants.PAYMENT_FAILED
            singleLiveEvent.value = message
        }
    }
}