package com.joshtalks.joshskills.ui.view_holders

import android.view.Gravity
//import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.audioplayer.view.JcPlayerView
import com.joshtalks.joshskills.core.interfaces.AudioPlayerInterface
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.service.DownloadUtils
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.eventbus.DownloadCompletedEventBus
import com.joshtalks.joshskills.repository.local.eventbus.DownloadMediaEventBus
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2core.DownloadBlock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

@Layout(R.layout.audio_player_view)
class AudioPlayerViewHolder(activityRef: WeakReference<FragmentActivity>, message: ChatModel) :
    BaseChatViewHolder(activityRef, message) {

    @View(R.id.audio_view)
    lateinit var audio_view: JcPlayerView

    @View(R.id.root_view)
    lateinit var root_view: RelativeLayout

    @View(R.id.audio_view_sent)
    lateinit var audioViewSent: android.view.View

    @View(R.id.audio_view_received)
    lateinit var audioViewReceived: android.view.View

    @View(R.id.profile_image)
    lateinit var profileImage: AppCompatImageView
    lateinit var audioPlayerViewHolder: AudioPlayerViewHolder


    private var downloadListener = object : FetchListener {
        override fun onAdded(download: Download) {

        }

        override fun onCancelled(download: Download) {

        }

        override fun onCompleted(download: Download) {
            AppAnalytics.create(AnalyticsEvent.AUDIO_DOWNLOAD.NAME)
                .addParam("ChatId", message.chatId).push()

            DownloadUtils.removeCallbackListener(download.tag)
            CoroutineScope(Dispatchers.IO).launch {
                DownloadUtils.updateDownloadStatus(download.file, download.extras).let {
                    RxBus2.publish(DownloadCompletedEventBus(audioPlayerViewHolder, message))
                }
            }


        }

        override fun onDeleted(download: Download) {

        }

        override fun onDownloadBlockUpdated(
            download: Download,
            downloadBlock: DownloadBlock,
            totalBlocks: Int
        ) {

        }

        override fun onError(download: Download, error: Error, throwable: Throwable?) {

        }

        override fun onPaused(download: Download) {

        }

        override fun onProgress(
            download: Download,
            etaInMilliSeconds: Long,
            downloadedBytesPerSecond: Long
        ) {

        }

        override fun onQueued(download: Download, waitingOnNetwork: Boolean) {

        }

        override fun onRemoved(download: Download) {

        }

        override fun onResumed(download: Download) {

        }

        override fun onStarted(
            download: Download,
            downloadBlocks: List<DownloadBlock>,
            totalBlocks: Int
        ) {

        }

        override fun onWaitingNetwork(download: Download) {

        }

    }


    @Resolve
    fun onResolved() {
        profileImage.setImageResource(R.drawable.ic_user_rec_placeholder)
        audioViewSent.visibility = android.view.View.GONE
        audioViewReceived.visibility = android.view.View.GONE
        this.audioPlayerViewHolder = this
        message.sender?.let {
            if (it.id.equals(getUserId(), ignoreCase = true)) {

                val params = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 80f), 0, com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 7f), 0)
                params.gravity = Gravity.END
                root_view.layoutParams = params
                root_view.setBackgroundResource(R.drawable.balloon_outgoing_normal)
                audioViewSent.visibility = android.view.View.VISIBLE
            } else {
                val params = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.gravity = Gravity.START
                params.setMargins(com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 7f), 0, com.vanniktech.emoji.Utils.dpToPx(getAppContext(), 80f), 0)
                root_view.layoutParams = params
                root_view.setBackgroundResource(R.drawable.balloon_incoming_normal)
                audioViewReceived.visibility = android.view.View.VISIBLE
            }
            it.user?.photo_url?.run {
               // setUrlInImageView(profileImage,this)
            }
        }


        audio_view.prepareAudioPlayer(activityRef.get(), message, object : AudioPlayerInterface {
            override fun downloadInQueue() {
                RxBus2.publish(DownloadMediaEventBus(audioPlayerViewHolder, message))
            }

            override fun downloadStart(url: String) {
                DownloadUtils.downloadFile(
                    url,
                    AppDirectory.recordingReceivedFile(url).absolutePath,
                    message.chatId,
                    message,
                    downloadListener
                )
            }

            override fun downloadStop() {

            }

        })

    }


}