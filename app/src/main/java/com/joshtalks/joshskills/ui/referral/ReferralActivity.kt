package com.joshtalks.joshskills.ui.referral

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.databinding.DataBindingUtil
import com.google.firebase.dynamiclinks.ShortDynamicLink
import com.google.firebase.dynamiclinks.ktx.androidParameters
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.dynamiclinks.ktx.googleAnalyticsParameters
import com.google.firebase.dynamiclinks.ktx.shortLinkAsync
import com.google.firebase.ktx.Firebase
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.ActivityReferralBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import com.muddzdev.styleabletoast.StyleableToast
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


const val REFERRAL_EARN_AMOUNT_KEY = "REFERRAL_EARN_AMOUNT"
const val REFERRAL_SHARE_TEXT_KEY = "REFERRAL_SHARE_TEXT"
const val REFERRAL_SHARE_TEXT_KEY2 = "referral_text_video"
const val REFERRAL_IMAGE_URL_KEY = "REFERRAL_IMAGE_URL"
const val VIDEO_URL = "https://www.youtube.com/watch?v=CMZohcIMQfc "
const val SHARE_DOMAIN = "SHARE_DOMAIN"
const val REPLACE_HOLDER = "****"
const val REFERRAL_AMOUNT_HOLDER = "**"
const val DRAWABLE_RIGHT = 2
const val USER_SHARE_SHORT_URL = "user_share_url"
const val FROM_CLASS = "parent_class"

class ReferralActivity : BaseActivity() {
    companion object {
        fun startReferralActivity(context: Activity, className: String = "") {
            Intent(context, ReferralActivity::class.java).apply {
                putExtra(FROM_CLASS, className)
            }.run {
                context.startActivity(this)
            }
        }
    }

    private lateinit var activityReferralBinding: ActivityReferralBinding
    private var userReferralCode: String = EMPTY
    private var userReferralURL: String = EMPTY
    var flowFrom: String? = null

