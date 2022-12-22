package com.joshtalks.joshskills.common.ui.voip.repository

import android.os.Handler
import android.os.Looper
import android.os.Message
import com.joshtalks.joshskills.common.ui.voip.new_arch.ui.viewmodels.voipLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class RepositoryHandler(private val scope: CoroutineScope) : Handler(Looper.getMainLooper()) {
    private val flowEvent = MutableSharedFlow<Message>(replay = 0)

    fun observerFlow() : SharedFlow<Message> {
        return flowEvent
    }

    override fun handleMessage(msg: Message) {
        val data = Message()
        data.copyFrom(msg)
        voipLog?.log("handleMessage: $data")
        scope.launch {
            voipLog?.log("Inside Suspend --> $data")
            flowEvent.emit(data)
        }
    }
}