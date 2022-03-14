package com.joshtalks.joshskills.ui.special_practice

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.daasuu.mp4compose.FillMode
import com.daasuu.mp4compose.composer.Mp4Composer
import com.daasuu.mp4compose.filter.GlWatermarkFilter
import com.google.android.exoplayer2.Player
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.databinding.FragmentViewShareVideoBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.LinkAttribution
import com.joshtalks.joshskills.ui.pdfviewer.CURRENT_VIDEO_PROGRESS_POSITION
import com.joshtalks.joshskills.ui.referral.REFERRAL_SHARE_TEXT_SHARABLE_VIDEO
import com.joshtalks.joshskills.ui.referral.USER_SHARE_SHORT_URL
import com.joshtalks.joshskills.ui.special_practice.utils.*
import com.joshtalks.joshskills.ui.special_practice.viewmodel.ViewAndShareViewModel
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.Defines
import io.branch.referral.util.LinkProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber


class ViewAndShareVideoFragment : CoreJoshFragment(), Player.EventListener {
    private lateinit var binding: FragmentViewShareVideoBinding
    private var userReferralCode = Mentor.getInstance().referralCode
    private lateinit var sharableVideoUrl: String
    private var specialId: String? = null
    private var videoPathOriginal: String? = null
    private var imagePath: String? = null
    private var videoName: String? = null
    private var isVideoProcessing = false

    private val viewAndShareViewModel: ViewAndShareViewModel by lazy {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            videoPathOriginal = it.getString(VIDEO_PATH)
            imagePath = it.getString(IMAGE_PATH)
            specialId = it.getString(SPECIAL_ID)
            videoName = it.getString(IMAGE_NAME)
        }
    }

    companion object {
        fun newInstance(
            videoPath: String,
            imagePath: String,
            specialId: String,
            videoName: String
        ) =
            ViewAndShareVideoFragment().apply {
                arguments = Bundle().apply {
                    putString(VIDEO_PATH, videoPath)
                    putString(IMAGE_PATH, imagePath)
                    putString(SPECIAL_ID, specialId)
                    putString(IMAGE_NAME, videoName)
                }
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

        binding.progressBar.isVisible = true
        addOverLayOnVideo(convertImageFilePathIntoBitmap(imagePath ?: EMPTY))

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
            if (packageString.isNullOrEmpty().not()) {
                waIntent.setPackage(packageString)
            }
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

    private fun showVideoUi(videoUrl: String) {
        try {
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
        } catch (ex: Exception) {
        }
    }

    override fun onPause() {
        binding.videoView.onPause()
        super.onPause()
    }

    private fun addOverLayOnVideo(bitmap: Bitmap?) {
        try {
            val videoPath = getVideoFilePath()
            Mp4Composer(videoPathOriginal ?: EMPTY, videoPath)
                .size(getWindowWidth(requireContext()), getHeightByPixel(requireContext()))
                .fillMode(FillMode.PRESERVE_ASPECT_CROP)
                .filter(GlWatermarkFilter(bitmap, GlWatermarkFilter.Position.RIGHT_BOTTOM))
                .listener(object : Mp4Composer.Listener {
                    override fun onProgress(progress: Double) {
                        isVideoProcessing = true
                    }

                    override fun onCurrentWrittenVideoTime(timeUs: Long) {}

                    override fun onCompleted() {
                        try {
                            if (isAdded) {
                                exportMp4ToGallery(requireActivity(), videoPath)
                                viewAndShareViewModel.submitPractice(videoPath, specialId ?: EMPTY)
                                lifecycleScope.launch(Dispatchers.Main) {
                                    binding.materialCardView.isClickable = true
                                    binding.progressBar.visibility = View.GONE
                                    sharableVideoUrl = videoPath
                                    showVideoUi(videoPath)
                                }
                                viewAndShareViewModel.updateUserRecordVideo(
                                    specialId ?: EMPTY,
                                    EMPTY
                                )
                                deleteFile(videoName ?: EMPTY)
                                isVideoProcessing = false
                            }
                        } catch (ex: Exception) {
                            showToast(ex.message ?: EMPTY)
                        }
                    }

                    override fun onCanceled() {}

                    override fun onFailed(exception: Exception) {
                        Log.e(TAG, "onFailed: $exception")
                    }
                })
                .start()
        } catch (ex: Exception) {
        }
    }

    fun onBackPress() {
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showToast(isVideoProcessing.toString())
                    if (isVideoProcessing)
                        showDialog()
                    else
                        moveToNewActivity()
                }
            })
    }

    private fun moveToNewActivity() {
        try {
            val i = Intent(activity, SpecialPracticeActivity::class.java)
            i.putExtra(SPECIAL_ID, specialId)
            startActivity(i)
            requireActivity().overridePendingTransition(0, 0)
            requireActivity().finish()
        } catch (ex: Exception) {
        }
    }

    private fun showDialog() {
        val dialogView = Dialog(requireActivity())
        dialogView.requestWindowFeature(Window.FEATURE_NO_TITLE)

        dialogView.setCancelable(false)
        dialogView.setContentView(R.layout.custom_k_factor_dialog)
        dialogView.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogView.show()

        val btnConfirm = dialogView.findViewById<AppCompatTextView>(R.id.yes_button)
        val btnNotNow = dialogView.findViewById<AppCompatTextView>(R.id.not_now)

        btnConfirm
            .setOnClickListener {
                moveToNewActivity()
                dialogView.dismiss()
            }
        btnNotNow.setOnClickListener {
            dialogView.dismiss()
        }
    }
}