package com.joshtalks.joshskills.ui.special_practice

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Outline
import android.graphics.Point
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.*
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

const val WHATSAPP_PACKAGE_STRING = "com.whatsapp"

class ViewAndShareVideoFragment : CoreJoshFragment(), Player.EventListener {
    private lateinit var binding: FragmentViewShareVideoBinding
    private var userReferralCode = Mentor.getInstance().referralCode
    private lateinit var sharableVideoUrl: String
    private var specialId: String? = null
    private var videoPathOriginal: String? = null
    private var imagePath: String? = null
    lateinit var imageBitmap: Bitmap

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
            imageBitmap = it.getParcelable(IMAGE_BITMAP)!!
            specialId = it.getString(SPECIAL_ID)
        }
    }

    companion object {
        private const val VIDEO_PATH = "VIDEO_PATH"
        private const val IMAGE_PATH = "IMAGE_PATH"
        private const val IMAGE_BITMAP = "IMAGE_BITMAP"
        private const val SPECIAL_ID = "SPECIAL_ID"
        fun newInstance(
            videoPath: String,
            imagePath: String,
            imageBitmap: Bitmap,
            specialId11: String
        ) =
            ViewAndShareVideoFragment().apply {
                arguments = Bundle().apply {
                    putString(VIDEO_PATH, videoPath)
                    putString(IMAGE_PATH, imagePath)
                    putParcelable(IMAGE_BITMAP, imageBitmap)
                    putString(SPECIAL_ID, specialId11)
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
        addOverLayOnVideo(imageBitmap)

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
                            getAppShareUrl(),
                        referralTimestamp = referralTimestamp
                    )
            }
    }

    private fun getAppShareUrl(): String {
        return "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID + "&referrer=utm_source%3D$userReferralCode"
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

    private fun showIntroVideoUi(videoUrl: String) {
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
            var sizeA = getVideoResolution(videoPathOriginal?: EMPTY)
            var videoPath = getVideoFilePath()
            Log.e(TAG, "addOverLayOnVideo: ${sizeA?.height} ${sizeA?.width}")
            Log.e("Sagar", "addOverLayOnVideo: $videoPath")
            Mp4Composer(videoPathOriginal ?: EMPTY, videoPath)
                .size(getWindowWidth(requireActivity()),
                    (((sizeA?.height?.times(2))?.minus(130))?:1080)
                )
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

                        viewAndShareViewModel.submitPractise(videoPath, specialId ?: EMPTY)

                        lifecycleScope.launch(Dispatchers.Main) {
                            binding.materialCardView.isClickable = true
                            binding.progressBar.visibility = View.GONE
                            sharableVideoUrl = videoPath
                            showIntroVideoUi(videoPath)
                        }
                        viewAndShareViewModel.updateUserRecordVideo(specialId?: EMPTY, EMPTY)

                    }

                    override fun onCanceled() {
                    }

                    override fun onFailed(exception: java.lang.Exception?) {
                        Log.e("Sagar", "onFailed: $exception")
                    }

                })
                .start()
        } catch (ex: Exception) {
        }
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
        try {
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
        } catch (ex: Exception) {

        }
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
        try {
            val i = Intent(activity, SpecialPracticeActivity::class.java)
            i.putExtra(SPECIAL_ID, specialId)
            startActivity(i)
            requireActivity().overridePendingTransition(0, 0)
            requireActivity().finish()
        } catch (ex: Exception) {
        }
    }

    fun getVideoResolution(path: String?): Size? {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        val width = Integer.valueOf(
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        )
        val height = Integer.valueOf(
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        )
        retriever.release()
        val rotation: Int = getVideoRotation(path)
        return if (rotation == 90 || rotation == 270) {
            Size(height, width)
        } else Size(width, height)
    }

    fun getVideoRotation(videoFilePath: String?): Int {
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(videoFilePath)
        val orientation = mediaMetadataRetriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
        )
        return Integer.valueOf(orientation)
    }


    fun getWindowWidth(context: Context): Int {
        val disp =
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val size = Point()
        disp.getSize(size)
        return size.x
    }
}