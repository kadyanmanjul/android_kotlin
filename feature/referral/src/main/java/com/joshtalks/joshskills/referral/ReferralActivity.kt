package com.joshtalks.joshskills.referral

import android.annotation.SuppressLint
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
import com.joshtalks.joshskills.common.core.*
import com.joshtalks.joshskills.common.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.common.core.analytics.AppAnalytics
import com.joshtalks.joshskills.common.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.common.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.referral.databinding.ActivityReferralBinding
import com.joshtalks.joshskills.common.repository.local.model.Mentor
import com.joshtalks.joshskills.common.ui.inbox.InboxActivity
import com.joshtalks.joshskills.common.util.DeepLinkUtil
import com.muddzdev.styleabletoast.StyleableToast
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

const val REFERRAL_WHATAPP_MESSAGE = "REFERRAL_WHATAPP_MESSAGE"
const val REFERRAL_EARN_AMOUNT_KEY = "REFERRAL_EARN_AMOUNT"
const val REPLACE_HOLDER = "****"
const val REFERRAL_AMOUNT_HOLDER = "**"
const val DRAWABLE_RIGHT = 2
const val FROM_CLASS = "parent_class"
const val WHATSAPP_PACKAGE_STRING = "com.whatsapp"

class ReferralActivity : BaseActivity() {
    companion object {
        fun openReferralActivity(contract: ReferralContract, context: Context) {
            context.startActivity(
                Intent(context, ReferralActivity::class.java).apply {
                    putExtra(FROM_CLASS, contract.flowFrom)
                    putExtra(NAVIGATOR, contract.navigator)
                    contract.flags.forEach { addFlags(it) }
                }
            )
        }
    }

    private lateinit var activityReferralBinding: ActivityReferralBinding
    private val viewModel: ReferralViewModel by lazy {
        ViewModelProvider(this).get(ReferralViewModel::class.java)
    }
    private var userReferralCode: String = EMPTY

    //    private var userReferralURL: String = EMPTY
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
                getString(R.string.referral_header)
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
            override fun onShowPress(e: MotionEvent) {
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

            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onFling(
                e1: MotionEvent,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                return true
            }

            override fun onScroll(
                e1: MotionEvent,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                return true
            }

            override fun onLongPress(e: MotionEvent) {
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
        DeepLinkUtil(this)
            .setReferralCode(Mentor.getInstance().referralCode)
            .setReferralCampaign()
            .setListener(object : DeepLinkUtil.OnDeepLinkListener {
                override fun onDeepLinkCreated(deepLink: String) {
                    inviteFriends(
                        packageString = packageString,
                        dynamicLink = deepLink
                    )
                }
            })
            .build()
    }

    fun inviteFriends(packageString: String? = null, dynamicLink: String) {
        var referralText = if(PrefManager.getStringValue(CURRENT_COURSE_ID) == DEFAULT_COURSE_ID || PrefManager.getBoolValue(IS_HINDI_SELECTED) || PrefManager.getBoolValue(IS_HINGLISH_SELECTED))
            AppObjectController.getFirebaseRemoteConfig().getString(REFERRAL_WHATAPP_MESSAGE)
        else
            getString(R.string.referral_share_text)
        val refAmount =
            AppObjectController.getFirebaseRemoteConfig().getLong(REFERRAL_EARN_AMOUNT_KEY)
                .toString()
        referralText = referralText.replace(REPLACE_HOLDER, userReferralCode)
        referralText = referralText.replace(REFERRAL_AMOUNT_HOLDER, refAmount)

        referralText = referralText.plus("\n").plus(dynamicLink)
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
            MixPanelTracker.publishEvent(MixPanelEvent.SHARE_REFERRAL_WHATSAPP).push()
            viewModel.saveImpression(IMPRESSION_REFER_VIA_WHATSAPP_CLICKED)
        } else {
            MixPanelTracker.publishEvent(MixPanelEvent.SHARE_REFERRAL_OTHERS).push()
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
        return "https://play.google.com/store/apps/details?id=" + AppObjectController.applicationDetails.applicationId() + "&referrer=utm_source%3D$userReferralCode"
    }

    @Synchronized
    private fun copyCodeIntoClipBoard() {
        MixPanelTracker.publishEvent(MixPanelEvent.COPY_REFERRAL).push()
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
        MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
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
