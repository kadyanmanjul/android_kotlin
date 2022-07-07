package com.joshtalks.badebhaiya.liveroom.heartbeat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.badebhaiya.pubnub.PubNubManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HeartbeatViewModel @Inject constructor(
    private val heartbeatRepository: HeartbeatRepository
) : ViewModel() {

    fun initViewModel() {
        Timber.d("INITIALIZED VIEW MODEL")
    }

    init {
        startHeartbeat()
    }

    private fun startHeartbeat() {
        if (!PubNubManager.getLiveRoomProperties().isModerator) {
            viewModelScope.launch(Dispatchers.IO) {
                heartbeatRepository.heartbeat
                    .catch {
                        Timber.tag("HEARTBEAT EXCEPTION")
                    }
                    .collect {
                        Timber.tag("HEARTBEAT TRIGGERED")
                    }
            }
        }
    }
}