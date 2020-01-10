package com.joshtalks.joshskills.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.crashlytics.android.Crashlytics
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.notification.HAS_NOTIFICATION
import com.joshtalks.joshskills.core.notification.NOTIFICATION_ID
import com.joshtalks.joshskills.repository.local.model.InstallReferrerModel
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import com.joshtalks.joshskills.ui.profile.CropImageActivity
import com.joshtalks.joshskills.ui.profile.ProfileActivity
import com.joshtalks.joshskills.ui.profile.SOURCE_IMAGE
import com.joshtalks.joshskills.ui.sign_up_old.OnBoardActivity
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import java.net.URLDecoder
import java.util.*

const val HELP_ACTIVITY_REQUEST_CODE = 9010

abstract class BaseActivity : AppCompatActivity() {

    protected val TAG: String = javaClass.simpleName
    private lateinit var referrerClient: InstallReferrerClient

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { ViewPumpContextWrapper.wrap(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor =
                ContextCompat.getColor(applicationContext, R.color.status_bar_color)
        }
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        AppObjectController.screenHeight = displayMetrics.heightPixels
        AppObjectController.screenWidth = displayMetrics.widthPixels
        initUserForCrashlytics()
        installReferrer()
    }


    fun getIntentForState(): Intent? {
        var intent: Intent? = null
        if (User.getInstance().token == null) {
            intent = Intent(this, OnBoardActivity::class.java)
        }/* else if (User.getInstance().dateOfBirth == null || User.getInstance().dateOfBirth.isNullOrEmpty()) {
            intent = Intent(this, ProfileActivity::class.java)
        }*/ else {
            intent = getInboxActivityIntent()
        }
        return intent.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun getCroppingActivity(filePath: String): Intent {
        return Intent(this, CropImageActivity::class.java).apply {
            putExtra(SOURCE_IMAGE, filePath)
        }
    }

    fun getInboxActivityIntent(): Intent {
        return Intent(this, InboxActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    protected fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivityForResult(intent, 101)
    }

    protected fun processIntent(mIntent: Intent?) {
        try {
            if (!(mIntent == null || !mIntent.hasExtra(HAS_NOTIFICATION) || !mIntent.hasExtra(
                    NOTIFICATION_ID
                ) || !mIntent.getStringExtra(NOTIFICATION_ID).isNullOrEmpty().not())
            ) {
                EngagementNetworkHelper.clickNotification(
                    mIntent.getStringExtra(
                        NOTIFICATION_ID
                    )
                )
            }
        } catch (ex: Exception) {
        }
    }

    private fun initUserForCrashlytics() {
        try {
            Crashlytics.getInstance().core.setUserName(User.getInstance().firstName)
            Crashlytics.getInstance().core.setUserEmail(User.getInstance().email)
            Crashlytics.getInstance()
                .core.setUserIdentifier(User.getInstance().phoneNumber + "$" + Mentor.getInstance().getId())
        } catch (ex: Exception) {
        }
    }

    fun callHelpLine() {
        AppAnalytics.create(AnalyticsEvent.CLICK_HELPLINE_SELECTED.NAME).push()
        Utils.call(this, AppObjectController.getFirebaseRemoteConfig().getString("helpline_number"))
    }

    private fun installReferrer() {

        val obj = InstallReferrerModel.getPrefObject()
        if (obj == null) {
            referrerClient = InstallReferrerClient.newBuilder(this).build()
            referrerClient.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {

                    if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                        try {
                            val response = referrerClient.installReferrer

                            val rawReferrerString =
                                URLDecoder.decode(response.installReferrer, "UTF-8")
                            val referrerMap = HashMap<String, String>()
                            val referralParams = rawReferrerString.split("&").toTypedArray()
                            for (referrerParam in referralParams) {
                                if (!TextUtils.isEmpty(referrerParam)) {
                                    var splitter = "="
                                    if (!referrerParam.contains("=") && referrerParam.contains("-")) {
                                        splitter = "-"
                                    }
                                    val keyValue = referrerParam.split(splitter).toTypedArray()
                                    if (keyValue.size > 1) { // To make sure that there is one key value pair in referrer
                                        referrerMap[URLDecoder.decode(keyValue[0], "UTF-8")] =
                                            URLDecoder.decode(keyValue[1], "UTF-8")
                                    }
                                }
                            }


                            val installReferrerModel = InstallReferrerModel()
                            installReferrerModel.otherInfo = referrerMap
                            installReferrerModel.utmMedium = referrerMap["utm_medium"]
                            installReferrerModel.utmSource = referrerMap["utm_source"]
                            installReferrerModel.installOn =
                                (response.referrerClickTimestampSeconds * 1000)
                            InstallReferrerModel.update(installReferrerModel)
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }

                        referrerClient.endConnection()
                    }
                }

                override fun onInstallReferrerServiceDisconnected() {
                    // Try to restart the connection on the next request to
                    // Google Play by calling the startConnection() method.
                }
            })
        }
    }

    protected fun isUserHavePersonalDetails(): Boolean {
        return !User.getInstance().dateOfBirth.isNullOrEmpty()
    }

    protected fun getPersonalDetailsActivityIntent(): Intent {
        return Intent(this, ProfileActivity::class.java)
    }



}