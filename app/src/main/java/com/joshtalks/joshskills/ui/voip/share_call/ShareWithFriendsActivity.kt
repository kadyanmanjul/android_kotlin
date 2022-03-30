package com.joshtalks.joshskills.ui.voip.share_call

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.abTest.CampaignKeys
import com.joshtalks.joshskills.core.abTest.GoalKeys
import com.joshtalks.joshskills.databinding.ActivityShareWithFriendsBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.referral.USER_SHARE_SHORT_URL
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.Defines
import io.branch.referral.util.LinkProperties
import java.io.File
import java.io.FileOutputStream

const val ARG_RECEIVER_NAME = "ARG_RECEIVER_NAME"
const val ARG_RECEIVER_IMAGE = "ARG_RECEIVER_IMAGE"
const val ARG_MINUTES_TALKED = "MINUTES_TALKED"
const val ARG_CALLER_CITY = "ARG_CALLER_CITY"
const val ARG_CALLER_STATE = "ARG_CALLER_STATE"
const val ARG_RECEIVER_CITY = "ARG_RECEIVER_CITY"
const val ARG_RECEIVER_STATE = "ARG_RECEIVER_STATE"
const val P2P_CALL_SHARE_TEXT = "P2P_CALL_SHARE_TEXT_"

class ShareWithFriendsActivity : AppCompatActivity() {
    private val binding by lazy<ActivityShareWithFriendsBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_share_with_friends)
    }

    private val courseId = PrefManager.getStringValue(CURRENT_COURSE_ID, false, DEFAULT_COURSE_ID)
    private val viewModel: ShareWithFriendsViewModel by lazy {
        ViewModelProvider(this).get(ShareWithFriendsViewModel::class.java)
    }

    val fragment = ShareScreenFragment()

    private var userReferralCode: String = EMPTY

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        super.onCreate(savedInstanceState)
        window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.white)

        populateViewsAndFragment()
        binding.lifecycleOwner = this
        binding.handler = this
    }

    fun sharePreviewToOtherApps() {
        userReferralCode = Mentor.getInstance().referralCode
        val branchUniversalObject = BranchUniversalObject()
            .setCanonicalIdentifier(userReferralCode.plus(System.currentTimeMillis()))
            .setTitle("Invite Friend")
            .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
            .setLocalIndexMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
        val lp = LinkProperties()
            .setChannel(userReferralCode)
            .setFeature("sharing")
            .setCampaign("referral")
            .addControlParameter(Defines.Jsonkey.ReferralCode.key, userReferralCode)
            .addControlParameter(Defines.Jsonkey.UTMCampaign.key, "referral")
            .addControlParameter(
                Defines.Jsonkey.UTMMedium.key,
                userReferralCode.plus(System.currentTimeMillis())
            )
        branchUniversalObject
            .generateShortUrl(this, lp) { url, error ->
                if (error == null)
                    inviteFriends(
                        dynamicLink = url
                    )
                else
                    inviteFriends(
                        dynamicLink = if (PrefManager.hasKey(USER_SHARE_SHORT_URL))
                            PrefManager.getStringValue(USER_SHARE_SHORT_URL)
                        else
                            getAppShareUrl()
                    )
            }
    }

    fun inviteFriends(dynamicLink: String) {
        var shareText = AppObjectController.getFirebaseRemoteConfig().getString(P2P_CALL_SHARE_TEXT + courseId)
        shareText = shareText.plus("\nLink: \n").plus(dynamicLink)

        viewModel.getDeepLink(
            dynamicLink,
            userReferralCode.plus(System.currentTimeMillis())
        )
        val view = fragment.getShareScreen()
        val bitmapCreated = getBitMapFromView(view)
        try {
            saveBitMapToFile(bitmapCreated)
           // shareFile(bitmapCreated, dynamicLink)
            shareFile(bitmapCreated, shareText)
            viewModel.postGoal(GoalKeys.P2P_IS_GT_20MIN.name, CampaignKeys.P2P_IMAGE_SHARING.name)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getAppShareUrl(): String {
        return "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID + "&referrer=utm_source%3D$userReferralCode"
    }

    private fun populateViewsAndFragment() {

        val mentor = Mentor.getInstance()

        binding.apply {
            tvGreatJob.text = getString(R.string.great_job, mentor.getUser()!!.firstName)
            tvMinutes.text = getString(
                R.string.you_practiced_in_english_prompt,
                intent.getIntExtra(ARG_MINUTES_TALKED, 0).toString()
            )

            tvCallerDetails.text = getString(
                R.string.caller_details,
                mentor.getUser()!!.firstName,
                (intent.getStringExtra(ARG_CALLER_CITY) ?: "").trim(),
                (intent.getStringExtra(ARG_CALLER_STATE) ?: "").trim()
            )

            tvReceiverDetails.text = getString(
                R.string.caller_details,
                intent.getStringExtra(ARG_RECEIVER_NAME),
                (intent.getStringExtra(ARG_RECEIVER_CITY) ?: "").trim(),
                (intent.getStringExtra(ARG_RECEIVER_STATE) ?: "").trim()
            )

            setImageForCallers(
                callerImage,
                mentor.getUser()!!.photo
            )

            setImageForCallers(
                receiverImage,
                intent.getStringExtra(ARG_RECEIVER_IMAGE)
            )
            val cDetails = tvCallerDetails.text
            val rDetails = tvReceiverDetails.text
            ShareScreenFragment.setArguments(
                fragment = fragment,
                minutesTalked = intent.getIntExtra(ARG_MINUTES_TALKED, 0).toString(),
                callerImage = mentor.getUser()!!.photo,
                receiverImage = intent.getStringExtra(ARG_RECEIVER_IMAGE),
                callerDetails = cDetails.toString(),
                receiverDetails = rDetails.toString()
            )
        }

        supportFragmentManager.beginTransaction().replace(R.id.share_fragment_container, fragment)
            .commit()

    }

    private fun setImageForCallers(image: ImageView, imageUrl: String?) {
        if (!imageUrl.isNullOrEmpty()) {
            image.setRoundImage(imageUrl)
        }
    }

    companion object {
        fun startShareWithFriendsActivity(
            activity: Activity,
            receiverName: String,
            receiverImage: String,
            minutesTalked: Int,
            callerState: String?,
            callerCity: String?,
            receiverState: String?,
            receiverCity: String?
        ) {
            Intent(activity, ShareWithFriendsActivity::class.java).apply {
                putExtra(ARG_MINUTES_TALKED, minutesTalked)
                putExtra(ARG_RECEIVER_NAME, receiverName)
                putExtra(ARG_RECEIVER_IMAGE, receiverImage)
                putExtra(ARG_CALLER_STATE, callerState)
                putExtra(ARG_CALLER_CITY, callerCity)
                putExtra(ARG_RECEIVER_STATE, receiverState)
                putExtra(ARG_RECEIVER_CITY, receiverCity)
            }.run {
                activity.startActivity(this)
            }
        }
    }

    private fun getBitMapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun saveBitMapToFile(bitmap: Bitmap) {
        val file = File(
            applicationContext.externalCacheDir,
            File.separator + "image that you want to share"
        )
        val fOut = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut)
        fOut.flush()
        fOut.close()
        file.setReadable(true, false)
    }

    private fun shareFile(bitmap: Bitmap, shareText: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.apply {
            type = "image/*"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK

            val path =
                MediaStore.Images.Media.insertImage(contentResolver, bitmap, "Title", null)
            putExtra(Intent.EXTRA_STREAM, Uri.parse(path))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,shareText)
        }.run {
            startActivity(Intent.createChooser(this, "Share image via"))
        }
    }
}
