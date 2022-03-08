package com.joshtalks.joshskills.ui.special_practice

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Outline
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.daasuu.mp4compose.FillMode
import com.daasuu.mp4compose.composer.Mp4Composer
import com.daasuu.mp4compose.filter.GlWatermarkFilter
import com.google.android.exoplayer2.Player
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.databinding.FragmentViewShareVideoBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.LinkAttribution
import com.joshtalks.joshskills.ui.pdfviewer.CURRENT_VIDEO_PROGRESS_POSITION
import com.joshtalks.joshskills.ui.referral.REFERRAL_SHARE_TEXT_SHARABLE_VIDEO
import com.joshtalks.joshskills.ui.referral.USER_SHARE_SHORT_URL
import com.joshtalks.joshskills.ui.special_practice.viewmodel.ViewAndShareViewModel
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.Defines
import io.branch.referral.util.LinkProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class ViewAndShareVideoFragment : CoreJoshFragment(), Player.EventListener {
    private lateinit var binding: FragmentViewShareVideoBinding
    private var userReferralCode: String = "12345"
    private lateinit var sharableVideoUrl: String
    private var specialId: Int = 0
    private val viewAndSViewModel: ViewAndShareViewModel by lazy {
        ViewModelProvider(this).get(ViewAndShareViewModel::class.java)
    }
    var openVideoPlayerActivity: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getLongExtra(
                CURRENT_VIDEO_PROGRESS_POSITION,
                0
            )?.let { progress ->
                binding.videoView.progress = progress
                binding.videoView.onResume()
            }
        }
    }

    companion object {
        private const val VIDEO_PATH = "VIDEO_PATH"
        private const val IMAGE_PATH = "IMAGE_PATH"
        private const val IMAGE_BITMAP = "IMAGE_BITMAP"

        fun newInstance(
            videoPath: String,
            imagePath: String,
            imageBitmap: Bitmap
        ): ViewAndShareVideoFragment {
            val args = Bundle()
            args.putString(VIDEO_PATH, videoPath)
            args.putString(IMAGE_PATH, imagePath)
            args.putParcelable(IMAGE_BITMAP, imageBitmap)
            val fragment = ViewAndShareVideoFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentViewShareVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewAndSViewModel.getSpecialId()
        arguments?.getString(VIDEO_PATH)?.let { videoPath ->
            Log.e("Sagar", "onViewCreated: $videoPath")
            binding.progressBar.isVisible = true
            if (arguments?.getString(IMAGE_PATH) != null) {
                // addOverlayToVideo(videoPath, arguments?.getString(IMAGE_PATH)!!)
                addOverLayOnVideo(arguments?.getParcelable(IMAGE_BITMAP))
            } else {
                showIntroVideoUi(Uri.parse(videoPath).toString())
            }
        }

        viewAndSViewModel.specialIdData.observe(requireActivity()) {
            specialId = it.id ?: 0
        }

        binding.materialCardView.setOnClickListener {
            if (binding.materialCardView.isClickable)
                getDeepLinkAndInviteFriends()
            else
                showToast("Video is processing")
        }

        onBackPress()
    }

    fun getDeepLinkAndInviteFriends() {
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
            .generateShortUrl(requireContext(), lp) { url, error ->
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
                            getAppShareUrl(),
                        referralTimestamp = referralTimestamp
                    )
            }
    }

    private fun getAppShareUrl(): String {
        return "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID + "&referrer=utm_source%3D$userReferralCode"
    }

    fun inviteFriends(dynamicLink: String, referralTimestamp: Long) {
        var referralText =
            AppObjectController.getFirebaseRemoteConfig()
                .getString(REFERRAL_SHARE_TEXT_SHARABLE_VIDEO)
        referralText = referralText.plus("\n").plus(dynamicLink)
        try {
            lifecycleScope.launch {
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
            waIntent.putExtra(Intent.EXTRA_TEXT, referralText)
            waIntent.putExtra(
                Intent.EXTRA_STREAM,
                Uri.parse(sharableVideoUrl)
            )
            waIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(waIntent, "Share with"))

        } catch (e: PackageManager.NameNotFoundException) {
            showToast(getString(R.string.whatsApp_not_installed))
        }
    }

    private fun showIntroVideoUi(videoUrl: String) {
        binding.videoView.visibility = View.VISIBLE
        binding.videoView.seekToStart()
        binding.videoView.setUrl(videoUrl)
        binding.videoView.onStart()
        binding.videoView.setPlayListener {
            val currentVideoProgressPosition = binding.videoView.progress
            openVideoPlayerActivity.launch(
                VideoPlayerActivity.getActivityIntent(
                    requireContext(),
                    EMPTY,
                    null,
                    videoUrl,
                    currentVideoProgressPosition,
                    conversationId = getConversationId()
                )
            )
        }

        lifecycleScope.launchWhenStarted {
            binding.videoView.downloadStreamPlay()
        }

        binding.videoView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, 15f)
            }
        }
        binding.videoView.clipToOutline = true
    }

    override fun onPause() {
        binding.videoView.onPause()
        super.onPause()
    }

    private fun addOverLayOnVideo(bitmap: Bitmap?) {
        var videoPath = getVideoFilePath()
        Log.e("Sagar", "addOverLayOnVideo: $videoPath")
        Mp4Composer(arguments?.getString(VIDEO_PATH)!!, videoPath)
            .size(1080, 1080)
            .fillMode(FillMode.PRESERVE_ASPECT_CROP)
            .filter(GlWatermarkFilter(bitmap, GlWatermarkFilter.Position.RIGHT_BOTTOM))
            .listener(object : Mp4Composer.Listener {
                override fun onProgress(progress: Double) {
                }

                override fun onCurrentWrittenVideoTime(timeUs: Long) {
                }

                override fun onCompleted() {
                    exportMp4ToGallery(requireContext(), videoPath)
                    showToast(videoPath)

                    viewAndSViewModel.submitPractise(videoPath, specialId)

                    lifecycleScope.launch(Dispatchers.Main) {
                        binding.materialCardView.isClickable = true
                        binding.progressBar.visibility = View.GONE
                        sharableVideoUrl = videoPath
                        showIntroVideoUi(videoPath)
                    }
                }

                override fun onCanceled() {
                }

                override fun onFailed(exception: java.lang.Exception?) {
                    Log.e("Sagar", "onFailed: $exception")
                }

            })
            .start()
    }

    fun getVideoFilePath(): String {
        return getAndroidMoviesFolder()?.absolutePath + "/" + SimpleDateFormat("yyyyMM_dd-HHmmss").format(
            Date()
        ) + "filter_apply.mp4"
    }

    fun getAndroidMoviesFolder(): File? {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    }

    fun exportMp4ToGallery(context: Context, filePath: String) {
        val values = ContentValues(2)
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        values.put(MediaStore.Video.Media.DATA, filePath)
        context.contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            values
        )
        context.sendBroadcast(
            Intent(
                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.parse("file://$filePath")
            )
        )
    }

    fun onBackPress() {
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    moveToNewActivity()
                }
            })
    }

    private fun moveToNewActivity() {
        val i = Intent(activity, SpecialPracticeActivity::class.java)
        startActivity(i)
        (activity as Activity?)?.overridePendingTransition(0, 0)
        requireActivity().finish()
    }
}