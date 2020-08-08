package com.joshtalks.joshskills.ui.inbox

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.drawable.PictureDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.facebook.share.internal.ShareConstants.ACTION_TYPE
import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYou
import com.google.android.gms.location.LocationRequest
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ARG_PLACEHOLDER_URL
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.COURSE_ID
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.EXPLORE_TYPE
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.core.IS_TRIAL_ENDED
import com.joshtalks.joshskills.core.IS_TRIAL_STARTED
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.REMAINING_TRIAL_DAYS
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.inapp_update.Constants
import com.joshtalks.joshskills.core.inapp_update.InAppUpdateManager
import com.joshtalks.joshskills.core.inapp_update.InAppUpdateStatus
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.NPSEventModel
import com.joshtalks.joshskills.repository.local.eventbus.ExploreCourseEventBus
import com.joshtalks.joshskills.repository.local.eventbus.NPSEventGenerateEventBus
import com.joshtalks.joshskills.repository.local.eventbus.OpenCourseEventBus
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.ACTION_UPSELLING_POPUP
import com.joshtalks.joshskills.repository.local.model.ExploreCardType
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.ProfileResponse
import com.joshtalks.joshskills.repository.server.SearchLocality
import com.joshtalks.joshskills.repository.server.UpdateUserLocality
import com.joshtalks.joshskills.ui.chat.ConversationActivity
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.payment.order_summary.PaymentSummaryActivity
import com.joshtalks.joshskills.ui.referral.ReferralActivity
import com.joshtalks.joshskills.ui.tooltip.BalloonFactory
import com.joshtalks.joshskills.ui.view_holders.InboxViewHolder
import com.joshtalks.skydoves.balloon.Balloon
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.patloew.rxlocation.RxLocation
import io.reactivex.CompletableObserver
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_inbox.progress_bar
import kotlinx.android.synthetic.main.activity_inbox.recycler_view_inbox
import kotlinx.android.synthetic.main.activity_inbox.subscriptionTipContainer
import kotlinx.android.synthetic.main.activity_inbox.txtConvert
import kotlinx.android.synthetic.main.activity_inbox.txtSubscriptionTip
import kotlinx.android.synthetic.main.find_more_layout.find_more
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.anko.collections.forEachWithIndex
import java.util.*

const val REGISTER_INFO_CODE = 2001
const val COURSE_EXPLORER_CODE = 2002
const val COURSE_EXPLORER_WITHOUT_CODE = 2003
const val PAYMENT_FOR_COURSE_CODE = 2004
const val REQ_CODE_VERSION_UPDATE = 530
const val USER_DETAILS_CODE = 1001
const val TRIAL_COURSE_ID = "76"
const val SUBSCRIPTION_COURSE_ID = "60"


class InboxActivity : CoreJoshActivity(), LifecycleObserver, InAppUpdateManager.InAppUpdateHandler {

