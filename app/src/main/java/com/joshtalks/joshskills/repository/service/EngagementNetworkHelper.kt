package com.joshtalks.joshskills.repository.service

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.eventbus.MediaEngageEventBus
import com.joshtalks.joshskills.repository.server.engage.AudioEngage
import com.joshtalks.joshskills.repository.server.engage.ImageEngage
import com.joshtalks.joshskills.repository.server.engage.PdfEngage
import com.joshtalks.joshskills.repository.server.engage.VideoEngage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object EngagementNetworkHelper {


    fun engageVideoApi(videoEngage: VideoEngage) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val obj = AppObjectController.chatNetworkService.engageVideo(videoEngage)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

    }

    @JvmStatic
    @Synchronized
    fun engageAudioApi(mediaEngageEventBus: MediaEngageEventBus) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var timeListen: Long = 0
                for (graph in mediaEngageEventBus.list) {
                    timeListen += (graph.endTime - graph.startTime)
                }
                if (timeListen<0){
                    return@launch
                }
                val audioEngage = AudioEngage(emptyList(), mediaEngageEventBus.id, timeListen)
                AppObjectController.chatNetworkService.engageAudio(audioEngage)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun engagePdfApi(pdfEngage: PdfEngage) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val obj = AppObjectController.chatNetworkService.engagePdf(pdfEngage)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun engageImageApi(imageEngage: ImageEngage) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val obj = AppObjectController.chatNetworkService.engageImage(imageEngage)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }


}