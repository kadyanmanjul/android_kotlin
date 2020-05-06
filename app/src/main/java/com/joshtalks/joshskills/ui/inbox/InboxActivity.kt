package com.joshtalks.joshskills.ui.inbox

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.drawable.PictureDrawable
import android.net.Uri
import android.os.Build
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
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.inapp_update.Constants
import com.joshtalks.joshskills.core.inapp_update.InAppUpdateManager
import com.joshtalks.joshskills.core.inapp_update.InAppUpdateStatus
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.DatabaseUtils
import com.joshtalks.joshskills.repository.local.eventbus.ExploreCourseEventBus
import com.joshtalks.joshskills.repository.local.eventbus.OpenCourseEventBus
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.ACTION_UPSELLING_POPUP
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.ProfileResponse
import com.joshtalks.joshskills.repository.server.SearchLocality
import com.joshtalks.joshskills.repository.server.UpdateUserLocality
import com.joshtalks.joshskills.repository.service.SyncChatService
import com.joshtalks.joshskills.ui.chat.ConversationActivity
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.payment.COURSE_ID
import com.joshtalks.joshskills.ui.referral.ReferralActivity
import com.joshtalks.joshskills.ui.tooltip.BalloonFactory
import com.joshtalks.joshskills.ui.view_holders.EmptyHorizontalView
import com.joshtalks.joshskills.ui.view_holders.InboxViewHolder
import com.joshtalks.skydoves.balloon.OnBalloonDismissListener
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.patloew.rxlocation.RxLocation
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_inbox.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.collections.forEachWithIndex

const val REGISTER_INFO_CODE = 2001
const val COURSE_EXPLORER_CODE = 2002               //TODO(FixMe) - Use of this RequestCode?
const val COURSE_EXPLORER_WITHOUT_CODE = 2003
const val REGISTER_NEW_COURSE_CODE = 2003           //TODO(FixMe) - Same request code
const val REQ_CODE_VERSION_UPDATE = 530
const val USER_DETAILS_CODE = 1001


class InboxActivity : CoreJoshActivity(), LifecycleObserver, InAppUpdateManager.InAppUpdateHandler {

    private val viewModel: InboxViewModel by lazy {
        ViewModelProvider(this).get(InboxViewModel::class.java)
    }
    private var compositeDisposable = CompositeDisposable()
    private var inAppUpdateManager: InAppUpdateManager? = null
    private lateinit var earnIV: AppCompatImageView
    private lateinit var findMoreLayout: FrameLayout
    private var findMoreVisible = true

