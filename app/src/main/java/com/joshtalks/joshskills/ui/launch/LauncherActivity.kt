package com.joshtalks.joshskills.ui.launch

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.TelephonyManager
import com.google.firebase.perf.metrics.AddTrace
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.INSTANCE_ID
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.READ_WRITE_PERMISSION_GIVEN
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.ui.course_details.CourseDetailsActivity
import com.joshtalks.joshskills.ui.extra.CustomPermissionDialogInteractionListener
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.branch.referral.Branch
import io.branch.referral.Defines
import org.json.JSONObject
import timber.log.Timber


class LauncherActivity : CoreJoshActivity(), CustomPermissionDialogInteractionListener {

    @AddTrace(name = "LauncherActivity - onCreate", enabled = true)
    override fun onCreate(savedInstanceState: Bundle?) {
        Branch.getInstance(applicationContext).resetUserSession()
        WorkMangerAdmin.appStartWorker()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        logAppLaunchEvent(getNetworkOperatorName())
        AppObjectController.initialiseFreshChat()
    }

    @AddTrace(name = "handleIntent", enabled = true)
    private fun handleIntent() {
        Branch.sessionBuilder(this).withCallback { referringParams, error ->
            try {
                val jsonParams = referringParams ?: (Branch.getInstance().firstReferringParams
                    ?: Branch.getInstance().latestReferringParams)
                Timber.tag("BranchDeepLinkParams : ")
                    .d("referringParams = $referringParams, error = $error")
                if (error == null && jsonParams?.has(Defines.Jsonkey.AndroidDeepLinkPath.key) == true) {
                    AppObjectController.uiHandler.removeCallbacksAndMessages(null)
                    val testId = jsonParams.getString(Defines.Jsonkey.AndroidDeepLinkPath.key)
                    WorkMangerAdmin.registerUserGAID(testId)
                    val referralCode = parseReferralCode(jsonParams)
                    referralCode?.let {
                        logInstallByReferralEvent(testId, null, it)
                    }
                    navigateToCourseDetailsScreen(testId)
                    this@LauncherActivity.finish()
                }
                if (error == null && jsonParams?.has(Defines.Jsonkey.ContentType.key) == true) {
                    val exploreType = if (jsonParams.has(Defines.Jsonkey.ContentType.key)) {
                        jsonParams.getString(Defines.Jsonkey.ContentType.key)
                    } else null
                    WorkMangerAdmin.registerUserGAID(null, exploreType)
                    val referralCode = parseReferralCode(jsonParams)
                    referralCode?.let {
                        logInstallByReferralEvent(null, exploreType, it)
                    }
                }
            } catch (ex: Throwable) {
                LogException.catchException(ex)
            }
        }.withData(this.intent.data).init()

    }

    override fun onStart() {
        super.onStart()
        handleIntent()
        if (PrefManager.getBoolValue(READ_WRITE_PERMISSION_GIVEN).not()) {
            if (PermissionUtils.isStoragePermissionEnabled(this).not()) {
                getPermission()
            } else {
                initInstanceId()
            }
        } else navigateToNextScreen()
    }

    private fun getPermission() {
        PermissionUtils.storageReadAndWritePermission(this, object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                report?.areAllPermissionsGranted()?.let { isGranted ->
                    PrefManager.put(READ_WRITE_PERMISSION_GIVEN, true)
                    if (isGranted) {
                        initInstanceId()
                        return
                    } else navigateToNextScreen()
                    if (report.isAnyPermissionPermanentlyDenied) {
                        PermissionUtils.permissionPermanentlyDeniedDialog(this@LauncherActivity)
                        return
                    }
                }
            }

            override fun onPermissionRationaleShouldBeShown(
                permissions: MutableList<PermissionRequest>?,
                token: PermissionToken?
            ) {
                token?.continuePermissionRequest()
            }
        })
    }

    private fun initInstanceId() {
        val instanceId = AppDirectory.readFromFile(AppDirectory.getInstanceIdKeyFile())
        if (instanceId.isNullOrBlank())
            writeInstanceIdInFile(PrefManager.getStringValue(INSTANCE_ID, true))
        else PrefManager.put(INSTANCE_ID, instanceId, true)
        navigateToNextScreen()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        handleIntent()
    }

    private fun writeInstanceIdInFile(instanceId: String) {
        AppDirectory.writeToFile(instanceId, AppDirectory.getInstanceIdKeyFile())
    }

    override fun onStop() {
        super.onStop()
        AppObjectController.uiHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        AppAnalytics.create(AnalyticsEvent.APP_LAUNCHED.NAME).endSession()
        super.onDestroy()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        this.finishAndRemoveTask()
    }

    override fun navigateToNextScreen() {
        AppObjectController.uiHandler.postDelayed({
            val intent = getIntentForState()
            startActivity(intent)
            this@LauncherActivity.finish()
        }, 2500)
    }

    private fun navigateToCourseDetailsScreen(testId: String) {
        CourseDetailsActivity.startCourseDetailsActivity(
            this,
            testId.split("_")[1].toInt(),
            this@LauncherActivity.javaClass.simpleName,
            arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        )
    }

    private fun logInstallByReferralEvent(
        testId: String?,
        exploreType: String?,
        referralCode: String
    ) = AppAnalytics.create(AnalyticsEvent.APP_INSTALL_BY_REFERRAL.NAME)
        .addBasicParam()
        .addUserDetails()
        .addParam(AnalyticsEvent.TEST_ID_PARAM.NAME, testId)
        .addParam(AnalyticsEvent.EXPLORE_TYPE.NAME, exploreType)
        .addParam(
            AnalyticsEvent.REFERRAL_CODE.NAME,
            referralCode
        )
        .push()

    private fun logAppLaunchEvent(networkOperatorName: String) =
        AppAnalytics.create(AnalyticsEvent.APP_LAUNCHED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.NETWORK_CARRIER.NAME, networkOperatorName)
            .push(true)


    private fun getNetworkOperatorName() =
        (baseContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?)?.networkOperatorName
            ?: ""


    private fun parseReferralCode(jsonParams: JSONObject) =
        if (jsonParams.has(Defines.Jsonkey.ReferralCode.key)) jsonParams.getString(
            Defines.Jsonkey.ReferralCode.key
        ) else null
}
