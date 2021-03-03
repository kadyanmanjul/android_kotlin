package com.joshtalks.joshskills.ui.chat.vh

import android.graphics.Color
import android.net.Uri
import android.view.View
import android.view.View.VISIBLE
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.exoplayer2.offline.Download
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.ShimmerImageView
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.core.extension.setImageViewPH
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.DownloadMediaEventBus
import com.joshtalks.joshskills.repository.local.eventbus.MediaProgressEventBus
import com.joshtalks.joshskills.repository.local.eventbus.PlayVideoEvent
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.pnikosis.materialishprogress.ProgressWheel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.lang.ref.WeakReference

class VideoViewHolder(
    view: View, private val activityRef: WeakReference<FragmentActivity>, userId: String
) : BaseViewHolder(view, userId) {
    private val rootSubView: FrameLayout = view.findViewById(R.id.root_sub_view)
    private val messageBody: JoshTextView = view.findViewById(R.id.text_message_body)
    private val titleView: AppCompatTextView = view.findViewById(R.id.text_title)
    private val textMessageTime: AppCompatTextView = view.findViewById(R.id.text_message_time)
    private val imageView: ShimmerImageView = view.findViewById(R.id.image_view)
    private val downloadContainer: FrameLayout = view.findViewById(R.id.download_container)
    private val ivCancelDownload: AppCompatImageView = view.findViewById(R.id.iv_cancel_download)
    private val ivStartDownload: AppCompatImageView = view.findViewById(R.id.iv_start_download)
    private val playIcon: AppCompatImageView = view.findViewById(R.id.play_icon)
    private val progressDialog: ProgressWheel = view.findViewById(R.id.progress_dialog)
    private val compositeDisposable = CompositeDisposable()
    private var message: ChatModel? = null

    private var appAnalytics: AppAnalytics = AppAnalytics.create(AnalyticsEvent.VIDEO_VH.NAME)
        .addBasicParam()
        .addUserDetails()


    init {
        ivStartDownload.also {
            it.setOnClickListener {
                executeDownload()

            }
        }
        downloadContainer.also {
            it.setOnClickListener {
                executeDownload()
            }
        }
        playIcon.also {
            it.setOnClickListener {
                executeDownload()
            }
        }
        imageView.also {
            it.setOnClickListener {
                executeDownload()
            }
        }
        ivCancelDownload.also {
            it.setOnClickListener {
                downloadCancel()
            }
        }

    }

    override fun bind(message: ChatModel, previousMessage: ChatModel?) {
        compositeDisposable.clear()
        if (null != message.sender) {
            setViewHolderBG(previousMessage?.sender, message.sender!!, rootSubView)
        }
        this.message = message
        appAnalytics.addParam(AnalyticsEvent.VIDEO_ID.NAME, message.chatId)
            .addParam(
                AnalyticsEvent.VIDEO_DURATION.NAME,
                message.duration.toString()
            )

        imageView.setImageResource(0)
        titleView.text = EMPTY
        messageBody.text = EMPTY
        titleView.visibility = View.GONE
        messageBody.visibility = View.GONE
        downloadContainer.visibility = View.INVISIBLE

        textMessageTime.text = Utils.messageTimeConversion(message.created)
        addMessageAutoLink(messageBody)

        if (message.url != null) {
            if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED) {
                if (AppDirectory.isFileExist(message.downloadedLocalPath)) {
                    Utils.fileUrl(message.downloadedLocalPath, message.url)?.run {
                        setImageInIV(this)
                        downloadContainer.visibility = View.GONE
                    }
                } else {
                    downloadContainer.visibility = View.GONE
                    imageView.background =
                        ContextCompat.getDrawable(getAppContext(), R.drawable.video_placeholder)
                }

            } else if (message.downloadStatus == DOWNLOAD_STATUS.UPLOADING) {
                fileDownloadingInProgressView()
                setImageInIV(message.downloadedLocalPath!!)
            } else {
                imageView.background =
                    ContextCompat.getDrawable(getAppContext(), R.drawable.video_placeholder)
            }
        } else {
            message.question?.videoList?.getOrNull(0)?.let { videoObj ->
                setImageInIV(videoObj.video_image_url)
                when (message.downloadStatus) {
                    DOWNLOAD_STATUS.DOWNLOADED -> {
                        fileDownloadSuccess()
                    }
                    DOWNLOAD_STATUS.DOWNLOADING -> {
                        subscribeDownloader()
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
                    titleView.text = HtmlCompat.fromHtml(
                        question.title!!,
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                    titleView.visibility = VISIBLE
                }
                if (question.qText.isNullOrEmpty().not()) {
                    messageBody.text = HtmlCompat.fromHtml(
                        question.qText!!,
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                    messageBody.visibility = VISIBLE
                }
            }
        }
    }

    private fun setImageInIV(url: String) {
        imageView.setImageViewPH(
            url,
            placeholderImage = R.drawable.image_ph,
            callback = {
                playIcon.visibility = VISIBLE
            })
    }

    override fun unBind() {
    }

    private fun fileDownloadSuccess() {
        appAnalytics.addParam(AnalyticsEvent.VIDEO_VIEW_STATUS.NAME, "Downloaded")
        downloadContainer.visibility = View.GONE
        ivStartDownload.visibility = View.GONE
        progressDialog.visibility = View.GONE
        ivCancelDownload.visibility = View.GONE
    }

    private fun fileNotDownloadView() {
        appAnalytics.addParam(AnalyticsEvent.VIDEO_VIEW_STATUS.NAME, "Not downloaded")
        downloadContainer.visibility = VISIBLE
        ivStartDownload.visibility = VISIBLE
        progressDialog.visibility = View.GONE
        ivCancelDownload.visibility = View.GONE
    }

    private fun fileDownloadingInProgressView() {
        downloadContainer.visibility = VISIBLE
        ivStartDownload.visibility = View.GONE
        progressDialog.visibility = VISIBLE
        ivCancelDownload.visibility = VISIBLE
    }


    private fun download(url: String) {
        appAnalytics.addParam(AnalyticsEvent.VIDEO_DOWNLOAD_STATUS.NAME, "Downloading")
    }

    private fun executeDownload() {
        if (PermissionUtils.isStoragePermissionEnabled(activityRef.get()!!)) {
            videoDownload()
        } else {
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
    }

    private fun videoDownload() {
        message?.let {
            if (it.url == null) {
                it.question?.videoList?.getOrNull(0)?.let { _ ->
                    appAnalytics.push()
                    if (it.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED) {
                        RxBus2.publish(PlayVideoEvent(it))
                        return
                    }
                    if (it.downloadStatus == DOWNLOAD_STATUS.DOWNLOADING) {
                        RxBus2.publish(PlayVideoEvent(it))
                        return
                    }
                    RxBus2.publish(
                        DownloadMediaEventBus(
                            DOWNLOAD_STATUS.REQUEST_DOWNLOADING,
                            it.chatId,
                            BASE_MESSAGE_TYPE.VI,
                            chatModel = it,
                            url = getUrlForDownload(it)
                        )
                    )
                    RxBus2.publish(PlayVideoEvent(it))
                }
            } else {
                when {
                    it.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED -> {
                        RxBus2.publish(PlayVideoEvent(it))
                    }
                    AppDirectory.isFileExist(it.downloadedLocalPath).not() -> {
                        showToast(getAppContext().getString(R.string.video_url_not_exist))
                    }
                    else -> {
                        appAnalytics.push()
                        RxBus2.publish(PlayVideoEvent(it))
                    }
                }

            }
        }
    }

    fun downloadCancel() {
        appAnalytics.addParam(AnalyticsEvent.VIDEO_DOWNLOAD_STATUS.NAME, "Cancelled")
        message?.question?.videoList?.getOrNull(0)?.video_url?.run {
            AppObjectController.videoDownloadTracker.cancelDownload(Uri.parse(this))
        }
        fileNotDownloadView()
        message?.downloadStatus = DOWNLOAD_STATUS.NOT_START
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
            RxBus2.listen(MediaProgressEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    try {
                        if (AppObjectController.gsonMapperForLocal.fromJson(
                                it.id,
                                ChatModel::class.java
                            ).chatId == message?.chatId
                        ) {

                            when {
                                Download.STATE_STOPPED == it.state -> updateProgress()
                                Download.STATE_DOWNLOADING == it.state -> {
                                    message?.progress = it.progress.toInt()
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

}