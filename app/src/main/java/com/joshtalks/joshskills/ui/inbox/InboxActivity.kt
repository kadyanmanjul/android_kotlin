package com.joshtalks.joshskills.ui.inbox

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.*
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.service.FCMTokenManager
import com.joshtalks.joshskills.messaging.RxBus
import com.joshtalks.joshskills.repository.local.DatabaseUtils
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.ProfileResponse
import com.joshtalks.joshskills.repository.server.SearchLocality
import com.joshtalks.joshskills.repository.server.UpdateUserLocality
import com.joshtalks.joshskills.repository.service.SyncChatService
import com.joshtalks.joshskills.ui.chat.ConversationActivity
import com.joshtalks.joshskills.ui.sign_up_old.RegisterInfoActivity
import com.joshtalks.joshskills.ui.view_holders.EmptyHorizontalView
import com.joshtalks.joshskills.ui.view_holders.InboxViewHolder
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_inbox.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.android.gms.location.LocationRequest
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.patloew.rxlocation.RxLocation


const val REGISTER_INFO_CODE = 2001

class InboxActivity : CoreJoshActivity(), LifecycleObserver {

    private val viewModel: InboxViewModel by lazy {
        ViewModelProviders.of(this).get(InboxViewModel::class.java)
    }
    private var compositeDisposable = CompositeDisposable()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FCMTokenManager.pushToken()
        AppObjectController.joshApplication.updateDeviceDetail()
        AppObjectController.joshApplication.userActive()
        DatabaseUtils.updateUserMessageSeen()
        setContentView(R.layout.activity_inbox)
        setToolbar()
        addObserver()
        lifecycle.addObserver(this)

        AppObjectController.clearDownloadMangerCallback()
        if (Mentor.getInstance().hasId()) {
            viewModel.getRegisterCourses()
            SyncChatService.syncChatWithServer()
            locationFetch()
            return
        }
        AppAnalytics.create(AnalyticsEvent.INBOX_SCREEN.NAME).push()



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
                                getLocationAndUpload();
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


    private fun addObserver() {
        compositeDisposable.add(
            RxBus.getDefault().toObservable()
                .subscribeOn(Schedulers.io()).subscribe({
                    if (it is InboxEntity) {
                        AppAnalytics.create(AnalyticsEvent.COURSE_SELECTED.NAME).addParam("course_id",it.conversation_id).push()
                        ConversationActivity.startConversionActivity(this, it)
                    }
                }, {
                    it.printStackTrace()

                })
        )


        viewModel.registerCourseNetworkLiveData.observe(this, Observer {
            if (it == null || it.isEmpty()) {
                startActivityForResult(
                    Intent(this, RegisterInfoActivity::class.java),
                    REGISTER_INFO_CODE
                )
            } else {
                recycler_view_inbox.removeAllViews()
                for (inbox in it) {
                    recycler_view_inbox.addView(
                        InboxViewHolder(
                            inbox
                        )
                    )
                }
                addEmptyView()
            }
        })

        viewModel.registerCourseMinimalLiveData.observe(this, Observer {
            if (it != null && it.isNotEmpty()) {
                for (inbox in it) {
                    recycler_view_inbox.addView(
                        InboxViewHolder(
                            inbox
                        )
                    )
                }
                addEmptyView()
            }
        })


    }

    private fun addEmptyView() {
        for (i in 1..8) {
            recycler_view_inbox.addView(EmptyHorizontalView())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REGISTER_INFO_CODE) {
            finish()
        }
    }

    private fun getLocationAndUpload() {
        val rxLocation = RxLocation(application)
        val locationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(5000)
        compositeDisposable.add(rxLocation.location().updates(locationRequest).subscribeOn(
            Schedulers.io()
        )
            .subscribe { location ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val request = UpdateUserLocality()
                        request.locality = SearchLocality(location.latitude, location.longitude)

                        val response: ProfileResponse =
                            AppObjectController.signUpNetworkService.updateUserAddressAsync(
                                Mentor.getInstance().getId(),
                                request
                            ).await()
                        Mentor.getInstance().setLocality(response.locality).update()
                        compositeDisposable.clear()

                    } catch (e: Exception) {
                        e.printStackTrace()
                        // onFailedToFetchLocation()
                    }

                }
            })

    }


    override fun onBackPressed() {
        AppAnalytics.create(AnalyticsEvent.BACK_PRESSED.NAME)
            .addParam("name", javaClass.simpleName)
            .push()
        super.onBackPressed()
    }



}
