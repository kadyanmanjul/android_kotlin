package com.joshtalks.joshskills.ui.referral

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.ShortDynamicLink
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.databinding.ActivityReferralBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
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

class ReferralActivity : BaseActivity() {
    companion object {
        fun startReferralActivity(context: Activity) {
            Intent(context, ReferralActivity::class.java).apply {
            }.run {
                context.startActivity(this)
            }
        }
    }

    private lateinit var activityReferralBinding: ActivityReferralBinding
    private var userReferralCode: String = EMPTY
    private var userReferralURL: String = EMPTY


    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor =
                ContextCompat.getColor(applicationContext, R.color.white)
        }

        activityReferralBinding = DataBindingUtil.setContentView(this, R.layout.activity_referral)
        activityReferralBinding.lifecycleOwner = this
        activityReferralBinding.handler = this
        userReferralCode = Mentor.getInstance().referralCode
        activityReferralBinding.tvReferralCode.text = userReferralCode

        initView()

        val refAmount =
            AppObjectController.getFirebaseRemoteConfig().getLong(REFERRAL_EARN_AMOUNT_KEY)
                .toString()
        activityReferralBinding.referralTv.text =
            getString(R.string.referral_content, refAmount, refAmount)
        activityReferralBinding.referralEarnTv.text =
            getString(R.string.referral_amount_title, refAmount)

        val baseUrl = Uri.parse(getAppShareUrl())
        val domain = AppObjectController.getFirebaseRemoteConfig().getString(SHARE_DOMAIN)

        if (PrefManager.hasKey(USER_SHARE_SHORT_URL).not()) {
            userReferralURL = PrefManager.getStringValue(USER_SHARE_SHORT_URL)

        }
        FirebaseDynamicLinks.getInstance()
            .createDynamicLink()
            .setLink(baseUrl)
            .setDomainUriPrefix(domain)
            // .setIosParameters(DynamicLink.IosParameters.Builder("com.joshtalks.joshskills").build())
            .setAndroidParameters(DynamicLink.AndroidParameters.Builder(BuildConfig.APPLICATION_ID).build())
            .setAndroidParameters(DynamicLink.AndroidParameters.Builder().build())
            .buildShortDynamicLink(ShortDynamicLink.Suffix.SHORT)

            .addOnSuccessListener { result ->
                result?.shortLink?.let {
                    try {
                        if (it.toString().isNotEmpty()) {
                            PrefManager.put(USER_SHARE_SHORT_URL, it.toString())
                            userReferralURL = it.toString()

                        }
                    } catch (ex: Exception) {

                    }
                }
            }.addOnFailureListener {
                it.printStackTrace()
            }
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        activityReferralBinding.ivBack.setOnClickListener {
            this@ReferralActivity.finish()
        }

        activityReferralBinding.tvReferralCode.setOnLongClickListener {
            copyCodeIntoClipBoard()
            return@setOnLongClickListener true
        }

        activityReferralBinding.tvReferralCode.setOnTouchListener { _, event ->
            try {
                if (event.action == MotionEvent.ACTION_UP) {
                    if (event.rawX >= (activityReferralBinding.tvReferralCode.right - activityReferralBinding.tvReferralCode.compoundDrawables[DRAWABLE_RIGHT].bounds.width())) {
                        copyCodeIntoClipBoard()
                        return@setOnTouchListener true
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            return@setOnTouchListener false
        }
    }

    fun inviteFriends() {
        var referralText =
            AppObjectController.getFirebaseRemoteConfig().getString(REFERRAL_SHARE_TEXT_KEY)
        val refAmount =
            AppObjectController.getFirebaseRemoteConfig().getLong(REFERRAL_EARN_AMOUNT_KEY)
                .toString()
        referralText = referralText.replace(REPLACE_HOLDER, userReferralCode)
        referralText = referralText.replace(REFERRAL_AMOUNT_HOLDER, refAmount)

        if (userReferralURL.isEmpty()) {
            referralText = referralText.plus("\n").plus(getAppShareUrl())
        } else {
            referralText =
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
                    if (Build.VERSION.SDK_INT >= 24) {
                        try {
                            sendIntent.putExtra(
                                Intent.EXTRA_STREAM,
                                FileProvider.getUriForFile(
                                    this@ReferralActivity,
                                    BuildConfig.APPLICATION_ID + ".provider",
                                    getBitmapFromView(resource)!!
                                )
                            )
                            sendIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        } catch (ignore: Exception) {
                            ignore.printStackTrace()
                            sendIntent.putExtra(
                                Intent.EXTRA_STREAM,
                                getBitmapFromView(resource)!!
                            )
                        }
                    } else {
                        sendIntent.putExtra(
                            Intent.EXTRA_STREAM,
                            Uri.fromFile(getBitmapFromView(resource)!!)
                        )
                    }

                    sendIntent.type = "image/*"
                    sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val shareIntent = Intent.createChooser(sendIntent, getString(R.string.app_name))
                    startActivity(shareIntent)
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
        return "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID + "&referrer=$userReferralCode"
    }

    private fun copyCodeIntoClipBoard() {
        val cManager =
            application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val cData = ClipData.newPlainText("text", userReferralCode)
        cManager.setPrimaryClip(cData)
        StyleableToast.Builder(this@ReferralActivity).gravity(Gravity.CENTER)
            .text(getString(R.string.copy_code)).cornerRadius(16).length(Toast.LENGTH_LONG)
            .solidBackground().show()
    }
}
