package com.joshtalks.joshskills.common.ui.payment

import com.joshtalks.joshskills.common.core.AppObjectController

class PaymentRepository {

    private val network by lazy {
        AppObjectController.commonNetworkService
    }

    suspend fun verifyPayment(orderId: String) = network.verifyPaymentV3(orderId)
}