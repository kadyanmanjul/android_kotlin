package com.joshtalks.joshskills.ui.special_practice.viewmodel

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Outline
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.ViewOutlineProvider
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableLong
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.custom_ui.JoshVideoPlayer
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.LinkAttribution
import com.joshtalks.joshskills.ui.referral.REFERRAL_SHARE_TEXT_SHARABLE_VIDEO
import com.joshtalks.joshskills.ui.referral.USER_SHARE_SHORT_URL
import com.joshtalks.joshskills.ui.special_practice.model.SpecialPractice
import com.joshtalks.joshskills.ui.special_practice.model.SpecialPracticeModel
import com.joshtalks.joshskills.ui.special_practice.repo.SpecialPracticeRepo
import com.joshtalks.joshskills.ui.special_practice.utils.K_FACTOR_ON_BACK_PRESSED
import com.joshtalks.joshskills.ui.special_practice.utils.SHOW_RECORDED_SPECIAL_VIDEO
import com.joshtalks.joshskills.ui.special_practice.utils.SHOW_RECORD_VIDEO
import com.joshtalks.joshskills.ui.special_practice.utils.DOWNLOAD_VIDEO
import com.joshtalks.joshskills.ui.special_practice.utils.getAndroidDownloadFolder
import com.joshtalks.joshskills.ui.special_practice.utils.getAppShareUrl
import com.joshtalks.joshskills.ui.special_practice.utils.WHATSAPP_PACKAGE_STRING
import com.joshtalks.joshskills.ui.special_practice.utils.START_VIDEO_RECORDING
import com.joshtalks.joshskills.ui.special_practice.utils.CALL_INVITE_FRIENDS_METHOD
import com.joshtalks.joshskills.ui.special_practice.utils.START_VIEW_AND_SHARE
import com.joshtalks.joshskills.ui.special_practice.utils.SHOW_SAMPLE_VIDEO
import com.joshtalks.joshskills.ui.special_practice.utils.CLOSE_SAMPLE_VIDEO
import com.joshtalks.joshskills.ui.special_practice.utils.DOWNLOAD_ID_DATA
import com.joshtalks.joshskills.ui.special_practice.utils.OPEN_VIEW_AND_SHARE
import com.joshtalks.joshskills.ui.special_practice.utils.SHOW_SAMPLE_SPECIAL_VIDEO
import com.joshtalks.joshskills.ui.special_practice.utils.MOVE_TO_ACTIVITY
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.Defines
import io.branch.referral.util.LinkProperties
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber
import java.util.Random

class SpecialPracticeViewModel : BaseViewModel() {
    val specialPracticeRepo = SpecialPracticeRepo()
    val specialPracticeData = MutableLiveData<SpecialPracticeModel>()
    val specialIdData = MutableLiveData<SpecialPractice>()
    private var userReferralCode = Mentor.getInstance().referralCode
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
                            withContext(dispatcher) {
                                Log.e("sagar", "fetchSpecialPracticeData: 1")
                                message.what = DOWNLOAD_VIDEO
                                singleLiveEvent.value = message
                            }
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
        sentenceInEnglish.set(specialPractice?.wordEnglish)
        wordInHindi.set(specialPractice?.wordHindi)
        sentenceInHindi.set(specialPractice?.sentenceHindi)
    }

    fun getSpecialIdData(specialId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            recordedPathLocal.set(AppObjectController.appDatabase.specialDao()
                .getSpecialPracticeFromId(specialId)?.recordedVideo)
        }
    }

    fun onBackPress(view: View) {
        message.what = K_FACTOR_ON_BACK_PRESSED
        singleLiveEvent.value = message
    }

//    fun checkDownloadPermissionExist(view: View){
//        message.what = CHECK_DOWNLOAD_PERMISSION_EXIST
//        singleLiveEvent.value = message
//    }

    fun getDeepLinkAndInviteFriends(view: View) {
        val referralTimestamp = System.currentTimeMillis()
        val branchUniversalObject = BranchUniversalObject()
            .setCanonicalIdentifier(userReferralCode.plus(referralTimestamp))
            .setTitle("Invite Friend")
            .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
            .setLocalIndexMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
        val lp = LinkProperties()
            .setChannel(userReferralCode)
            .setFeature("sharing")
            .setCampaign(userReferralCode.plus(referralTimestamp))
            .addControlParameter(Defines.Jsonkey.ReferralCode.key, userReferralCode)
            .addControlParameter(
                Defines.Jsonkey.UTMCampaign.key,
                userReferralCode.plus(referralTimestamp)
            )
            .addControlParameter(Defines.Jsonkey.UTMMedium.key, "referral")

        branchUniversalObject
            .generateShortUrl(AppObjectController.joshApplication, lp) { url, error ->
                if (error == null)
                    inviteFriends(
                        dynamicLink = url,
                        referralTimestamp = referralTimestamp
                    )
                else
                    inviteFriends(
                        dynamicLink = if (PrefManager.hasKey(USER_SHARE_SHORT_URL))
                            PrefManager.getStringValue(USER_SHARE_SHORT_URL)
                        else
                            getAppShareUrl(userReferralCode),
                        referralTimestamp = referralTimestamp
                    )
            }
    }

    fun inviteFriends(dynamicLink: String, referralTimestamp: Long) {
        var referralText =
            AppObjectController.getFirebaseRemoteConfig()
                .getString(REFERRAL_SHARE_TEXT_SHARABLE_VIDEO)
        referralText = referralText.plus("\n").plus(dynamicLink)
        try {
            viewModelScope.launch {
                try {
                    val requestData = LinkAttribution(
                        mentorId = Mentor.getInstance().getId(),
                        contentId = userReferralCode.plus(
                            referralTimestamp
                        ),
                        sharedItem = "User Video",
                        sharedItemType = "VI",
                        deepLink = dynamicLink
                    )
                    val res = AppObjectController.commonNetworkService.getDeepLink(requestData)
                    Timber.i(res.body().toString())
                } catch (ex: Exception) {
                    Timber.e(ex)
                }
            }

            val waIntent = Intent(Intent.ACTION_SEND)
            waIntent.type = "*/*"
            if (WHATSAPP_PACKAGE_STRING.isEmpty().not()) {
                waIntent.setPackage(WHATSAPP_PACKAGE_STRING)
            }
            waIntent.putExtra(Intent.EXTRA_TEXT, referralText)
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
        message.what = SHOW_SAMPLE_VIDEO
        message.obj = false
        singleLiveEvent.value = message
    }

    fun closeIntroVideoPopUpUi(view: View) {
        message.what = CLOSE_SAMPLE_VIDEO
        singleLiveEvent.value = message
    }

    fun startRecording(view: View) {
        message.what = START_VIDEO_RECORDING
        singleLiveEvent.value = message
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
            view.setPlayListener {
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