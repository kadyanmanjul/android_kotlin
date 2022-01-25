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
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.ActivityReferralBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import com.muddzdev.styleabletoast.StyleableToast
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.Defines
import io.branch.referral.util.LinkProperties
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


const val REFERRAL_EARN_AMOUNT_KEY = "REFERRAL_EARN_AMOUNT"
const val REFERRAL_SHARE_TEXT_KEY = "REFERRAL_SHARE_TEXT"
const val REFERRAL_SHARE_TEXT_SHARABLE_VIDEO = "REFERRAL_SHARE_TEXT_SHARABLE_VIDEO"
const val REFERRAL_IMAGE_URL_KEY = "REFERRAL_IMAGE_URL"
const val VIDEO_URL = "https://www.youtube.com/watch?v=CMZohcIMQfc "
const val SHARE_DOMAIN = "SHARE_DOMAIN"
const val REPLACE_HOLDER = "****"
const val REFERRAL_AMOUNT_HOLDER = "**"
const val DRAWABLE_RIGHT = 2
const val USER_SHARE_SHORT_URL = "user_share_url"
const val FROM_CLASS = "parent_class"
const val WHATSAPP_PACKAGE_STRING = "com.whatsapp"

class ReferralActivity : BaseActivity() {
    companion object {
        @JvmStatic
        fun startReferralActivity(context: Activity, className: String = "") {
            Intent(context, ReferralActivity::class.java).apply {
                putExtra(FROM_CLASS, className)
            }.run {
                context.startActivity(this)
            }
        }
    }

    private lateinit var activityReferralBinding: ActivityReferralBinding
    private val viewModel: ReferralViewModel by lazy {
        ViewModelProvider(this).get(ReferralViewModel::class.java)
    }
    private var userReferralCode: String = EMPTY

    //    private var userReferralURL: String = EMPTY
    var flowFrom: String? = null
    var referralTimestamp: Long? = null

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
        getDeepLinkAndInviteFriends(WHATSAPP_PACKAGE_STRING)
    }

    fun getDeepLinkAndInviteFriends(packageString: String? = null) {
        referralTimestamp = System.currentTimeMillis()
        val branchUniversalObject = BranchUniversalObject()
            .setCanonicalIdentifier(userReferralCode.plus(referralTimestamp))
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
                userReferralCode.plus(referralTimestamp)
            )

        branchUniversalObject
            .generateShortUrl(this, lp) { url, error ->
                if (error == null)
                    inviteFriends(
                        packageString = packageString,
                        dynamicLink = url
                    )
                else
                    inviteFriends(
                        packageString = packageString,
                        dynamicLink = if (PrefManager.hasKey(USER_SHARE_SHORT_URL))
                            PrefManager.getStringValue(USER_SHARE_SHORT_URL)
                        else
                            getAppShareUrl()
                    )
            }
        /* val domain = AppObjectController.getFirebaseRemoteConfig().getString(SHARE_DOMAIN)
         Firebase.dynamicLinks.shortLinkAsync(ShortDynamicLink.Suffix.SHORT) {
             domainUriPrefix = domain
             link = Uri.parse("https://joshskill.app.link")
 //            link =
 //                Uri.parse(
 //                    "https://joshskill.app.link/" +
 //                            "?apn=${application.packageName}" +
 //                            "&link=https://joshskill.app.link/" +
 //                            "&source=$userReferralCode" +
 //                            "&medium=${
 //                                userReferralCode.plus(
 //                                    System.currentTimeMillis()
 //                                )
 //                            }&campaign=referral"
 //                )
             androidParameters(BuildConfig.APPLICATION_ID) {
                 minimumVersion = 69
             }
             googleAnalyticsParameters {
                 source = userReferralCode.plus(System.currentTimeMillis())
                 medium = "Mobile"
                 campaign = "user_referer"
             }

         }.addOnSuccessListener { result ->
             Log.e(TAG, "getDeepLinkAndInviteFriends: ${result.shortLink}")
             Log.e(TAG, "getDeepLinkAndInviteFriends: ${result.previewLink}")
             result.warnings.forEach {
                 Log.w(TAG, "getDeepLinkAndInviteFriends: Warning${it.message}")
             }
             result.shortLink?.let {
                 try {
                     if (it.toString().isNotEmpty()) {
                         if (PrefManager.hasKey(USER_SHARE_SHORT_URL).not())
                             PrefManager.put(USER_SHARE_SHORT_URL, it.toString())
                         inviteFriends(packageString = packageString, dynamicLink = it.toString())
                     } else
                         inviteFriends(
                             packageString = packageString,
                             dynamicLink = if (PrefManager.hasKey(USER_SHARE_SHORT_URL))
                                 PrefManager.getStringValue(USER_SHARE_SHORT_URL)
                             else
                                 getAppShareUrl()
                         )
                 } catch (ex: Exception) {
                     ex.printStackTrace()
                 }
             }
         }.addOnFailureListener {
             it.printStackTrace()
             inviteFriends(
                 packageString = packageString,
                 dynamicLink = if (PrefManager.hasKey(USER_SHARE_SHORT_URL))
                     PrefManager.getStringValue(USER_SHARE_SHORT_URL)
                 else
                     getAppShareUrl()
             )
         }*/
    }

    fun inviteFriends(packageString: String? = null, dynamicLink: String) {
        var referralText = VIDEO_URL.plus("\n").plus(
            AppObjectController.getFirebaseRemoteConfig().getString(REFERRAL_SHARE_TEXT_KEY)
        )
        val refAmount =
            AppObjectController.getFirebaseRemoteConfig().getLong(REFERRAL_EARN_AMOUNT_KEY)
                .toString()
        referralText = referralText.replace(REPLACE_HOLDER, userReferralCode)
        referralText = referralText.replace(REFERRAL_AMOUNT_HOLDER, refAmount)

        referralText = referralText.plus("\n").plus(dynamicLink)
        viewModel.getDeepLink(
            dynamicLink,
            userReferralCode.plus(referralTimestamp ?: System.currentTimeMillis())
        )
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
            showToast(getString(R.string.whatsApp_not_installed))
        }
        if (packageString == WHATSAPP_PACKAGE_STRING) {
            viewModel.saveImpression(IMPRESSION_REFER_VIA_WHATSAPP_CLICKED)
        } else {
            viewModel.saveImpression(IMPRESSION_REFER_VIA_OTHER_CLICKED)
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
        viewModel.saveImpression(IMPRESSION_REFERRAL_CODE_COPIED)
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
