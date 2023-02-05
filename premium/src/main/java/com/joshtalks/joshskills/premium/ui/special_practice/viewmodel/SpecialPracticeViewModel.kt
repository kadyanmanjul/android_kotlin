package com.joshtalks.joshskills.premium.ui.special_practice.viewmodel

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Outline
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.View
import android.view.ViewOutlineProvider
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableLong
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.premium.base.BaseViewModel
import com.joshtalks.joshskills.premium.core.*
import com.joshtalks.joshskills.premium.core.Utils.isInternetAvailable
import com.joshtalks.joshskills.premium.core.custom_ui.JoshVideoPlayer
import com.joshtalks.joshskills.premium.repository.local.model.Mentor
import com.joshtalks.joshskills.premium.ui.referral.REFERRAL_SHARE_TEXT_SHARABLE_VIDEO
import com.joshtalks.joshskills.premium.ui.special_practice.model.SpecialPractice
import com.joshtalks.joshskills.premium.ui.special_practice.model.SpecialPracticeModel
import com.joshtalks.joshskills.premium.ui.special_practice.repo.SpecialPracticeRepo
import com.joshtalks.joshskills.premium.ui.special_practice.utils.*
import com.joshtalks.joshskills.premium.util.DeepLinkUtil
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*

class SpecialPracticeViewModel : BaseViewModel() {
    val specialPracticeRepo = SpecialPracticeRepo()
    val specialPracticeData = MutableLiveData<SpecialPracticeModel>()
    val specialIdData = MutableLiveData<SpecialPractice>()
    val recordedPathLocal = ObservableField(EMPTY)
    var wordText = ObservableField(EMPTY)
    var instructionText = ObservableField(EMPTY)
    var textMessageTitle = ObservableField(0)

    val wordInEnglish = ObservableField(EMPTY)
    val sentenceInEnglish = ObservableField(EMPTY)
    val wordInHindi = ObservableField(EMPTY)
    val sentenceInHindi = ObservableField(EMPTY)
    val specialId = ObservableField(EMPTY)

    var videoUrl = EMPTY
    var recordedUrl = EMPTY
    val dispatcher: CoroutineDispatcher by lazy { Dispatchers.Main }
    val isRecordButtonClick = ObservableBoolean(true)
    val isVideoPopUpShow = ObservableBoolean(false)

    val imageNameForDelete = ObservableField(EMPTY)
    val cameraVideoPath = ObservableField(EMPTY)
    val imagePathForSetOnVideo = ObservableField(EMPTY)
    val videoUri = ObservableField(EMPTY)

    var downloadID = ObservableLong(0L)
    val isVideoPlay = ObservableBoolean(false)
    val isVideoDownloadingStarted = ObservableBoolean(false)

    val videoDownloadPath = ObservableField(EMPTY)
    val downloadComplete = ObservableBoolean(false)