    private val viewModel: InboxViewModel by lazy {
        ViewModelProvider(this).get(InboxViewModel::class.java)
    }
    private var compositeDisposable = CompositeDisposable()
    private var inAppUpdateManager: InAppUpdateManager? = null
    private lateinit var earnIV: AppCompatImageView
    private lateinit var findMoreLayout: FrameLayout
    private var offerInHint: Balloon? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        WorkMangerAdmin.requiredTaskInLandingPage()
        AppAnalytics.create(AnalyticsEvent.INBOX_SCREEN.NAME).push()
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(this)
        setContentView(R.layout.activity_inbox)
        setToolbar()
        addLiveDataObservable()
        checkAppUpdate()
        workInBackground()
        handelIntentAction()
    }

    private fun updateSubscriptionTipView() {
        val exploreType = PrefManager.getStringValue(EXPLORE_TYPE, true)
        if (exploreType.isNotBlank() && ExploreCardType.valueOf(exploreType) == ExploreCardType.FREETRIAL) {
            subscriptionTipContainer.visibility = View.VISIBLE

            val remainingTrialDays = PrefManager.getIntValue(REMAINING_TRIAL_DAYS, true)
            txtSubscriptionTip.text = when {

                remainingTrialDays < 0 -> AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY7)

                remainingTrialDays == 1 -> AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY6)

                remainingTrialDays == 2 -> AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY5)

                remainingTrialDays == 3 -> AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY4)

                remainingTrialDays == 4 -> AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY3)

                remainingTrialDays == 5 -> AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY2)

                remainingTrialDays == 6 -> AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY1)

                remainingTrialDays > 6 -> AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_TRIAL_TIP_DAY0)
                else -> EMPTY
            }
        } else if (exploreType.isNotBlank() && ExploreCardType.valueOf(exploreType) == ExploreCardType.FFCOURSE) {
            subscriptionTipContainer.visibility = View.VISIBLE
            viewModel.registerCourseNetworkLiveData.value?.let {
                txtSubscriptionTip.text = if (it.size > 1) {
                    AppObjectController.getFirebaseRemoteConfig()
                        .getString(FirebaseRemoteConfigKey.EXPLORE_TYPE_NORMAL_TIP)
                } else {
                    AppObjectController.getFirebaseRemoteConfig()
                        .getString(FirebaseRemoteConfigKey.EXPLORE_TYPE_FFCOURSE_TIP)
                }
            }
        } else if (exploreType.isNotBlank() && ExploreCardType.valueOf(exploreType) == ExploreCardType.NORMAL) {
            subscriptionTipContainer.visibility = View.VISIBLE
            viewModel.registerCourseNetworkLiveData.value?.let {
                txtSubscriptionTip.text = AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.EXPLORE_TYPE_NORMAL_TIP)
            }
        }

    }

    private fun setToolbar() {
        val titleView = findViewById<AppCompatTextView>(R.id.text_message_title)
        titleView.text = getString(R.string.inbox_header)
        earnIV = findViewById(R.id.iv_earn)
        earnIV.setOnClickListener {
            AppAnalytics
                .create(AnalyticsEvent.REFER_BUTTON_CLICKED.NAME)
                .addBasicParam()
                .addUserDetails()
                .addParam(AnalyticsEvent.REFERRAL_CODE.NAME, Mentor.getInstance().referralCode)
                .push()
            ReferralActivity.startReferralActivity(
                this@InboxActivity,
                InboxActivity::class.java.name
            )
        }
        findMoreLayout = findViewById(R.id.parent_layout)
        find_more.setOnClickListener {
            AppAnalytics.create(AnalyticsEvent.FIND_MORE_COURSE_CLICKED.NAME)
                .addBasicParam()
                .addUserDetails()
                .push()
            RxBus2.publish(ExploreCourseEventBus())
        }
        visibleShareEarn()
    }

    private fun workInBackground() {
        CoroutineScope(Dispatchers.Default).launch {
            processIntent(intent)
            WorkMangerAdmin.determineNPAEvent()
        }
        when {
            shouldRequireCustomPermission() -> {
                checkForOemNotifications()
            }
            NPSEventModel.getCurrentNPA() != null -> {
                showNetPromoterScoreDialog()
            }

            else -> {
                viewModel.registerCourseMinimalLiveData.value?.run {
                    if (this.isNotEmpty()) {
                        locationFetch()
                    }
                }
            }
        }
    }


    private fun checkAppUpdate() {
        val forceUpdateMinVersion =
            AppObjectController.getFirebaseRemoteConfig().getLong("force_upgrade_after_version")
        val forceUpdateFlag =
            AppObjectController.getFirebaseRemoteConfig().getBoolean("update_force")
        val currentAppVersion = BuildConfig.VERSION_CODE
        var updateMode = Constants.UpdateMode.FLEXIBLE

        if (currentAppVersion <= forceUpdateMinVersion && forceUpdateFlag) {
            updateMode = Constants.UpdateMode.IMMEDIATE
        }
        inAppUpdateManager = InAppUpdateManager.Builder(this, REQ_CODE_VERSION_UPDATE)
            .resumeUpdates(true)
            .mode(updateMode)
            .useCustomNotification(false)
            .snackBarMessage(getString(R.string.update_message))
            .snackBarAction(getString(R.string.restart))
            .handler(this)

        inAppUpdateManager?.checkForAppUpdate()
    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        processIntent(intent)
        this.intent = intent
        handelIntentAction()
    }

    private fun handelIntentAction() {
        if (intent != null && intent.hasExtra(ACTION_TYPE)) {
            intent.getStringExtra(ACTION_TYPE)?.let {
                if (ACTION_UPSELLING_POPUP.equals(it, ignoreCase = true)) {
                    showPromotionScreen(
                        intent.getStringExtra(COURSE_ID)!!,
                        intent.getStringExtra(ARG_PLACEHOLDER_URL)!!
                    )
                }
            }
        }
    }


    private fun locationFetch() {
        if (Mentor.getInstance().getLocality() == null) {
            Dexter.withContext(this)
                .withPermissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION

                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        token?.continuePermissionRequest()
                    }

                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.areAllPermissionsGranted()?.let { flag ->
                            if (flag) {
                                getLocationAndUpload()
                            }
                        }
                    }

                }).check()
        }
    }


    private fun addLiveDataObservable() {
        viewModel.registerCourseNetworkLiveData.observe(this, Observer {
            if (it == null || it.isEmpty()) {
                openCourseExplorer()
            } else {
                addCourseInRecyclerView(it)
                setTrialEndParam(it)
                updateExploreTypeParam(it)
                updateSubscriptionTipView()
            }
        })
        viewModel.registerCourseMinimalLiveData.observe(this, Observer {
            addCourseInRecyclerView(it)
        })

        txtConvert.setOnClickListener {
            logEvent(AnalyticsEvent.CONVERT_CLICKED.name)
            PaymentSummaryActivity.startPaymentSummaryActivity(this, "122") // todo remove hardcode
        }
    }

    private fun logEvent(eventName: String) {
        AppAnalytics.create(eventName)
            .addBasicParam()
            .addUserDetails()
            .push()
    }

    private fun addCourseInRecyclerView(items: List<InboxEntity>?) {
        if (items.isNullOrEmpty()) {
            return
        }
        recycler_view_inbox.removeAllViews()
        val total = items.size
        items.forEachWithIndex { i, inbox ->
            if (inbox.courseId != SUBSCRIPTION_COURSE_ID && inbox.courseId != TRIAL_COURSE_ID)
                recycler_view_inbox.addView(
                    InboxViewHolder(
                        inbox, total, i
                    )
                )
        }
        progress_bar.visibility = View.GONE
        addCourseExploreView()
    }


    private fun addObserver() {
        compositeDisposable.add(
            RxBus2.listen(OpenCourseEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    ConversationActivity.startConversionActivity(this, it.inboxEntity)
                }, {
                    it.printStackTrace()
                })
        )
        compositeDisposable.add(RxBus2.listen(ExploreCourseEventBus::class.java)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                openCourseExplorer()
            })
        compositeDisposable.add(RxBus2.listen(NPSEventGenerateEventBus::class.java)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                showNetPromoterScoreDialog()
            })

    }

    private fun openCourseExplorer() {
        val registerCourses: MutableSet<InboxEntity> = mutableSetOf()
        viewModel.registerCourseMinimalLiveData.value?.let {
            registerCourses.addAll(it)
        }
        viewModel.registerCourseNetworkLiveData.value?.let {
            registerCourses.addAll(it)
        }
        CourseExploreActivity.startCourseExploreActivity(
            this,
            COURSE_EXPLORER_CODE,
            registerCourses, state = ActivityEnum.Inbox
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REGISTER_INFO_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                overridePendingTransition(0, 0)
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                finish()
                overridePendingTransition(0, 0)
                startActivity(intent)
            } else {
                finish()
            }
        } else if (requestCode == REQ_CODE_VERSION_UPDATE) {
            if (resultCode == Activity.RESULT_CANCELED) {
                inAppUpdateManager?.checkForAppUpdate()
            }
        } else if (requestCode == COURSE_EXPLORER_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                overridePendingTransition(0, 0)
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                finish()
                overridePendingTransition(0, 0)
                startActivity(intent)
            } else if (resultCode == Activity.RESULT_CANCELED && viewModel.registerCourseNetworkLiveData.value.isNullOrEmpty()) {
                if ((viewModel.registerCourseMinimalLiveData.value.isNullOrEmpty())) {
                    this@InboxActivity.finish()
                }
            }
        } else if (requestCode == USER_DETAILS_CODE && resultCode == Activity.RESULT_CANCELED) {
            finish()
        }


    }

    @SuppressLint("MissingPermission")
    private fun getLocationAndUpload() {
        val rxLocation = RxLocation(application)
        val locationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(10000)

        rxLocation.settings().checkAndHandleResolutionCompletable(locationRequest)
            .subscribeOn(Schedulers.computation())
            .subscribe(object : CompletableObserver {
                override fun onSubscribe(d: Disposable) {
                }

                override fun onComplete() {
                    compositeDisposable.add(
                        rxLocation.location().updates(locationRequest)
                            .subscribeOn(Schedulers.computation())
                            .subscribe({ location ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val request = UpdateUserLocality()
                                        request.locality =
                                            SearchLocality(location.latitude, location.longitude)
                                        AppAnalytics.setLocation(
                                            location.latitude,
                                            location.longitude
                                        )
                                        val response: ProfileResponse =
                                            AppObjectController.signUpNetworkService.updateUserAddressAsync(
                                                Mentor.getInstance().getId(),
                                                request
                                            ).await()
                                        Mentor.getInstance().setLocality(response.locality).update()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    compositeDisposable.clear()
                                }
                            }, { ex ->
                                // ex.printStackTrace()
                            })
                    )
                }

                override fun onError(e: Throwable) {
                    //  e.printStackTrace()
                }
            })
    }


    override fun onResume() {
        super.onResume()
        Runtime.getRuntime().gc()
        addObserver()
        viewModel.getRegisterCourses()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
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

    private fun addCourseExploreView() {
        findMoreLayout.visibility = View.VISIBLE
        attachOfferHintView()
    }

    private fun visibleShareEarn() {
        val url = AppObjectController.getFirebaseRemoteConfig().getString("EARN_SHARE_IMAGE_URL")
        var iconUri = Uri.parse(url)
        if (url.isEmpty()) {
            iconUri = Uri.parse("file:///android_asset/ic_rupee.svg")
        }
        earnIV.visibility = View.VISIBLE
        val requestBuilder = GlideToVectorYou
            .init()
            .with(this)
            .requestBuilder
        requestBuilder.load(iconUri)
            .transition(DrawableTransitionOptions.withCrossFade())
            .apply(RequestOptions().centerCrop())
            .listener(object : RequestListener<PictureDrawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<PictureDrawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: PictureDrawable?,
                    model: Any?,
                    target: Target<PictureDrawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    resource?.setTint(ContextCompat.getColor(applicationContext, R.color.white))
                    earnIV.setImageDrawable(resource)
                    earnIV.visibility = View.VISIBLE
                    return false
                }

            })
            .into(earnIV)
    }

    private fun attachOfferHintView() {
        compositeDisposable.add(
            AppObjectController.appDatabase
                .courseDao()
                .isUserInOfferDays()
                .concatMap {
                    val (flag, remainDay) = Utils.isUserInDaysOld(it.courseCreatedDate)
                    if (offerInHint == null) {
                        offerInHint =
                            BalloonFactory.offerIn7Days(this, this, remainDay.toString())
                    }
                    return@concatMap Maybe.just(flag)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { value ->
                        val exploreType = PrefManager.getStringValue(EXPLORE_TYPE, true)
                        if (exploreType.isBlank() || exploreType.contentEquals(ExploreCardType.NORMAL.name)) {
                            val root = findViewById<View>(R.id.find_more)
                            offerInHint?.run {
                                if (this.isShowing.not() && isFinishing.not() && value) {
                                    this.showAlignBottom(root)
                                    findViewById<View>(R.id.bottom_line).visibility = View.GONE
                                }
                            }
                        }
                    },
                    { error ->
                        error.printStackTrace()
                    }
                ))
    }

    private fun setTrialEndParam(coursesList: List<InboxEntity>) {
        val trialCourse =
            coursesList.filter { it.courseId == TRIAL_COURSE_ID }.getOrNull(0)
        if (trialCourse != null) {
            PrefManager.put(IS_TRIAL_STARTED, true, true)
        }
        val expiryTimeInMs =
            trialCourse?.courseCreatedDate?.time?.plus(
                (trialCourse.duration ?: 7)
                    .times(24L)
                    .times(60L)
                    .times(60L)
                    .times(1000L)
            )
        val currentTimeInMs = Calendar.getInstance().timeInMillis

        expiryTimeInMs?.let {
            if (it <= currentTimeInMs) {
                logTrailEventExpired()
                PrefManager.put(IS_TRIAL_ENDED, true, true)
            }
            val remainingTrialDays =
                (it.minus(currentTimeInMs))
                    .div(1000L)
                    .div(60L)
                    .div(60L)
                    .div(24L)

            PrefManager.put(REMAINING_TRIAL_DAYS, remainingTrialDays.toInt(), true)
        }

    }

    private fun logTrailEventExpired() {
        if (PrefManager.getBoolValue(IS_TRIAL_ENDED, true).not()) {
            AppAnalytics.create(AnalyticsEvent.SEVEN_DAY_TRIAL_OVER.NAME)
                .addBasicParam()
                .addUserDetails()
                .push()
        }
    }

    private fun updateExploreTypeParam(coursesList: List<InboxEntity>) {
        val trialCourse =
            coursesList.filter { it.courseId == TRIAL_COURSE_ID }.getOrNull(0)
        val subscriptionCourse =
            coursesList.filter { it.courseId == SUBSCRIPTION_COURSE_ID }.getOrNull(0)
        if (subscriptionCourse != null) {
            PrefManager.put(EXPLORE_TYPE, ExploreCardType.SUBSCRIPTION.name, true)
        } else if (trialCourse != null) {
            PrefManager.put(EXPLORE_TYPE, ExploreCardType.FREETRIAL.name, true)
        }
    }

}
