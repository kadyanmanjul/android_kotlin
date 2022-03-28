package com.joshtalks.joshskills.ui.special_practice.viewmodel

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Outline
import android.net.Uri
import android.view.View
import android.view.ViewOutlineProvider
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.daasuu.mp4compose.FillMode
import com.daasuu.mp4compose.composer.Mp4Composer
import com.daasuu.mp4compose.filter.GlWatermarkFilter
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.custom_ui.JoshVideoPlayer
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.AmazonPolicyResponse
import com.joshtalks.joshskills.repository.server.LinkAttribution
import com.joshtalks.joshskills.ui.referral.REFERRAL_SHARE_TEXT_SHARABLE_VIDEO
import com.joshtalks.joshskills.ui.referral.USER_SHARE_SHORT_URL
import com.joshtalks.joshskills.ui.special_practice.model.SaveVideoModel
import com.joshtalks.joshskills.ui.special_practice.model.SpecialPractice
import com.joshtalks.joshskills.ui.special_practice.repo.ViewAndShareRepo
import com.joshtalks.joshskills.ui.special_practice.utils.*
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.Defines
import io.branch.referral.util.LinkProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.File

class ViewAndShareViewModel : BaseViewModel() {
    val specialIdData = MutableLiveData<SpecialPractice>()
    private var userReferralCode = Mentor.getInstance().referralCode
    var sharableVideoUrl = ObservableField(EMPTY)
    var videoPathOriginal = ObservableField(EMPTY)
    val isVideoProcessing = ObservableBoolean(false)
    val isShareCardClickable = ObservableBoolean(false)
    val isProgressbarShow = ObservableBoolean(true)
    fun submitPractice(localPath: String, specialId: String) {
        var videoUrl = EMPTY
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (localPath.isEmpty().not()) {
                    val obj = mapOf("media_path" to File(localPath).name)
                    val responseObj =
                        AppObjectController.chatNetworkService.requestUploadMediaAsync(obj).await()
                    val statusCode: Int = uploadOnS3Server(responseObj, localPath)
                    if (statusCode in 200..210) {
                        val url =
                            responseObj.url.plus(File.separator).plus(responseObj.fields["key"])
                        videoUrl = url
                    } else {
                        return@launch
                    }
                }

                val resp = ViewAndShareRepo().saveRecordedVideo(
                    SaveVideoModel(
                        Mentor.getInstance().getId(), videoUrl, specialId
                    )
                )
            } catch (ex: Exception) {
                showToast("${ex.message}")

            }
        }
    }

    private suspend fun uploadOnS3Server(
        responseObj: AmazonPolicyResponse,
        mediaPath: String
    ): Int {
        return viewModelScope.async(Dispatchers.IO) {
            val parameters = emptyMap<String, RequestBody>().toMutableMap()
            for (entry in responseObj.fields) {
                parameters[entry.key] = Utils.createPartFromString(entry.value)
            }

            val requestFile = File(mediaPath).asRequestBody("*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData(
                "file",
                responseObj.fields["key"],
                requestFile
            )
            val responseUpload = AppObjectController.mediaDUNetworkService.uploadMediaAsync(
                responseObj.url,
                parameters,
                body
            ).execute()
            return@async responseUpload.code()
        }.await()
    }

    fun updateUserRecordVideo(specialId: String, recordVideo: String) {
        viewModelScope.launch(Dispatchers.IO) {
            AppObjectController.appDatabase.specialDao()
                .updateRecordedTable(specialId, recordVideo)
        }
    }

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
                        WHATSAPP_PACKAGE_STRING,
                        dynamicLink = url,
                        referralTimestamp = referralTimestamp
                    )
                else
                    inviteFriends(
                        WHATSAPP_PACKAGE_STRING,
                        dynamicLink = if (PrefManager.hasKey(USER_SHARE_SHORT_URL))
                            PrefManager.getStringValue(USER_SHARE_SHORT_URL)
                        else
                            getAppShareUrl(userReferralCode),
                        referralTimestamp = referralTimestamp
                    )
            }
    }

    fun inviteFriends(packageString: String? = null, dynamicLink: String, referralTimestamp: Long) {
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
            if (packageString.isNullOrEmpty().not()) {
                waIntent.setPackage(packageString)
            }
            waIntent.putExtra(Intent.EXTRA_TEXT, referralText)
            waIntent.putExtra(
                Intent.EXTRA_STREAM,
                Uri.parse(sharableVideoUrl.get())
            )
            waIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            message.what = CALL_INVITE_FRIENDS_METHOD
            message.obj = waIntent
            singleLiveEvent.value = message
        } catch (e: PackageManager.NameNotFoundException) {
            showToast("WhatsApp not Installed")
        }
    }

    fun showVideoUi(
        videoUrl: String,
        videoView: JoshVideoPlayer,
    ) {
        try {
            videoView.visibility = View.VISIBLE
            videoView.seekToStart()
            videoView.setUrl(videoUrl)
            videoView.onStart()
            message.what = PLAY_RECORDED_VIDEO
            singleLiveEvent.value = message
            viewModelScope.launch {
                videoView.downloadStreamPlay()
            }

            videoView.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, 15f)
                }
            }
            videoView.clipToOutline = true
        } catch (ex: Exception) {
        }
    }

    fun addOverLayOnVideo(
        bitmap: Bitmap?,
        spViewModel: SpecialPracticeViewModel,
        videoView: JoshVideoPlayer
    ) {
        try {
            val videoPath = getVideoFilePath()

            Mp4Composer(spViewModel.cameraVideoPath.get() ?: EMPTY, videoPath)
                .size(getWindowWidth(), getHeightByPixel())
                .fillMode(FillMode.PRESERVE_ASPECT_CROP)
                .filter(GlWatermarkFilter(bitmap, GlWatermarkFilter.Position.RIGHT_BOTTOM))
                .listener(object : Mp4Composer.Listener {
                    override fun onProgress(progress: Double) {
                        isVideoProcessing.set(true)
                    }

                    override fun onCurrentWrittenVideoTime(timeUs: Long) {}

                    override fun onCompleted() {
                        try {

                            submitPractice(videoPath, spViewModel.specialId.get() ?: EMPTY)
                            viewModelScope.launch(Dispatchers.Main) {
                                isShareCardClickable.set(true)
                                isProgressbarShow.set(false)
                                sharableVideoUrl.set(videoPath)
                                showVideoUi(videoPath, videoView)
                            }
                            updateUserRecordVideo(spViewModel.specialId.get() ?: EMPTY, EMPTY)
                            deleteFile(spViewModel.imageNameForDelete.get() ?: EMPTY)
                            isVideoProcessing.set(false)
                        } catch (ex: Exception) {
                            showToast(ex.message ?: EMPTY)
                        }
                    }

                    override fun onCanceled() {}

                    override fun onFailed(exception: Exception) {
                        showToast(exception.message?: EMPTY)
                    }
                })
                .start()
        } catch (ex: Exception) {}
    }

}
