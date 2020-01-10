package com.joshtalks.joshskills.ui.inbox

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.location.LocationRequest
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.COURSE_STARTED_FB_EVENT
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.inapp_update.Constants
import com.joshtalks.joshskills.core.inapp_update.InAppUpdateManager
import com.joshtalks.joshskills.core.inapp_update.InAppUpdateStatus
import com.joshtalks.joshskills.core.notification.FCMTokenManager
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.DatabaseUtils
import com.joshtalks.joshskills.repository.local.eventbus.ExploreCourseEventBus
import com.joshtalks.joshskills.repository.local.eventbus.OpenCourseEventBus
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.ProfileResponse
import com.joshtalks.joshskills.repository.server.SearchLocality
import com.joshtalks.joshskills.repository.server.UpdateUserLocality
import com.joshtalks.joshskills.repository.service.SyncChatService
import com.joshtalks.joshskills.ui.chat.ConversationActivity
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.view_holders.EmptyHorizontalView
import com.joshtalks.joshskills.ui.view_holders.FindMoreViewHolder
import com.joshtalks.joshskills.ui.view_holders.InboxViewHolder
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.patloew.rxlocation.RxLocation
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
const val COURSE_EXPLORER_CODE = 2002
const val COURSE_EXPLORER_WITHOUT_CODE = 2003
const val REGISTER_NEW_COURSE_CODE = 2003
const val REQ_CODE_VERSION_UPDATE = 530
const val USER_DETAILS_CODE = 1001


class InboxActivity : CoreJoshActivity(), LifecycleObserver, InAppUpdateManager.InAppUpdateHandler {


    private val viewModel: InboxViewModel by lazy {
        ViewModelProviders.of(this).get(InboxViewModel::class.java)
    }
    private var compositeDisposable = CompositeDisposable()

    private var inAppUpdateManager: InAppUpdateManager? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FCMTokenManager.pushToken()
        AppObjectController.joshApplication.updateDeviceDetail()
        AppObjectController.joshApplication.userActive()
        DatabaseUtils.updateUserMessageSeen()
        setContentView(R.layout.activity_inbox)
        setToolbar()
        lifecycle.addObserver(this)
        AppAnalytics.create(AnalyticsEvent.INBOX_SCREEN.NAME).push()
        addLiveDataObservable()
        checkAppUpdate()
        workInBackground()
        viewModel.getRegisterCourses()
        SyncChatService.syncChatWithServer()
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
        var updateMode = Constants.UpdateMode.FLEXIBLE
        if (AppObjectController.getFirebaseRemoteConfig().getBoolean("update_force")) {
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

    }

    private fun addLiveDataObservable() {
        viewModel.registerCourseNetworkLiveData.observe(this, Observer {
            if (it == null || it.isEmpty()) {
                openCourseExplorer()
            } else {
                buyCourseFBEvent()
                recycler_view_inbox.removeAllViews()
                val total = it.size
                it.forEachWithIndex { i, inbox ->
                    recycler_view_inbox.addView(
                        InboxViewHolder(
                            inbox, total, i
                        )
                    )
                }
                progress_bar.visibility = View.GONE

                addCourseExploreView()
            }
        })

        viewModel.registerCourseMinimalLiveData.observe(this, Observer {
            if (it != null && it.isNotEmpty()) {
                buyCourseFBEvent()
                recycler_view_inbox.removeAllViews()
                val total = it.size
                it.forEachWithIndex { i, inbox ->
                    recycler_view_inbox.addView(
                        InboxViewHolder(
                            inbox, total, i
                        )
                    )
                }
                progress_bar.visibility = View.GONE

                addCourseExploreView()
            }
        })

    }

    private fun addObserver() {
        compositeDisposable.add(
            RxBus2.listen(OpenCourseEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (isUserHavePersonalDetails()) {
                        AppAnalytics.create(AnalyticsEvent.COURSE_SELECTED.NAME)
                            .addParam("course_id", it.inboxEntity.conversation_id).push()
                        ConversationActivity.startConversionActivity(this, it.inboxEntity)

                    } else {
                        startActivity(getPersonalDetailsActivityIntent())
                    }
                }, {
                    it.printStackTrace()
                })
        )
        compositeDisposable.add(RxBus2.listen(ExploreCourseEventBus::class.java).subscribe {
            compositeDisposable.clear()
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


    override fun onBackPressed() {
        AppAnalytics.create(AnalyticsEvent.BACK_PRESSED.NAME)
            .addParam("name", javaClass.simpleName)
            .push()
        super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        Runtime.getRuntime().gc()
        addObserver()

    }

    override fun onPause() {
        super.onPause()
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
        if (AppObjectController.getFirebaseRemoteConfig().getBoolean("course_explore_flag")) {
            recycler_view_inbox.addView(FindMoreViewHolder())
        } else {
            addEmptyView()
        }
    }

    private fun buyCourseFBEvent() {
        CoroutineScope(Dispatchers.Default).launch {
            if (PrefManager.hasKey(COURSE_STARTED_FB_EVENT)) {
                return@launch
            }
            val params = Bundle()
            params.putString("mentor_id", Mentor.getInstance().getId())
            AppObjectController.facebookEventLogger.logEvent(
                AnalyticsEvent.COURSE_STARTED.name,
                params
            )
            PrefManager.put(COURSE_STARTED_FB_EVENT, true)
        }
    }


}
