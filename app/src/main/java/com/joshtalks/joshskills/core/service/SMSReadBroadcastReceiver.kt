package com.joshtalks.joshskills.core.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.AppSignatureHelper
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.OTPReceivedEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


var MESSAGE_START_FORMAT = "<#>"
var MESSAGE_END_FORMAT = "is your OTP verification code for Josh Skills."

class SMSReadBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (intent != null) {
                    if (SmsRetriever.SMS_RETRIEVED_ACTION == intent.action) {
                        intent.extras?.run {
                            val status = this[SmsRetriever.EXTRA_STATUS] as Status
                            when (status.statusCode) {
                                CommonStatusCodes.SUCCESS -> {
                                    val message: String? =
                                        this[SmsRetriever.EXTRA_SMS_MESSAGE] as String
                                    if (message != null) {
                                        val signature =
                                            AppSignatureHelper(AppObjectController.joshApplication).appSignatures[0]

                                        var otp =
                                            message.replace(MESSAGE_START_FORMAT, EMPTY).replace(
                                                MESSAGE_END_FORMAT, EMPTY
                                            ).split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                                        otp = otp.replace(signature, EMPTY).trimStart().trimEnd()
                                            .trim()
                                        RxBus2.publish(OTPReceivedEventBus(otp))
                                    }
                                }
                                CommonStatusCodes.TIMEOUT -> {
                                }
                                else -> {

                                }
                            }
                        }


                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

}