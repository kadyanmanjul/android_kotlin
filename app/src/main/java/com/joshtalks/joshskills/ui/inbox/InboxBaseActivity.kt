package com.joshtalks.joshskills.ui.inbox

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.facebook.share.internal.ShareConstants
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.inapp_update.Constants
import com.joshtalks.joshskills.core.inapp_update.InAppUpdateManager
import com.joshtalks.joshskills.core.inapp_update.InAppUpdateStatus
import com.joshtalks.joshskills.repository.local.model.NotificationAction
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

abstract class InboxBaseActivity : CoreJoshActivity(), InAppUpdateManager.InAppUpdateHandler {
    private var isFromOnBoarding: Boolean = true
    protected var inAppUpdateManager: InAppUpdateManager? = null
    private lateinit var activityRef: WeakReference<AppCompatActivity>
    protected val viewModel: InboxViewModel by lazy {
        ViewModelProvider(this).get(InboxViewModel::class.java)
    }
    private var isSubscriptionStarted: Boolean = false
    private var isSubscriptionEnd: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityRef = WeakReference(this)
       // addObserver()
        AppObjectController.isSettingUpdate = false
    }

    override fun onStart() {
        super.onStart()
        CoroutineScope(Dispatchers.IO).launch {
            isSubscriptionStarted = PrefManager.getBoolValue(IS_SUBSCRIPTION_STARTED)
            isSubscriptionEnd = PrefManager.getBoolValue(IS_SUBSCRIPTION_ENDED).not()
        }
    }

    protected suspend fun checkInAppUpdate() {
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
                .handler(this@InboxBaseActivity)
                inAppUpdateManager?.checkForAppUpdate()
    }

    protected fun logEvent(eventName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            AppAnalytics.create(eventName)
                .addBasicParam()
                .addUserDetails()
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

    protected fun handleIntentAction() {
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
                MixPanelTracker.publishEvent(MixPanelEvent.FIND_MORE_COURSES).push()
                logEvent(AnalyticsEvent.FIND_MORE_COURSE_CLICKED.NAME)
                return@launch
            }
            openCourseExplorer()
        }
    }
    abstract fun openCourseExplorer()
}