    @ExperimentalUnsignedTypes
    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)
        activityReferralBinding = DataBindingUtil.setContentView(this, R.layout.activity_referral)
        activityReferralBinding.lifecycleOwner = this
        activityReferralBinding.handler = this
        userReferralCode = Mentor.getInstance().referralCode
        activityReferralBinding.tvReferralCode.text = userReferralCode
        initView()
        val domain = AppObjectController.getFirebaseRemoteConfig().getString(SHARE_DOMAIN)

        if (PrefManager.hasKey(USER_SHARE_SHORT_URL).not()) {
            userReferralURL = PrefManager.getStringValue(USER_SHARE_SHORT_URL)

        }
        Firebase.dynamicLinks.shortLinkAsync(ShortDynamicLink.Suffix.SHORT) {
            link = Uri.parse("https://joshskill.app.link")
            domainUriPrefix = domain
            androidParameters(BuildConfig.APPLICATION_ID) {
                minimumVersion = 69
            }
            googleAnalyticsParameters {
                source = userReferralCode
                medium = "Mobile"
                campaign = "user_referer"
            }

        }.addOnSuccessListener { result ->
            result?.shortLink?.let {
                try {
                    if (it.toString().isNotEmpty()) {
                        PrefManager.put(USER_SHARE_SHORT_URL, it.toString())
                        userReferralURL = it.toString()

                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
            .addOnFailureListener {
                it.printStackTrace()

            }
        if (intent.hasExtra(FROM_CLASS)) {
            flowFrom = intent.getStringExtra(FROM_CLASS)
        }
        AppAnalytics
            .create(AnalyticsEvent.REFERRAL_PAGE.NAME)
            .addUserDetails()
            .addBasicParam()
            .addParam(AnalyticsEvent.REFERRAL_CODE.name, userReferralCode)
            .addParam(AnalyticsEvent.FLOW_FROM_PARAM.name, flowFrom)
            .push()
    }


    @ExperimentalUnsignedTypes
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun initView() {
        val refAmount =
            AppObjectController.getFirebaseRemoteConfig().getLong(REFERRAL_EARN_AMOUNT_KEY)
                .toString()


        val referralScreenVersion =
            AppObjectController.getFirebaseRemoteConfig().getString("referral_screen_page")

        if (referralScreenVersion == "version_1") {
            activityReferralBinding.tvHeader.text =
                getString(R.string.referral_header, refAmount, refAmount)
            activityReferralBinding.textView1.text = HtmlCompat.fromHtml(
                getString(R.string.refferal_desc1, refAmount),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            activityReferralBinding.textView2.text = HtmlCompat.fromHtml(
                getString(R.string.referral_desc2, refAmount),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        } else if (referralScreenVersion == "version_2") {
            activityReferralBinding.tvHeader.text = getString(R.string.earn_upto)
            activityReferralBinding.textView1.text = HtmlCompat.fromHtml(
                getString(R.string.refferal_desc1, refAmount),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            activityReferralBinding.textView2.text = HtmlCompat.fromHtml(
                getString(R.string.no_conditions_apply),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        }

        activityReferralBinding.ivBack.setOnClickListener {
            onBackPressed()
        }

        val mDetector = GestureDetector(this, object :
            GestureDetector.OnGestureListener {
            override fun onShowPress(e: MotionEvent?) {
                // Not Required
            }

            override fun onSingleTapUp(event: MotionEvent): Boolean {
                if (
                    event.action == MotionEvent.ACTION_UP &&
                    event.rawX.toUInt() >= ((activityReferralBinding.tvReferralCode.right - activityReferralBinding.tvReferralCode.compoundDrawables[DRAWABLE_RIGHT].bounds.width()).toUInt())
                ) {
                    copyCodeIntoClipBoard()
                }
                return true
            }

            override fun onDown(e: MotionEvent?): Boolean {
                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent?,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                return true
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent?,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                return true
            }

            override fun onLongPress(e: MotionEvent?) {
                copyCodeIntoClipBoard()
            }

        })

        val touchListener =
            View.OnTouchListener { _, event ->
                mDetector.onTouchEvent(event)
            }
        activityReferralBinding.tvReferralCode.setOnTouchListener(touchListener)
    }

    fun inviteOnlyWhatsapp() {
        inviteFriends("com.whatsapp")
    }

    fun inviteFriends(packageString: String? = null) {
        var referralText = VIDEO_URL.plus("\n").plus(
            AppObjectController.getFirebaseRemoteConfig().getString(REFERRAL_SHARE_TEXT_KEY)
        )
        val refAmount =
            AppObjectController.getFirebaseRemoteConfig().getLong(REFERRAL_EARN_AMOUNT_KEY)
                .toString()
        referralText = referralText.replace(REPLACE_HOLDER, userReferralCode)
        referralText = referralText.replace(REFERRAL_AMOUNT_HOLDER, refAmount)

        referralText = if (userReferralURL.isEmpty()) {
            referralText.plus("\n").plus(getAppShareUrl())
        } else {
            referralText.plus("\n").plus(userReferralURL)
        }

        val pm = packageManager
        try {

            val waIntent = Intent(Intent.ACTION_SEND)
            waIntent.type = "text/plain"
            if (packageString.isNullOrEmpty().not()) {
                waIntent.setPackage(packageString)
            }
            waIntent.putExtra(Intent.EXTRA_TEXT, referralText)
            startActivity(Intent.createChooser(waIntent, "Share with"))
            AppAnalytics
                .create(AnalyticsEvent.REFERRAL_SCREEN_ACTION.NAME)
                .addParam(AnalyticsEvent.ACTION.NAME, AnalyticsEvent.SHARE_ON_WHATSAPP.NAME)
                .addUserDetails()
                .addParam(AnalyticsEvent.REFERRAL_CODE.NAME, userReferralCode)
                .push()

        } catch (e: PackageManager.NameNotFoundException) {
            Toast.makeText(this, "WhatsApp not Installed", Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun getBitmapFromView(bmp: Bitmap?): File? {
        val file = File(this.cacheDir, System.currentTimeMillis().toString() + ".jpg")
        try {
            val out = FileOutputStream(file)
            bmp?.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.close()
            return file

        } catch (e: IOException) {
            e.printStackTrace()
        }
        return file
    }


    private fun getAppShareUrl(): String {
        return "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID + "&referrer=utm_source%3D$userReferralCode"
    }

    @Synchronized
    private fun copyCodeIntoClipBoard() {
        AppObjectController.uiHandler.removeCallbacksAndMessages(null)
        val cManager =
            application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val cData = ClipData.newPlainText("text", userReferralCode)
        cManager.setPrimaryClip(cData)
        StyleableToast.Builder(this@ReferralActivity).gravity(Gravity.CENTER)
            .text(getString(R.string.copy_code)).cornerRadius(16).length(Toast.LENGTH_LONG)
            .solidBackground().show()
        AppAnalytics
            .create(AnalyticsEvent.REFERRAL_SCREEN_ACTION.NAME)
            .addParam(AnalyticsEvent.ACTION.NAME, AnalyticsEvent.CODE_COPIED.NAME)
            .addUserDetails()
            .addBasicParam()
            .addParam(AnalyticsEvent.REFERRAL_CODE.name, userReferralCode)
            .push()
    }

    override fun onBackPressed() {
        if (intent.hasExtra(FROM_CLASS)) {
            intent.getStringExtra(FROM_CLASS)?.run {
                if (this.equals(InboxActivity::class.java.name, ignoreCase = true)) {
                    startActivity(Intent(this@ReferralActivity, InboxActivity::class.java))
                }
            }
        }
        this.finish()
        super.onBackPressed()

    }
}
