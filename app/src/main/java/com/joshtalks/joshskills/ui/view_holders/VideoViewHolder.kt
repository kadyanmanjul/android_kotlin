package com.joshtalks.joshskills.ui.view_holders

import android.Manifest
import android.graphics.Color
import android.net.Uri
import android.view.View.*
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.exoplayer2.offline.Download
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.MINIMUM_VIDEO_DOWNLOAD_PROGRESS
import com.joshtalks.joshskills.core.Utils
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
    lateinit var imageView: AppCompatImageView

    var context = AppObjectController.joshApplication


    @View(R.id.root_view)
    lateinit var rootView: FrameLayout

    @View(R.id.root_sub_view)
    lateinit var rootSubView: FrameLayout

    @View(R.id.message_view)
    lateinit var messageView: android.view.View

    @View(R.id.text_title)
    lateinit var textTitle: TextView

    @View(R.id.text_message_body)
    lateinit var textMessageBody: JoshTextView


    @View(R.id.text_message_time)
    lateinit var textMessageTime: AppCompatTextView


    @View(R.id.download_container)
    lateinit var downloadContainer: FrameLayout

    @View(R.id.iv_cancel_download)
    lateinit var ivCancelDownload: AppCompatImageView

    @View(R.id.iv_start_download)
    lateinit var ivStartDownload: AppCompatImageView


    @View(R.id.play_icon)
    lateinit var playIcon: android.view.View


    @View(R.id.progress_dialog)
    lateinit var progressDialog: ProgressWheel

    lateinit var videoViewHolder: VideoViewHolder

    private val compositeDisposable = CompositeDisposable()


    @Resolve
    fun onResolved() {
        imageView.setImageResource(0)
        textTitle.visibility = GONE
        textMessageBody.visibility = GONE
        textTitle.text = EMPTY
        videoViewHolder = this
        textMessageBody.text = EMPTY
        downloadContainer.visibility = INVISIBLE
        textMessageTime.text = Utils.messageTimeConversion(message.created)
        message.sender?.let {
            updateView(it, rootView, rootSubView, messageView)
        }

        if (message.url != null) {
            if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED) {
                if (AppDirectory.isFileExist(message.downloadedLocalPath!!)) {
                    Dexter.withActivity(activityRef.get())
                        .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        .withListener(object : PermissionListener {
                            override fun onPermissionGranted(response: PermissionGrantedResponse) {
                                setImageInImageView(imageView, message.downloadedLocalPath!!,
                                    Runnable {
                                        playIcon.visibility = VISIBLE
                                    })
                                downloadContainer.visibility = GONE

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
                    downloadContainer.visibility = GONE
                }

            } else if (message.downloadStatus == DOWNLOAD_STATUS.UPLOADING) {
                fileDownloadingInProgressView()
                setImageInImageView(imageView, message.downloadedLocalPath!!)
            } else {
                setVideoImageView(imageView, R.drawable.ic_file_error)
            }
        } else {
            message.question?.videoList?.getOrNull(0)?.let { videoObj ->
                when (message.downloadStatus) {
                    DOWNLOAD_STATUS.DOWNLOADED -> {
                        setImageView(
                            imageView,
                            videoObj.video_image_url,
                            false
                        )
                        fileDownloadSuccess()
                    }
                    DOWNLOAD_STATUS.DOWNLOADING -> {
                        setImageView(imageView, videoObj.video_image_url, true)
                        videoObj.video_url?.let {
                            fileDownloadingInProgressView()
                            download(it)
                        }
                        subscribeDownloader()
                        if (message.progress == 0) {
                            progressDialog.barColor = Color.WHITE
                        } else {
                            message.progress.let {
                                progressDialog.resetCount()
                                updateProgress(it)
                            }
                        }
                    }
                    else -> {
                        fileNotDownloadView()
                        setImageView(imageView, videoObj.video_image_url, true)

                    }
                }
            }

            message.question?.let { question ->
                if (question.title?.isNotEmpty()!!) {
                    textTitle.text = HtmlCompat.fromHtml(
                        question.title.toString(),
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                    textTitle.visibility = VISIBLE
                }
                if (question.qText?.isNotEmpty()!!) {
                    textMessageBody.text = HtmlCompat.fromHtml(
                        question.qText.toString(),
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                    textMessageBody.visibility = VISIBLE
                }

            }
        }


        updateTime(textMessageTime)
        addMessageAutoLink(textMessageBody)

    }

    private fun fileDownloadSuccess() {
        downloadContainer.visibility = GONE
        ivStartDownload.visibility = GONE
        progressDialog.visibility = GONE
        ivCancelDownload.visibility = GONE

    }

    private fun fileNotDownloadView() {
        downloadContainer.visibility = VISIBLE
        ivStartDownload.visibility = VISIBLE
        progressDialog.visibility = GONE
        ivCancelDownload.visibility = GONE


    }

    private fun fileDownloadingInProgressView() {
        downloadContainer.visibility = VISIBLE
        ivStartDownload.visibility = GONE
        progressDialog.visibility = VISIBLE
        ivCancelDownload.visibility = VISIBLE
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
            message.question?.videoList?.getOrNull(0)?.let { _ ->
                when (message.downloadStatus) {
                    DOWNLOAD_STATUS.DOWNLOADED -> {
                        RxBus2.publish(PlayVideoEvent(message))
                    }
                    else -> {
                        if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADING && message.progress > MINIMUM_VIDEO_DOWNLOAD_PROGRESS) {
                            RxBus2.publish(PlayVideoEvent(message))
                            return
                        }
                        RxBus2.publish(DownloadMediaEventBus(this, message))
                    }
                }
            }
        }
    }


    @Click(R.id.download_container)
    fun downloadStart() {
        if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADING && message.progress > MINIMUM_VIDEO_DOWNLOAD_PROGRESS) {
            RxBus2.publish(PlayVideoEvent(message))
            return
        }
        RxBus2.publish(DownloadMediaEventBus(this, message))
    }

    @Click(R.id.download_container)
    fun downloadCancel() {
        message.question?.videoList?.getOrNull(0)?.video_url?.run {
            AppObjectController.videoDownloadTracker.cancelDownload(Uri.parse(this))
        }
        fileNotDownloadView()
        message.downloadStatus = DOWNLOAD_STATUS.NOT_START


    }

    @Click(R.id.download_container)
    fun downloadStart1() {
        if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADING && message.progress > MINIMUM_VIDEO_DOWNLOAD_PROGRESS) {
            RxBus2.publish(PlayVideoEvent(message))
            return
        }
        RxBus2.publish(DownloadMediaEventBus(videoViewHolder, message))
    }

    private fun updateProgress(progress: Int) {
        progressDialog.setLinearProgress(true)
        progressDialog.spinSpeed = 0.25f
        progressDialog.barColor = Color.parseColor("#00ACFF")
        progressDialog.rimColor = Color.parseColor("#3300ACFF")
        progressDialog.progress = progress.toFloat() / 100
    }

    private fun updateProgress() {
        progressDialog.barColor = Color.WHITE
        progressDialog.setLinearProgress(false)
        progressDialog.resetCount()
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
                                    message.progress = it.progress.toInt()
                                    updateProgress(it.progress.toInt())
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