    fun fetchSpecialPracticeData(params: HashMap<String, String>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = specialPracticeRepo.getSpecialData(params)
                if (response.isSuccessful) {
                    wordText.set(response.body()?.specialPractice?.wordText)
                    textMessageTitle.set(response.body()?.specialPractice?.practiceNo)
                    instructionText.set(response.body()?.specialPractice?.instructionText)
                    setData(response.body()?.specialPractice)
                    videoUrl = response.body()?.specialPractice?.sampleVideoUrl ?: EMPTY
                    recordedUrl = response.body()?.recordedVideoUrl ?: EMPTY

                    if (recordedUrl != EMPTY) {
                        withContext(dispatcher) {
                            message.what = SHOW_RECORD_VIDEO
                            message.obj = true
                            singleLiveEvent.value = message
                        }
                        if (recordedPathLocal.get() == EMPTY || recordedPathLocal.get() == null) {
                            specialPracticeData.postValue(response.body())
                        }
                    }
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    private fun setData(specialPractice: SpecialPractice?) {
        wordInEnglish.set(specialPractice?.wordEnglish)
        sentenceInEnglish.set(specialPractice?.sentenceEnglish)
        wordInHindi.set(specialPractice?.wordHindi)
        sentenceInHindi.set(specialPractice?.sentenceHindi)
    }

    fun getSpecialIdData(specialId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            recordedPathLocal.set(
                AppObjectController.appDatabase.specialDao()
                    .getSpecialPracticeFromId(specialId)?.recordedVideo
            )
        }
    }

    fun onBackPress(view: View) {
        message.what = K_FACTOR_ON_BACK_PRESSED
        singleLiveEvent.value = message
    }

    fun getDeepLinkAndInviteFriends(view: View) {
        DeepLinkUtil(AppObjectController.joshApplication)
            .setReferralCode(Mentor.getInstance().referralCode)
            .setReferralCampaign()
            .setListener(object : DeepLinkUtil.OnDeepLinkListener {
                override fun onDeepLinkCreated(deepLink: String) {
                    inviteFriends(deepLink)
                }
            })
            .build()
    }

    fun inviteFriends(dynamicLink: String) {
        var referralText =
            AppObjectController.getFirebaseRemoteConfig()
                .getString(REFERRAL_SHARE_TEXT_SHARABLE_VIDEO)
        referralText = referralText.plus("\n").plus(dynamicLink)
        try {
            val waIntent = Intent(Intent.ACTION_SEND)
            waIntent.type = "*/*"
            if (WHATSAPP_PACKAGE_STRING.isEmpty().not()) {
                waIntent.setPackage(WHATSAPP_PACKAGE_STRING)
            }
            waIntent.putExtra(Intent.EXTRA_TEXT, referralText)
            if (recordedPathLocal.get() == EMPTY) {
                recordedPathLocal.set(videoDownloadPath.get())
            }
            waIntent.putExtra(
                Intent.EXTRA_STREAM,
                Uri.parse(getAndroidDownloadFolder()?.absolutePath + "/" + recordedPathLocal.get())
            )

            waIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            message.what = CALL_INVITE_FRIENDS_METHOD
            message.obj = waIntent
            singleLiveEvent.value = message

        } catch (e: PackageManager.NameNotFoundException) {
            showToast("WhatsApp not Installed")
        }
    }

    fun onCardSampleVideoPlayer(view: View) {
        if (isInternetAvailable()) {
            message.what = SHOW_SAMPLE_VIDEO
            message.obj = false
            singleLiveEvent.value = message
        } else {
            showToast("Seems like your Internet is too slow or not available.")
        }
    }

    fun closeIntroVideoPopUpUi(view: View) {
        message.what = CLOSE_SAMPLE_VIDEO
        singleLiveEvent.value = message
    }

    fun startRecording(view: View) {
        if (isInternetAvailable()) {
            message.what = START_VIDEO_RECORDING
            singleLiveEvent.value = message
        } else {
            showToast("Seems like your Internet is too slow or not available.")
        }
    }

    fun startViewAndShareScreen() {
        message.what = START_VIEW_AND_SHARE
        singleLiveEvent.value = message
    }

    fun downloadFile(
        url: String,
        messageFile: String = "Downloading file",
        title: String = "Josh Skills"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            var fileName = Utils.getFileNameFromURL(url)
            if (fileName.isEmpty()) {
                url.let {
                    fileName = it + Random(5).nextInt().toString().plus(it.getExtension())
                }
            }
            videoDownloadPath.set(fileName)

            val env = Environment.DIRECTORY_DOWNLOADS

            val request: DownloadManager.Request =
                DownloadManager.Request(Uri.parse(url))
                    .setTitle(title)
                    .setDescription(messageFile)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)
                    .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                    .setDestinationInExternalPublicDir(env, fileName)

            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                request.setRequiresCharging(false).setRequiresDeviceIdle(false)
            }

            withContext(dispatcher) {
                message.what = DOWNLOAD_ID_DATA
                message.obj = request
                singleLiveEvent.value = message
            }
        }
    }

    var onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadID.get() == id) {
                try {
                    CoroutineScope(Dispatchers.IO).launch {
                        AppObjectController.appDatabase.specialDao()
                            .updateRecordedTable(
                                specialId.get() ?: EMPTY,
                                videoDownloadPath.get() ?: EMPTY
                            )
                    }

                    if (isVideoDownloadingStarted.get().not()) {
                        // showToast(getString(R.string.downloading_complete))
                    } else {
                        getSpecialIdData(specialId.get() ?: EMPTY)
                        //  downloadComplete.set(true)
                    }
                    isVideoDownloadingStarted.set(false)
                } catch (Ex: Exception) {
                    //showToast(getString(R.string.something_went_wrong))
                }

            }
        }
    }

    fun openViewShareFrag() {
        message.what = OPEN_VIEW_AND_SHARE
        singleLiveEvent.value = message
    }

    fun showRecordedVideoUi(
        isRecordVideo: Boolean,
        view: JoshVideoPlayer,
        videoUrl: String
    ) {
        try {
            if (isRecordVideo)
                isVideoPlay.set(true)
            else {
                isVideoPopUpShow.set(true)
                isRecordButtonClick.set(false)
            }

            view.seekToStart()
            view.setUrl(videoUrl)
            view.onStart()
            view.fitToScreen()
            view.setFullScreenListener {
                if (isRecordVideo) {
                    message.what = SHOW_RECORDED_SPECIAL_VIDEO
                    singleLiveEvent.value = message
                } else {
                    message.what = SHOW_SAMPLE_SPECIAL_VIDEO
                    singleLiveEvent.value = message
                }
            }

            if (isRecordVideo) {
                viewModelScope.launch {
                    view.downloadStreamButNotPlay()
                }
            } else {
                viewModelScope.launch {
                    view.downloadStreamPlay()
                }
            }

            view.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, 15f)
                }
            }
            view.clipToOutline = true
        } catch (ex: Exception) {
            Timber.d(ex)
        }
    }

    fun moveToActivity() {
        message.what = MOVE_TO_ACTIVITY
        singleLiveEvent.value = message
    }
}