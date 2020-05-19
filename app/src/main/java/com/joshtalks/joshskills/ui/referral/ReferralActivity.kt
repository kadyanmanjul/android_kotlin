package com.joshtalks.joshskills.ui.referral

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.text.HtmlCompat
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.firebase.dynamiclinks.ShortDynamicLink
import com.google.firebase.dynamiclinks.ktx.androidParameters
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.dynamiclinks.ktx.googleAnalyticsParameters
import com.google.firebase.dynamiclinks.ktx.shortLinkAsync
import com.google.firebase.ktx.Firebase
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.databinding.ActivityReferralBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import com.muddzdev.styleabletoast.StyleableToast
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


const val REFERRAL_EARN_AMOUNT_KEY = "REFERRAL_EARN_AMOUNT"
const val REFERRAL_SHARE_TEXT_KEY = "REFERRAL_SHARE_TEXT"
const val REFERRAL_IMAGE_URL_KEY = "REFERRAL_IMAGE_URL"
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
                "Earn <strong><u>₹$refAmount</u></strong> in your account <br/>for every friend who <br/> joins a course!",
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            activityReferralBinding.textView2.text = HtmlCompat.fromHtml(
                "Your friend also gets <br/> <strong>₹$refAmount OFF</strong> on their first course",
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        } else if (referralScreenVersion == "version_2") {
            activityReferralBinding.tvHeader.text = "Earn upto ₹10,000 "
            activityReferralBinding.textView1.text = HtmlCompat.fromHtml(
                "Earn <strong><u>₹$refAmount</u></strong> in your account <br/>for every friend who <br/> joins a course!",
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            activityReferralBinding.textView2.text = HtmlCompat.fromHtml(
                "No Conditions Apply",
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
        AppAnalytics
            .create(AnalyticsEvent.SHARE_ON_WHATSAPP.NAME)
            .addUserDetails()
            .addParam(AnalyticsEvent.REFERRAL_CODE.NAME, userReferralCode)
            .push()
    }

    fun inviteFriends(packageString: String? = null) {
        WorkMangerAdmin.referralEventTracker(REFERRAL_EVENT.CLICK_ON_SHARE)
        var referralText =
            AppObjectController.getFirebaseRemoteConfig().getString(REFERRAL_SHARE_TEXT_KEY)
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

        val referralImageUrl =
            AppObjectController.getFirebaseRemoteConfig().getString(REFERRAL_IMAGE_URL_KEY)

        Glide.with(AppObjectController.joshApplication)
            .asBitmap()
            .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
            .load(referralImageUrl)
            .listener(object : RequestListener<Bitmap> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Bitmap?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {

                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, referralText)
                    }
                    sendIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    sendIntent.flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    try {
                        sendIntent.putExtra(
                            Intent.EXTRA_STREAM,
                            FileProvider.getUriForFile(
                                this@ReferralActivity,
                                BuildConfig.APPLICATION_ID + ".provider",
                                getBitmapFromView(resource)!!
                            )
                        )
                    } catch (ignore: Exception) {
                        ignore.printStackTrace()
                        sendIntent.putExtra(
                            Intent.EXTRA_STREAM,
                            Uri.fromFile(getBitmapFromView(resource)!!)
                        )
                    }

                    sendIntent.type = "image/*"
                    sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    if (packageString.isNullOrEmpty()) {
                        val shareIntent =
                            Intent.createChooser(sendIntent, getString(R.string.app_name))
                        startActivity(shareIntent)
                    } else {
                        try {
                            sendIntent.setPackage(packageString)
                            startActivity(sendIntent)
                            AppAnalytics
                                .create(AnalyticsEvent.SHARE_ON_WHATSAPP.NAME)
                                .addUserDetails()
                                .addParam(AnalyticsEvent.REFERRAL_CODE.NAME, userReferralCode)
                                .push()
                            return true
                        } catch (ex: ActivityNotFoundException) {
                            val shareIntent =
                                Intent.createChooser(sendIntent, getString(R.string.app_name))
                            startActivity(shareIntent)
                        }
                    }
                    AppAnalytics
                        .create(AnalyticsEvent.SHARE_ON_ALL.NAME)
                        .addUserDetails()
                        .addParam(AnalyticsEvent.REFERRAL_CODE.NAME, userReferralCode)
                        .push()
                    return false
                }
            }
            ).submit()
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
        AppObjectController.uiHandler.postDelayed({
            WorkMangerAdmin.referralEventTracker(REFERRAL_EVENT.LONG_PRESS_CODE)
        }, 1200)
        AppAnalytics
            .create(AnalyticsEvent.CODE_COPIED.NAME)
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