    private val offerIn7DaysHint by lazy { BalloonFactory.offerIn7Days(this, this) }
    private val hintFirstTime by lazy {
        BalloonFactory.hintOfferFirstTime(this, this, object :
            OnBalloonDismissListener {
            override fun onBalloonDismiss() {
                attachOfferHintView()
            }
        })
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)
        DatabaseUtils.updateUserMessageSeen()
        setContentView(R.layout.activity_inbox)
        setToolbar()
        lifecycle.addObserver(this)
        AppAnalytics.create(AnalyticsEvent.INBOX_SCREEN.NAME).push()
        addLiveDataObservable()
        checkAppUpdate()
        workInBackground()
        SyncChatService.syncChatWithServer()
        handelIntentAction()
        addObserver()
    }

    private fun workInBackground() {
        CoroutineScope(Dispatchers.Default).launch {
            AppObjectController.clearDownloadMangerCallback()
            AppAnalytics.updateUser()
            processIntent(intent)
            delay(1000)
            locationFetch()
        }
        WorkMangerAdmin.installReferrerWorker()
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
            Dexter.withActivity(this)
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

    private fun setToolbar() {
        val titleView = findViewById<AppCompatTextView>(R.id.text_message_title)
        titleView.text = getString(R.string.inbox_header)
        earnIV = findViewById(R.id.iv_earn)
        earnIV.setOnClickListener {
            WorkMangerAdmin.referralEventTracker(REFERRAL_EVENT.CLICK_ON_REFERRAL)
            AppAnalytics.create(AnalyticsEvent.REFERRAL_SELECTED.NAME).push()
            ReferralActivity.startReferralActivity(
                this@InboxActivity,
                InboxActivity::class.java.name
            )
        }
        visibleShareEarn()
        findMoreLayout = findViewById(R.id.parent_layout)
        findViewById<View>(R.id.find_more).setOnClickListener {
            RxBus2.publish(ExploreCourseEventBus())
        }
    }

    private fun addLiveDataObservable() {
        viewModel.registerCourseNetworkLiveData.observe(this, Observer {
            if (it == null || it.isEmpty()) {
                openCourseExplorer()
            } else {
                addCourseInRecyclerView(it)
            }
        })
        viewModel.registerCourseMinimalLiveData.observe(this, Observer {
            addCourseInRecyclerView(it)
        })
    }

    private fun addCourseInRecyclerView(items: List<InboxEntity>?) {
        if (items.isNullOrEmpty()) {
            return
        }
        recycler_view_inbox.removeAllViews()
        val total = items.size
        items.forEachWithIndex { i, inbox ->
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
                    AppAnalytics.create(AnalyticsEvent.COURSE_SELECTED.NAME)
                        .addParam("course_id", it.inboxEntity.conversation_id).push()
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
    }

    private fun openCourseExplorer() {
        val registerCourses: MutableSet<InboxEntity> = mutableSetOf()
        viewModel.registerCourseMinimalLiveData.value?.let {
            registerCourses.addAll(it)
        }
        viewModel.registerCourseNetworkLiveData.value?.let {
            registerCourses.addAll(it)
        }
        WorkMangerAdmin.fineMoreEventWorker()
        CourseExploreActivity.startCourseExploreActivity(
            this,
            COURSE_EXPLORER_CODE,
            registerCourses
        )
    }


    private fun addEmptyView() {
        for (i in 1..8) {
            recycler_view_inbox.addView(EmptyHorizontalView())
        }
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
            } else if (resultCode == Activity.RESULT_CANCELED) {
                if (viewModel.registerCourseNetworkLiveData.value.isNullOrEmpty()) {
                    if ((viewModel.registerCourseMinimalLiveData.value.isNullOrEmpty())) {
                        this@InboxActivity.finish()
                    }
                }
            }
        } else if (requestCode == USER_DETAILS_CODE) {
            if (resultCode == Activity.RESULT_CANCELED) {
                finish()
            }
        }


    }

    @SuppressLint("MissingPermission")
    private fun getLocationAndUpload() {
        val rxLocation = RxLocation(application)
        val locationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(10000)
        compositeDisposable.add(
            rxLocation.location().updates(locationRequest)
                .subscribeOn(Schedulers.computation())
                .subscribe({ location ->
                    if (Mentor.getInstance().getLocality() == null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val request = UpdateUserLocality()
                                request.locality =
                                    SearchLocality(location.latitude, location.longitude)

                                val response: ProfileResponse =
                                    AppObjectController.signUpNetworkService.updateUserAddressAsync(
                                        Mentor.getInstance().getId(),
                                        request
                                    ).await()
                                Mentor.getInstance().setLocality(response.locality).update()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                // onFailedToFetchLocation()
                            }
                            compositeDisposable.clear()
                        }
                    }
                }, { ex ->
                    ex.printStackTrace()

                })
        )

    }


    override fun onResume() {
        super.onResume()
        Runtime.getRuntime().gc()
        viewModel.getRegisterCourses()
        addObserver()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    override fun onInAppUpdateError(code: Int, error: Throwable?) {
        error?.printStackTrace()

    }

    override fun onInAppUpdateStatus(status: InAppUpdateStatus?) {
        if (status != null) {
            if (status.isDownloaded) {
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
    }

    private fun addCourseExploreView() {
        hintFirstTime.dismiss()
        //  offerIn7DaysHint.dismiss()
        findMoreLayout.visibility = View.VISIBLE
        if (PrefManager.getBoolValue(FIRST_TIME_OFFER_SHOW).not()) {
            PrefManager.put(FIRST_TIME_OFFER_SHOW, true)
            compositeDisposable.add(AppObjectController.appDatabase.courseDao()
                .isUserOldThen7Days()
                .concatMap {
                    val (flag, _) = Utils.isUser7DaysOld(it.courseCreatedDate)
                    return@concatMap Maybe.just(flag)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { value ->
                        if (value) {
                            if (offerIn7DaysHint.isShowing.not() && isFinishing.not()) {
                                val root = findViewById<View>(R.id.find_more)
                                hintFirstTime.showAlignBottom(root)
                            }
                        }
                    },
                    { error ->
                        error.printStackTrace()
                    }
                ))
        } else {
            attachOfferHintView()
        }
        userProfileActivityNotExist()
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


    private fun userProfileActivityNotExist() {
        try {
            val entity = viewModel.registerCourseMinimalLiveData.value?.get(0)
            val entity2 = viewModel.registerCourseNetworkLiveData.value?.get(0)
            if (entity == null && entity2 == null) {
                return
            }
            if (Mentor.getInstance().hasId() && User.getInstance().dateOfBirth.isNullOrEmpty()) {
                startActivity(getPersonalDetailsActivityIntent())
            }

        } catch (ex: Exception) {
        }

    }

    /* private fun showAppUseWhenComeFirstTime() {
         try {
             val entity = viewModel.registerCourseMinimalLiveData.value?.get(0)
             val entity2 = viewModel.registerCourseNetworkLiveData.value?.get(0)
             if (entity == null && entity2 == null) {
                 return
             }

             if (PrefManager.getBoolValue(FIRST_COURSE_BUY).not()) {
                 isUserFirstTime = false
                 PrefManager.put(FIRST_COURSE_BUY, true)
                 TooltipUtility.showFirstTimeUserTooltip(entity ?: entity2, this, this) {
                 }
             }
         } catch (ex: Exception) {
         }
     }*/

    fun attachOfferHintView() {
        compositeDisposable.add(AppObjectController.appDatabase
            .courseDao()
            .isUserOldThen7Days()
            .concatMap {
                val (flag, _) = Utils.isUser7DaysOld(it.courseCreatedDate)
                return@concatMap Maybe.just(flag)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { value ->
                    if (value) {
                        val root = findViewById<View>(R.id.find_more)
                        if (offerIn7DaysHint.isShowing.not() && isFinishing.not()) {
                            offerIn7DaysHint.showAlignBottom(root)
                            findViewById<View>(R.id.bottom_line).visibility = View.GONE

                        }
                    }
                },
                { error ->
                    error.printStackTrace()
                }
            ))
    }
}
