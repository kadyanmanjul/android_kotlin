package com.joshtalks.joshskills.ui.inbox

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.facebook.share.internal.ShareConstants
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.review.ReviewManagerFactory
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.inapp_update.Constants
import com.joshtalks.joshskills.core.inapp_update.InAppUpdateManager
import com.joshtalks.joshskills.core.inapp_update.InAppUpdateStatus
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.NotificationAction
import com.joshtalks.joshskills.repository.server.onboarding.ONBOARD_VERSIONS
import com.joshtalks.joshskills.repository.server.onboarding.VersionResponse
import com.joshtalks.joshskills.ui.assessment.view.Stub
import com.joshtalks.joshskills.ui.inbox.extra.NewUserLayout
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.lang.ref.WeakReference
import kotlinx.android.synthetic.main.activity_inbox.*
import kotlinx.android.synthetic.main.find_more_layout.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class InboxBaseActivity :
    WebRtcMiddlewareActivity(),
    InAppUpdateManager.InAppUpdateHandler {
    private var isFromOnBoarding: Boolean = true
    private var newUserLayoutStub: Stub<NewUserLayout>? = null
    protected var inAppUpdateManager: InAppUpdateManager? = null
    private var versionResponse: VersionResponse? = null
    private lateinit var activityRef: WeakReference<AppCompatActivity>
    protected val viewModel: InboxViewModel by lazy {
        ViewModelProvider(this).get(InboxViewModel::class.java)
    }
    private var isSubscriptionStarted: Boolean = false
    private var isSubscriptionEnd: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityRef = WeakReference(this)
        addObserver()
        lifecycleScope.launch(Dispatchers.IO) {
            versionResponse = VersionResponse.getInstance()
        }
        AppObjectController.isSettingUpdate = false
    }

    override fun onStart() {
        super.onStart()
        CoroutineScope(Dispatchers.IO).launch {
            isSubscriptionStarted = PrefManager.getBoolValue(IS_SUBSCRIPTION_STARTED)
            isSubscriptionEnd = PrefManager.getBoolValue(IS_SUBSCRIPTION_ENDED).not()
        }
    }

    private fun addObserver() {
        lifecycleScope.launchWhenResumed {
            viewModel.overAllWatchTime.collectLatest {
                lifecycleScope.launch(Dispatchers.IO) {
                    var reviewCount = PrefManager.getIntValue(IN_APP_REVIEW_COUNT)
                    val reviewFrequency =
                        AppObjectController.getFirebaseRemoteConfig()
                            .getLong(FirebaseRemoteConfigKey.MINIMUM_TIME_TO_SHOW_REVIEW)
                    when (reviewCount) {
                        0 -> if (it > reviewFrequency) {
                            showInAppReview()
                            PrefManager.put(IN_APP_REVIEW_COUNT, ++reviewCount)
                        }
                        1 -> if (it > reviewFrequency * 2) {
                            PrefManager.put(IN_APP_REVIEW_COUNT, ++reviewCount)
                            showInAppReview()
                        }
                        2 -> if (it > reviewFrequency * 3) {
                            PrefManager.put(IN_APP_REVIEW_COUNT, ++reviewCount)
                            showInAppReview()
                        }
                    }
                }
            }
        }
    }

    private fun showInAppReview() {
        val manager = ReviewManagerFactory.create(applicationContext)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (request.isSuccessful) {
                val reviewInfo = request.result
                manager.launchReviewFlow(this, reviewInfo).addOnCompleteListener { result ->
                    println("result = [${result.isSuccessful}]")
                    result.exception?.printStackTrace()
                }
            }
        }
    }

    protected fun initNewUserTip() {
        lifecycleScope.launchWhenStarted {
            delay(250)
            if (isFromOnBoarding) {
                newUserLayoutStub = Stub(findViewById(R.id.new_user_layout_stub))
                newUserLayoutStub?.let { view ->
                    view.resolved()
                    if (view.resolved().not()) {
                        view.get().addCallback(object : NewUserLayout.Callback {
                            override fun callback(name: String) {
                                this@InboxBaseActivity.logEvent(AnalyticsEvent.OK_GOT_IT_CLICKED.NAME)
                                viewModel.logInboxEngageEvent()
                                view.get().removeAllViews()
                            }
                        })
                    }
                }
            }
        }
    }

    protected fun checkInAppUpdate() {
        val forceUpdateMinVersion =
            AppObjectController.getFirebaseRemoteConfig().getLong("force_upgrade_after_version")
        val forceUpdateFlag =
            AppObjectController.getFirebaseRemoteConfig().getBoolean("update_force")
        val currentAppVersion = BuildConfig.VERSION_CODE
        var updateMode = Constants.UpdateMode.FLEXIBLE

        if (currentAppVersion <= forceUpdateMinVersion && forceUpdateFlag) {
            updateMode = Constants.UpdateMode.IMMEDIATE
        }
        inAppUpdateManager = InAppUpdateManager.Builder(activityRef.get(), REQ_CODE_VERSION_UPDATE)
            .resumeUpdates(true)
            .mode(updateMode)
            .useCustomNotification(false)
            .snackBarMessage(getString(R.string.update_message))
            .snackBarAction(getString(R.string.restart))
            .handler(this)

        inAppUpdateManager?.checkForAppUpdate()
    }

    protected fun logEvent(eventName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            AppAnalytics.create(eventName)
                .addBasicParam()
                .addUserDetails()
                .addParam("version", versionResponse?.version?.name.toString())
                .push()
        }
    }

    override fun onInAppUpdateError(code: Int, error: Throwable?) {
        error?.printStackTrace()
    }

    override fun onInAppUpdateStatus(status: InAppUpdateStatus?) {
        if (status != null && status.isDownloaded) {
            val rootView = window.decorView.findViewById<View>(android.R.id.content)
            val snackBar = Snackbar.make(
                rootView,
                getString(R.string.update_download_success_message),
                Snackbar.LENGTH_INDEFINITE
            )
            snackBar.setAction(getString(R.string.restart)) {
                inAppUpdateManager?.completeUpdate()
            }
            snackBar.show()
        }
    }

    protected fun handelIntentAction() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (intent != null && intent.hasExtra(ShareConstants.ACTION_TYPE)) {
                val obj =
                    intent.getSerializableExtra(ShareConstants.ACTION_TYPE) as NotificationAction?
                obj?.let {
                    if (NotificationAction.ACTION_UP_SELLING_POPUP == it) {
                        showPromotionScreen(
                            intent.getStringExtra(COURSE_ID)!!,
                            intent.getStringExtra(ARG_PLACEHOLDER_URL)!!
                        )
                    }
                }
            }
            if (intent != null && intent.hasExtra(IS_FROM_NEW_ONBOARDING)) {
                isFromOnBoarding = intent.getBooleanExtra(IS_FROM_NEW_ONBOARDING, false)
            }
        }
    }

    protected fun courseExploreClick() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (isGuestUser().not()) {
                openCourseExplorer()
                logEvent(AnalyticsEvent.FIND_MORE_COURSE_CLICKED.NAME)
                return@launch
            }
            versionResponse?.version?.name?.let {

                when (it) {
                    ONBOARD_VERSIONS.ONBOARDING_V1, ONBOARD_VERSIONS.ONBOARDING_V7, ONBOARD_VERSIONS.ONBOARDING_V8 -> {
                        openCourseExplorer()
                        logEvent(AnalyticsEvent.FIND_MORE_COURSE_CLICKED.NAME)
                    }
                    ONBOARD_VERSIONS.ONBOARDING_V2, ONBOARD_VERSIONS.ONBOARDING_V4, ONBOARD_VERSIONS.ONBOARDING_V3, ONBOARD_VERSIONS.ONBOARDING_V5, ONBOARD_VERSIONS.ONBOARDING_V6 -> {
                        if (isSubscriptionStarted && isSubscriptionEnd.not()) {
                            openCourseExplorer()
                        } else {
                            openCourseSelectionExplorer(true)
                        }
                        logEvent(AnalyticsEvent.ADD_MORE_COURSE_CLICKED.NAME)
                    }
                }
            }
        }
    }

    protected fun locationFetch() {
        if (Mentor.getInstance().getLocality() == null) {
            AppObjectController.uiHandler.post {
                PermissionUtils.locationPermission(
                    this,
                    object : MultiplePermissionsListener {
                        override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                            report?.areAllPermissionsGranted()?.let { flag ->
                                if (flag) {
                                    fetchUserLocation()
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
                    }
                )
            }
        }
    }

    abstract fun openCourseExplorer()
    abstract fun openCourseSelectionExplorer(alreadyHaveCourses: Boolean = false)
}
