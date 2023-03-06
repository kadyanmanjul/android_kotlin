package com.joshtalks.joshskills.repository.service

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_UNIQUE_ID
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.analytics.ParamKeys
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.repository.local.entity.VideoEngage
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.NotificationObject
import com.joshtalks.joshskills.repository.server.engage.AudioEngage
import com.joshtalks.joshskills.repository.server.engage.Graph
import com.joshtalks.joshskills.repository.server.engage.ImageEngage
import com.joshtalks.joshskills.repository.server.engage.PdfEngage
import com.joshtalks.joshskills.repository.server.engage.SharePdfEngage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber

const val VIDEO_TIME_LEAP = 6

object EngagementNetworkHelper {
    @JvmStatic
    fun engageVideoApi(videoEngage: VideoEngage) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (videoEngage.watchTime == 0L) {
                    return@launch
                }
                videoEngage.gID = PrefManager.getStringValue(USER_UNIQUE_ID)
                if (Mentor.getInstance().hasId()) {
                    videoEngage.mentorId = Mentor.getInstance().getId()
                }
                AppObjectController.appDatabase.videoEngageDao().insertVideoEngage(videoEngage)
                WorkManagerAdmin.syncEngageVideoTask()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

    }

    fun engageAudioApi(audioId: String, mediaEngageList: List<Graph>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var timeListen: Long = 0
                for (graph in mediaEngageList) {
                    timeListen += (graph.endTime - graph.startTime)
                }
                if (timeListen < 0) {
                    return@launch
                }
                val audioEngage = AudioEngage(mediaEngageList, audioId, timeListen)
                AppObjectController.chatNetworkService.engageAudio(audioEngage)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun engagePdfApi(pdfEngage: PdfEngage) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppObjectController.chatNetworkService.engagePdf(pdfEngage)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun savePdfShareImpression(pdfEngage: SharePdfEngage) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppObjectController.chatNetworkService.engagePdf(pdfEngage)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun engageImageApi(imageEngage: ImageEngage) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppObjectController.chatNetworkService.engageImage(imageEngage)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

}
