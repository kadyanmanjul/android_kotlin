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
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.ExploreCourseEventBus
import com.joshtalks.joshskills.repository.local.model.ExploreCardType
import com.joshtalks.joshskills.repository.local.model.NotificationAction
import com.joshtalks.joshskills.repository.server.onboarding.ONBOARD_VERSIONS
import com.joshtalks.joshskills.repository.server.onboarding.VersionResponse
import com.joshtalks.joshskills.ui.assessment.view.Stub
import com.joshtalks.joshskills.ui.inbox.extra.NewUserLayout
import kotlinx.android.synthetic.main.activity_inbox.*
import kotlinx.android.synthetic.main.find_more_layout.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

abstract class InboxBaseActivity : CoreJoshActivity(),
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
        lifecycleScope.launchWhenCreated {
            versionResponse = VersionResponse.getInstance()
        }
    }

    override fun onStart() {
        super.onStart()
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            isSubscriptionStarted = PrefManager.getBoolValue(IS_SUBSCRIPTION_STARTED)
            isSubscriptionEnd = PrefManager.getBoolValue(IS_SUBSCRIPTION_ENDED).not()
        }
    }

    private fun addObserver() {
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            viewModel.overAllWatchTime.collectLatest {
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
        AppAnalytics.create(eventName)
            .addBasicParam()
            .addUserDetails()
            .addParam("version", versionResponse?.version?.name.toString())
            .push()
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
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
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
        if (isGuestUser().not()) {
            RxBus2.publish(ExploreCourseEventBus())
            logEvent(AnalyticsEvent.FIND_MORE_COURSE_CLICKED.NAME)
            return
        }
        versionResponse?.version?.name?.let {

            when (it) {
                ONBOARD_VERSIONS.ONBOARDING_V1, ONBOARD_VERSIONS.ONBOARDING_V7, ONBOARD_VERSIONS.ONBOARDING_V8 -> {
                    RxBus2.publish(ExploreCourseEventBus())
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

    protected fun openOnBoardFlow() {
        versionResponse?.version?.name.let {
            when (it) {
                ONBOARD_VERSIONS.ONBOARDING_V1, ONBOARD_VERSIONS.ONBOARDING_V7, ONBOARD_VERSIONS.ONBOARDING_V8 -> {
                    openCourseExplorer()
                }
                ONBOARD_VERSIONS.ONBOARDING_V2, ONBOARD_VERSIONS.ONBOARDING_V3, ONBOARD_VERSIONS.ONBOARDING_V4, ONBOARD_VERSIONS.ONBOARDING_V5, ONBOARD_VERSIONS.ONBOARDING_V6 -> {
                    openCourseSelectionExplorer()
                }
                else -> {
                    openCourseExplorer()
                }
            }
        }
    }

    protected fun updateSubscriptionTipView(
        exploreType: ExploreCardType,
        showTooltip1: Boolean,
        showTooltip2: Boolean,
        courseSize: Int
    ) {
        if (PrefManager.getBoolValue(IS_SUBSCRIPTION_STARTED).not()) {
            when (exploreType) {
                ExploreCardType.FREETRIAL -> {
                    subscriptionTipContainer.visibility = View.VISIBLE

                    val remainingTrialDays = PrefManager.getIntValue(REMAINING_TRIAL_DAYS)
                    if (remainingTrialDays in 0..7 && showTooltip1) {
                        showToolTipBelowFindMoreCourse(remainingTrialDays)
                    }
                    if (remainingTrialDays in 0..4 && showTooltip2) {
                        showExpiryTimeToolTip()
                    }
                    txtSubscriptionTip.isSelected = true
                    when {
                        remainingTrialDays <= 0 -> {
                            txtSubscriptionTip.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY7)
                            txtSubscriptionTip2.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY7)
                        }

                        remainingTrialDays == 1 -> {
                            txtSubscriptionTip.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY6)
                            txtSubscriptionTip2.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY6)

                        }

                        remainingTrialDays == 2 -> {
                            txtSubscriptionTip.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY5)
                            txtSubscriptionTip2.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY5)

                        }

                        remainingTrialDays == 3 -> {
                            txtSubscriptionTip.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY4)
                            txtSubscriptionTip2.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY4)

                        }

                        remainingTrialDays == 4 -> {
                            txtSubscriptionTip.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY3)
                            txtSubscriptionTip2.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY3)
                        }

                        remainingTrialDays == 5 -> {
                            txtSubscriptionTip.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY2)
                            txtSubscriptionTip2.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY2)
                        }

                        remainingTrialDays == 6 -> {
                            txtSubscriptionTip.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY1)
                            txtSubscriptionTip2.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY1)
                        }

                        remainingTrialDays > 6 -> {
                            txtSubscriptionTip.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY0)
                            txtSubscriptionTip2.text = AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY0)
                        }
                    }
                }

                ExploreCardType.FFCOURSE -> {
                    subscriptionTipContainer.visibility = View.VISIBLE
                    txtSubscriptionTip.text = if (courseSize > 1) {
                        AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.EXPLORE_TYPE_NORMAL_TIP)
                    } else {
                        AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.EXPLORE_TYPE_FFCOURSE_TIP)
                    }
                    txtSubscriptionTip2.text = if (courseSize > 1) {
                        AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.EXPLORE_TYPE_NORMAL_TIP)
                    } else {
                        AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.EXPLORE_TYPE_FFCOURSE_TIP)
                    }
                }

                ExploreCardType.NORMAL -> {
                    subscriptionTipContainer.visibility = View.VISIBLE
                    if (courseSize > 0) {
                        txtSubscriptionTip.text = AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.EXPLORE_TYPE_NORMAL_TIP)
                        txtSubscriptionTip2.text = AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.EXPLORE_TYPE_NORMAL_TIP)
                        subscriptionTipContainer.visibility = View.GONE
                    }
                }

            }
        } else {
            subscriptionTipContainer.visibility = View.GONE
        }

    }

    abstract fun showExpiryTimeToolTip()
    abstract fun showToolTipBelowFindMoreCourse(remainingTrialDays: Int)
    abstract fun openCourseExplorer()
    abstract fun openCourseSelectionExplorer(alreadyHaveCourses: Boolean = false)
}