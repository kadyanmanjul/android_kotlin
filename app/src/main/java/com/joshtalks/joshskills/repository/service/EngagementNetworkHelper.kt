package com.joshtalks.joshskills.repository.service

import com.joshtalks.joshskills.core.AppObjectController
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
    fun engageAudioApi(audioEngage: AudioEngage) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                audioEngage.audioId?.let {
                    val obj = AppObjectController.chatNetworkService.engageAudio(audioEngage)
                }

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