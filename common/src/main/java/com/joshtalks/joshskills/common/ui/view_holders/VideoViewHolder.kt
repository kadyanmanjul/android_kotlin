package com.joshtalks.joshskills.common.ui.view_holders

import android.graphics.Color
import android.net.Uri
import android.view.View.*
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.exoplayer2.offline.Download
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.*
import com.joshtalks.joshskills.common.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.common.core.analytics.AppAnalytics
import com.joshtalks.joshskills.common.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.common.core.io.AppDirectory
import com.joshtalks.joshskills.common.core.service.video_download.VideoDownloadController
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.local.entity.ChatModel
import com.joshtalks.joshskills.common.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.common.repository.local.eventbus.MediaProgressEventBus
import com.joshtalks.joshskills.common.repository.local.eventbus.PlayVideoEvent
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.mindorks.placeholderview.annotations.*
import com.pnikosis.materialishprogress.ProgressWheel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.lang.ref.WeakReference



class VideoViewHolder(
    activityRef: WeakReference<FragmentActivity>,
    message: ChatModel,
    previousMessage: ChatModel?
) :
    BaseChatViewHolder(activityRef, message, previousMessage) {

    
    lateinit var imageView: AppCompatImageView

    
    lateinit var rootView: FrameLayout

    
    lateinit var rootSubView: FrameLayout

    
    lateinit var messageView: ViewGroup

    
    lateinit var textTitle: TextView

    
    lateinit var textMessageBody: JoshTextView


    
    lateinit var textMessageTime: AppCompatTextView


    
    lateinit var downloadContainer: FrameLayout

    
    lateinit var ivCancelDownload: AppCompatImageView

    
    lateinit var ivStartDownload: AppCompatImageView


    
    lateinit var playIcon: android.view.View


    
    lateinit var progressDialog: ProgressWheel


    lateinit var videoViewHolder: VideoViewHolder

    private val compositeDisposable = CompositeDisposable()

    private lateinit var appAnalytics: AppAnalytics


    @Resolve
    override fun onViewInflated() {
        super.onViewInflated()
        imageView.setImageResource(0)
        textTitle.visibility = GONE
        textMessageBody.visibility = GONE
        textTitle.text = EMPTY
        videoViewHolder = this
        textMessageBody.text = EMPTY
        downloadContainer.visibility = INVISIBLE
        textMessageTime.text = Utils.messageTimeConversion(message.created)
        messageView.findViewById<ViewGroup>(R.id.tag_view).visibility = GONE
        message.sender?.let {
            setViewHolderBG(previousMessage?.sender, it, rootView, rootSubView, messageView)
        }
        message.parentQuestionObject?.run {
            addLinkToTagMessage(messageView, this, message.sender)
        }
        if (message.chatId.isNotEmpty() && sId == message.chatId) {
            highlightedViewForSomeTime(rootView)
        }
        appAnalytics = AppAnalytics.create(AnalyticsEvent.VIDEO_VH.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.VIDEO_ID.NAME, message.chatId)
            .addParam(
                AnalyticsEvent.VIDEO_DURATION.NAME,
                message.duration.toString()
            )
        updateTime(textMessageTime)
        addMessageAutoLink(textMessageBody)

        if (message.url != null) {
            if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED) {
                if (AppDirectory.isFileExist(message.downloadedLocalPath)) {
                    Utils.fileUrl(message.downloadedLocalPath, message.url)?.run {
                        setImageInImageView(
                            imageView, message.downloadedLocalPath!!,
                            Runnable {
                                playIcon.visibility = VISIBLE
                            })
                        downloadContainer.visibility = GONE
                    }
                } else {
                    downloadContainer.visibility = GONE
                    imageView.background =
                        ContextCompat.getDrawable(activityRef.get()!!, R.drawable.video_placeholder)
                }

            } else if (message.downloadStatus == DOWNLOAD_STATUS.UPLOADING) {
                fileDownloadingInProgressView()
                setImageInImageView(imageView, message.downloadedLocalPath!!)
            } else {
                imageView.background =
                    ContextCompat.getDrawable(activityRef.get()!!, R.drawable.video_placeholder)
//                setVideoImageView(imageView, R.drawable.ic_file_error)
            }
        } else {
            message.question?.videoList?.getOrNull(0)?.let { videoObj ->
                subscribeDownloader()
                if (videoObj.video_image_url.isEmpty()) {
                    imageView.background =
                        ContextCompat.getDrawable(activityRef.get()!!, R.drawable.video_placeholder)
                } else {
                    setImageView(imageView, videoObj.video_image_url)
                }
                when (message.downloadStatus) {
                    DOWNLOAD_STATUS.DOWNLOADED -> {
                        fileDownloadSuccess()
                    }
                    DOWNLOAD_STATUS.DOWNLOADING -> {
                        videoObj.video_url?.run {
                            fileDownloadingInProgressView()
                            download(this)
                        }
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
                    }
                }
            }

            message.question?.let { question ->
                if (question.title.isNullOrEmpty().not()) {
                    textTitle.text = HtmlCompat.fromHtml(
                        question.title!!,
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                    textTitle.visibility = VISIBLE
                }
                if (question.qText.isNullOrEmpty().not()) {
                    textMessageBody.text = HtmlCompat.fromHtml(
                        question.qText!!,
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                    textMessageBody.visibility = VISIBLE
                }

            }
        }
    }

    private fun fileDownloadSuccess() {
        appAnalytics.addParam(AnalyticsEvent.VIDEO_VIEW_STATUS.NAME, "Downloaded")
        downloadContainer.visibility = GONE
        ivStartDownload.visibility = GONE
        progressDialog.visibility = GONE
        ivCancelDownload.visibility = GONE

    }

    private fun fileNotDownloadView() {
        appAnalytics.addParam(AnalyticsEvent.VIDEO_VIEW_STATUS.NAME, "Not downloaded")
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
        if (AppObjectController.getVideoTracker()!=null) {
            AppObjectController.videoDownloadTracker.download(
                message,
                Uri.parse(url),
                VideoDownloadController.getInstance().buildRenderersFactory(true)
            )
        }
        appAnalytics.addParam(AnalyticsEvent.VIDEO_DOWNLOAD_STATUS.NAME, "Downloading")
    }


    private fun setImageView(iv: AppCompatImageView, url: String) {
        setImageInImageView(iv, url, Runnable {
            playIcon.visibility = VISIBLE
        })
    }

    
    fun onClick() {
        executeDownload()
    }

    
    fun playVideo() {
        executeDownload()
    }


    
    fun downloadStart() {
        executeDownload()
    }

    
    fun downloadStartContainer() {
        executeDownload()
    }


    private fun executeDownload() {
        if (PermissionUtils.isStoragePermissionEnabled(activityRef.get()!!).not()) {
            PermissionUtils.storageReadAndWritePermission(
                activityRef.get()!!,
                object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.areAllPermissionsGranted()?.let { flag ->
                            if (flag) {
                                videoDownload()
                                return

                            }
                            if (report.isAnyPermissionPermanentlyDenied) {
                                PermissionUtils.permissionPermanentlyDeniedDialog(
                                    activityRef.get()!!
                                )
                                return
                            }
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        token?.continuePermissionRequest()
                    }
                })
            return
        }
        videoDownload()
    }

    private fun videoDownload() {
        if (message.url != null) {
            when {
                message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED -> {
                    com.joshtalks.joshskills.common.messaging.RxBus2.publish(PlayVideoEvent(message))
                }
                AppDirectory.isFileExist(message.downloadedLocalPath).not() -> {
                    showToast(getAppContext().getString(R.string.video_url_not_exist))
                }
                else -> {
                    appAnalytics.push()
                    com.joshtalks.joshskills.common.messaging.RxBus2.publish(PlayVideoEvent(message))
                }
            }
        } else {
            appAnalytics.push()
            message.question?.videoList?.getOrNull(0)?.let { _ ->
                if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED) {
                    com.joshtalks.joshskills.common.messaging.RxBus2.publish(PlayVideoEvent(message))
                    return
                }
                if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADING) {
                    com.joshtalks.joshskills.common.messaging.RxBus2.publish(PlayVideoEvent(message))
                    return
                }
                //  RxBus2.publish(DownloadMediaEventBus(this, message))
                com.joshtalks.joshskills.common.messaging.RxBus2.publish(PlayVideoEvent(message))
            }
        }
    }


    
    fun downloadCancel() {
        appAnalytics.addParam(AnalyticsEvent.VIDEO_DOWNLOAD_STATUS.NAME, "Cancelled")
        message.question?.videoList?.getOrNull(0)?.video_url?.run {
            if (AppObjectController.getVideoTracker()!=null) {
                AppObjectController.videoDownloadTracker.cancelDownload(Uri.parse(this))
            }
        }
        fileNotDownloadView()
        message.downloadStatus = DOWNLOAD_STATUS.NOT_START
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
        progressDialog.setLinearProgress(true)
        progressDialog.resetCount()
    }

    private fun subscribeDownloader() {
        compositeDisposable.add(
            com.joshtalks.joshskills.common.messaging.RxBus2.listen(MediaProgressEventBus::class.java)
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

    override fun getRoot(): FrameLayout {
        return rootView
    }

    @Recycle
    fun onRecycled() {
        compositeDisposable.clear()
    }

}