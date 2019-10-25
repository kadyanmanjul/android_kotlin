package com.joshtalks.joshskills.ui.view_holders

import android.Manifest
import android.graphics.Color
import android.net.Uri
import android.view.View.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.exoplayer2.offline.Download
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.service.video_download.VideoDownloadController
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.DownloadMediaEventBus
import com.joshtalks.joshskills.repository.local.eventbus.MediaProgressEventBus
import com.joshtalks.joshskills.repository.local.eventbus.PlayVideoEvent
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.mindorks.placeholderview.annotations.*
import com.pnikosis.materialishprogress.ProgressWheel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.lang.Exception
import java.lang.ref.WeakReference


@Layout(R.layout.video_view_holder)
class VideoViewHolder(activityRef: WeakReference<FragmentActivity>, message: ChatModel) :
    BaseChatViewHolder(activityRef, message) {

    @View(R.id.image_view)
    lateinit var image_view: AppCompatImageView

    var context = AppObjectController.joshApplication


    @View(R.id.root_view)
    lateinit var root_view: FrameLayout

    @View(R.id.root_sub_view)
    lateinit var root_sub_view: FrameLayout

    @View(R.id.message_view)
    lateinit var message_view: android.view.View

    @View(R.id.text_title)
    lateinit var text_title: TextView

    @View(R.id.text_message_body)
    lateinit var text_message_body: JoshTextView


    @View(R.id.text_message_time)
    lateinit var text_message_time: AppCompatTextView


    @View(R.id.download_container)
    lateinit var download_container: FrameLayout

    @View(R.id.iv_cancel_download)
    lateinit var iv_cancel_download: AppCompatImageView

    @View(R.id.iv_start_download)
    lateinit var iv_start_download: AppCompatImageView


    @View(R.id.play_icon)
    lateinit var playIcon: android.view.View


    @View(R.id.progress_dialog)
    lateinit var progress_dialog: ProgressWheel

    @View(R.id.ll_container)
    lateinit var ll_container: LinearLayout

    lateinit var videoViewHolder: VideoViewHolder

    private val compositeDisposable = CompositeDisposable()


    @Resolve
    fun onResolved() {
        image_view.setImageResource(0)
        text_title.visibility = GONE
        text_message_body.visibility = GONE
        text_title.text = ""
        videoViewHolder = this
        text_message_body.text = ""
        download_container.visibility = INVISIBLE
        text_message_time.text = Utils.messageTimeConversion(message.created)
        message.sender?.let {
            updateView(it, root_view, root_sub_view, message_view)
        }

        if (message.url != null) {
            if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED) {
                if (AppDirectory.isFileExist(message.downloadedLocalPath!!)) {
                    Dexter.withActivity(activityRef.get())
                        .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        .withListener(object : PermissionListener {
                            override fun onPermissionGranted(response: PermissionGrantedResponse) {
                                setImageInImageView(image_view, message.downloadedLocalPath!!,
                                    Runnable {
                                        playIcon.visibility = VISIBLE
                                    })
                                download_container.visibility = GONE

                            }

                            override fun onPermissionDenied(response: PermissionDeniedResponse) {
                            }

                            override fun onPermissionRationaleShouldBeShown(
                                permission: PermissionRequest,
                                token: PermissionToken
                            ) {

                            }
                        }).check()
                } else {
                    download_container.visibility = GONE
                }

            } else if (message.downloadStatus == DOWNLOAD_STATUS.UPLOADING) {
                fileDownloadingInProgressView()
                setImageInImageView(image_view, message.downloadedLocalPath!!)
            } else {
                setVideoImageView(image_view, R.drawable.ic_file_error)
            }
        } else {
            message.question?.videoList?.getOrNull(0)?.let { videoObj ->
                when {
                    message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED -> {
                        setImageView(
                            image_view,
                            videoObj.video_image_url,
                            false
                        )
                        fileDownloadSuccess()
                    }
                    message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADING -> {
                        setImageView(image_view, videoObj.video_image_url, true)
                        videoObj.video_url?.let {
                            fileDownloadingInProgressView()
                            download(it)
                        }
                        subscribeDownloader()
                        if (message.progress == null) {
                            progress_dialog.barColor = Color.WHITE
                        } else {
                            message.progress?.let {
                                progress_dialog.resetCount()
                                updateProgress(it)
                            }
                        }
                    }
                    else -> {
                        fileNotDownloadView()
                        setImageView(image_view, videoObj.video_image_url, true)

                    }
                }
            }

            message.question?.let { question ->
                if (question.title?.isNotEmpty()!!) {
                    text_title.text = HtmlCompat.fromHtml(
                        question.title.toString(),
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                    text_title.visibility = VISIBLE
                }
                if (question.qText?.isNotEmpty()!!) {
                    text_message_body.text = HtmlCompat.fromHtml(
                        question.qText.toString(),
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                    text_message_body.visibility = VISIBLE
                }

            }
        }


        updateTime(text_message_time)
        addMessageAutoLink(text_message_body)

    }

    private fun fileDownloadSuccess() {
        download_container.visibility = GONE
        iv_start_download.visibility = GONE
        progress_dialog.visibility = GONE
        iv_cancel_download.visibility = GONE

    }

    private fun fileNotDownloadView() {
        download_container.visibility = VISIBLE
        iv_start_download.visibility = VISIBLE
        progress_dialog.visibility = GONE
        iv_cancel_download.visibility = GONE


    }

    private fun fileDownloadingInProgressView() {
        download_container.visibility = VISIBLE
        iv_start_download.visibility = GONE
        progress_dialog.visibility = VISIBLE
        iv_cancel_download.visibility = VISIBLE
    }


    private fun download(url: String) {
        AppObjectController.addVideoCallback(message.chatId)
        AppObjectController.videoDownloadTracker.download(
            message,
            Uri.parse(url),
            VideoDownloadController.getInstance().buildRenderersFactory(false)
        )
    }


    private fun setImageView(iv: AppCompatImageView, url: String, blur: Boolean) {
        if (blur) {
            setBlurImageInImageView(iv, url)
        } else {
            setImageInImageView(iv, url, Runnable {
                playIcon.visibility = VISIBLE
            })
        }

    }

    @Click(R.id.video_container_fl)
    fun onClick() {
        if (message.url != null) {
            if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED) {
                RxBus2.publish(PlayVideoEvent(message))
            }
        } else {
            message.question?.videoList?.getOrNull(0)?.let { videoObj ->
                when {
                    message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED -> {
                        RxBus2.publish(PlayVideoEvent(message))
                    }
                    else -> {
                        RxBus2.publish(DownloadMediaEventBus(this, message))
                    }
                }
            }
        }
    }


    @Click(R.id.download_container)
    fun downloadStart() {
        RxBus2.publish(DownloadMediaEventBus(this, message))
    }

    @Click(R.id.iv_cancel_download)
    fun downloadCancel() {
        message.question?.videoList?.getOrNull(0)?.video_url?.run {
            AppObjectController.videoDownloadTracker.cancelDownload(Uri.parse(this))
        }
        fileNotDownloadView()
        message.downloadStatus = DOWNLOAD_STATUS.NOT_START


    }

    @Click(R.id.iv_start_download)
    fun downloadStart1() {
        RxBus2.publish(DownloadMediaEventBus(videoViewHolder, message))
    }

    private fun updateProgress(progress: Float) {
        progress_dialog.setLinearProgress(true)
        progress_dialog.spinSpeed = 0.25f
        progress_dialog.barColor = Color.parseColor("#128C7E")
        progress_dialog.rimColor = Color.parseColor("#33128C7E")
        progress_dialog.progress = progress / 100
    }

    private fun updateProgress() {
        progress_dialog.barColor = Color.WHITE
        progress_dialog.setLinearProgress(false)
        progress_dialog.resetCount()
    }

    private fun subscribeDownloader() {
        compositeDisposable.add(
            RxBus2.listen(MediaProgressEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())

                .subscribe({
                    try {
                        if (AppObjectController.gsonMapperForLocal.fromJson(
                                it.id,
                                ChatModel::class.java
                            ).chatId == message.chatId
                        ) {

                            when {
                                Download.STATE_STOPPED == it.state -> updateProgress()
                                Download.STATE_DOWNLOADING == it.state -> {
                                    message.progress = it.progress
                                    updateProgress(it.progress)
                                }
                                Download.STATE_FAILED == it.state -> updateProgress()
                            }
                        }
                    } catch (ex: Exception) {

                    }
                }, {
                    it.printStackTrace()
                })
        )
    }

    @Recycle
    fun onRecycled() {
        compositeDisposable.clear()
    }

}