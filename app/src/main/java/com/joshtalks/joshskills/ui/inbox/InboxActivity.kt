package com.joshtalks.joshskills.ui.inbox

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.os.Process
import android.view.View
import android.view.View.GONE
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.constants.CALLING_SERVICE_ACTION
import com.joshtalks.joshskills.base.constants.PREF_KEY_MAIN_PROCESS_PID
import com.joshtalks.joshskills.base.constants.SERVICE_ACTION_MAIN_PROCESS_IN_BACKGROUND
import com.joshtalks.joshskills.base.constants.SERVICE_BROADCAST_KEY
import com.joshtalks.joshskills.base.constants.START_SERVICE
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.core.interfaces.OnOpenCourseListener
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.*
import com.joshtalks.joshskills.ui.chat.ConversationActivity
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.inbox.adapter.InboxAdapter
import com.joshtalks.joshskills.ui.newonboarding.OnBoardingActivityNew
import com.joshtalks.joshskills.ui.payment.FreeTrialPaymentActivity
import com.joshtalks.joshskills.ui.referral.ReferralActivity
import com.joshtalks.joshskills.ui.referral.ReferralViewModel
import com.joshtalks.joshskills.ui.settings.SettingsActivity
import com.joshtalks.joshskills.ui.voip.WebRtcService
import com.joshtalks.joshskills.util.FileUploadService
import com.joshtalks.joshskills.voip.data.CallingRemoteService
import io.agora.rtc.RtcEngine
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_inbox.*
import kotlinx.android.synthetic.main.find_more_layout.*
import kotlinx.android.synthetic.main.fragment_listen_practise.*
import kotlinx.android.synthetic.main.inbox_toolbar.*
import kotlinx.android.synthetic.main.top_free_trial_expire_time_tooltip_view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

const val REGISTER_INFO_CODE = 2001
const val COURSE_EXPLORER_CODE = 2002
const val COURSE_EXPLORER_WITHOUT_CODE = 2003
const val PAYMENT_FOR_COURSE_CODE = 2004
const val REQ_CODE_VERSION_UPDATE = 530
const val USER_DETAILS_CODE = 1001
const val TRIAL_COURSE_ID = "76"
const val SUBSCRIPTION_COURSE_ID = "60"
const val IS_FROM_NEW_ONBOARDING = "is_from_new_on_boarding_flow"

class InboxActivity : InboxBaseActivity(), LifecycleObserver, OnOpenCourseListener {

    private var popupMenu: PopupMenu? = null
    private var compositeDisposable = CompositeDisposable()
    private lateinit var findMoreLayout: View
    var isPermissionRequired: Boolean = true
    private val courseListSet: MutableSet<InboxEntity> = hashSetOf()
    private val inboxAdapter: InboxAdapter by lazy { InboxAdapter(this, this) }

    private val refViewModel: ReferralViewModel by lazy {
        ViewModelProvider(this).get(ReferralViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WorkManagerAdmin.requiredTaskInLandingPage()
        FileUploadService.uploadAllPendingTasks(AppObjectController.joshApplication)
        AppAnalytics.create(AnalyticsEvent.INBOX_SCREEN.NAME).push()
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(this)
        setContentView(R.layout.activity_inbox)
        initView()
        addLiveDataObservable()
        addAfterTime()
    }

    private fun addAfterTime() {
        workInBackground()
        handelIntentAction()
        viewModel.getTotalWatchTime()
    }

    private fun initView() {
        text_message_title.text = getString(R.string.inbox_header)
        iv_reminder.visibility = GONE
        iv_setting.visibility = View.VISIBLE

        iv_icon_referral.setOnClickListener {
            refViewModel.saveImpression(IMPRESSION_REFER_VIA_INBOX_ICON)

            ReferralActivity.startReferralActivity(this@InboxActivity)
        }
        
        findMoreLayout = findViewById(R.id.parent_layout)
        recycler_view_inbox.apply {
            itemAnimator = null
            layoutManager = LinearLayoutManager(applicationContext).apply {
                isSmoothScrollbarEnabled = true
            }
        }
        recycler_view_inbox.addItemDecoration(
            LayoutMarginDecoration(
                Utils.dpToPx(
                    applicationContext,
                    6f
                )
            )
        )
        recycler_view_inbox.adapter = inboxAdapter
        iv_setting.setOnClickListener {
            openPopupMenu(it)
        }
        find_more.setOnClickListener {
            courseExploreClick()
        }
        find_more_new.setOnClickListener {
            courseExploreClick()
        }
        buy_english_course.setOnClickListener {
            FreeTrialPaymentActivity.startFreeTrialPaymentActivity(
                this,
                AppObjectController.getFirebaseRemoteConfig().getString(
                    FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID
                )
            )
        }
    }

    private fun openPopupMenu(view: View) {
        if (popupMenu == null) {
            popupMenu = PopupMenu(this, view, R.style.setting_menu_style)
            popupMenu?.inflate(R.menu.more_options_menu)
            popupMenu?.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_referral -> {
                        refViewModel.saveImpression(IMPRESSION_REFER_VIA_INBOX_MENU)
                        ReferralActivity.startReferralActivity(this@InboxActivity)
                        return@setOnMenuItemClickListener true
                    }
                    R.id.menu_help -> {
                        openHelpActivity()
                    }
                    R.id.menu_settings ->
                        openSettingActivity()
                }
                return@setOnMenuItemClickListener false
            }
        }
        popupMenu?.show()
    }

    private fun openSettingActivity() {
        openSettingActivity.launch(SettingsActivity.getIntent(this))
    }

    private fun workInBackground() {
        lifecycleScope.launchWhenResumed {
            processIntent(intent)
            checkInAppUpdate()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        processIntent(intent)
        this.intent = intent
        handelIntentAction()
    }

    private fun addLiveDataObservable() {
        lifecycleScope.launchWhenStarted {
            viewModel.registerCourseNetworkData.collect {
                if (it.isNullOrEmpty()) {
                    openCourseExplorer()
                } else {
                    addCourseInRecyclerView(it)
                }
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.registerCourseLocalData.collect {
                addCourseInRecyclerView(it)
            }
        }
    }

    private fun addCourseInRecyclerView(items: List<InboxEntity>) {
        if (items.isEmpty()) {
            return
        }
        var haveFreeTrialCourse = false
        lifecycleScope.launch(Dispatchers.Default) {
            val temp: ArrayList<InboxEntity> = arrayListOf()
            var isServiceStarted = false
            items.filter { it.isCapsuleCourse }.sortedByDescending { it.courseCreatedDate }
                .let { courseList ->
                    courseList.forEach { inboxEntity ->
                        // User is Free Trail
                        if(isServiceStarted.not()) {
                            isServiceStarted = true
                            val broadcastIntent=Intent().apply {
                                action = CALLING_SERVICE_ACTION
                                putExtra(SERVICE_BROADCAST_KEY, START_SERVICE)
                            }
                            LocalBroadcastManager.getInstance(this@InboxActivity).sendBroadcast(broadcastIntent)
                        }
                        if (inboxEntity.isCourseBought.not()) {
                            haveFreeTrialCourse = true
                        }
                    }
                    temp.addAll(courseList)
                    if (courseList.isNullOrEmpty().not()) {
                        val capsuleCourse = courseList[0]
                        PrefManager.put(CURRENT_COURSE_ID, capsuleCourse.courseId)
                        PrefManager.put(PAID_COURSE_TEST_ID, capsuleCourse.paidTestId ?: "")
                    }
                }

            items.filter { (it.created == null || it.created == 0L) && it.courseId != TRIAL_COURSE_ID && it.isCapsuleCourse.not() }
                .sortedByDescending { it.courseCreatedDate }.let {
                    temp.addAll(it)
                }

            items.filter { it.created != null && it.created != 0L && it.isCapsuleCourse.not() }
                .sortedByDescending { it.created }.let {
                    temp.addAll(it)
                }

            courseListSet.addAll(temp)
            lifecycleScope.launch(Dispatchers.Main) {
                inboxAdapter.addItems(temp)
                if (haveFreeTrialCourse) {
                    findMoreLayout.findViewById<MaterialTextView>(R.id.find_more).visibility=View.GONE
                    findMoreLayout.findViewById<MaterialTextView>(R.id.find_more_new).visibility=View.VISIBLE
                    findMoreLayout.findViewById<MaterialTextView>(R.id.buy_english_course).visibility=View.VISIBLE
                } else {
                    findMoreLayout.findViewById<MaterialTextView>(R.id.find_more).visibility=View.VISIBLE
                    findMoreLayout.findViewById<MaterialTextView>(R.id.find_more_new).visibility=View.GONE
                    findMoreLayout.findViewById<MaterialTextView>(R.id.buy_english_course).visibility=View.GONE
                }
            }
        }
        if (findMoreLayout.visibility != View.VISIBLE && PrefManager.getIntValue(
                INBOX_SCREEN_VISIT_COUNT
            ) >= 2
        ) {
            findMoreLayout.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        PrefManager.put(
            INBOX_SCREEN_VISIT_COUNT,
            PrefManager.getIntValue(INBOX_SCREEN_VISIT_COUNT).plus(1)
        )
        if (findMoreLayout.visibility != View.VISIBLE && PrefManager.getIntValue(
                INBOX_SCREEN_VISIT_COUNT
            ) >= 2
        ) {
            findMoreLayout.visibility = View.VISIBLE
        }
        try {
            inboxAdapter.notifyDataSetChanged()

        } catch (ex:Exception){

        }
        Runtime.getRuntime().gc()
        viewModel.getRegisterCourses()
        viewModel.getProfileData(Mentor.getInstance().getId())
        viewModel.handleGroupTimeTokens()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    override fun openCourseExplorer() {
        CourseExploreActivity.startCourseExploreActivity(
            this,
            COURSE_EXPLORER_CODE,
            courseListSet, state = ActivityEnum.Inbox
        )
    }

    override fun openCourseSelectionExplorer(alreadyHaveCourses: Boolean) {
        OnBoardingActivityNew.startOnBoardingActivity(
            this,
            COURSE_EXPLORER_NEW,
            true,
            alreadyHaveCourses
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
                val forceUpdateMinVersion =
                    AppObjectController.getFirebaseRemoteConfig()
                        .getLong("force_upgrade_after_version")
                val forceUpdateFlag =
                    AppObjectController.getFirebaseRemoteConfig().getBoolean("update_force")
                val currentAppVersion = BuildConfig.VERSION_CODE

                if (currentAppVersion <= forceUpdateMinVersion && forceUpdateFlag) {
                    inAppUpdateManager?.checkForAppUpdate()
                }
            }
        } else if (requestCode == COURSE_EXPLORER_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                overridePendingTransition(0, 0)
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                finish()
                overridePendingTransition(0, 0)
                startActivity(intent)
            } else if (resultCode == Activity.RESULT_CANCELED && courseListSet.isNullOrEmpty()) {
                this@InboxActivity.finish()
            }
        } else if (requestCode == USER_DETAILS_CODE && resultCode == Activity.RESULT_CANCELED) {
            finish()
        }
    }

    override fun onUpdateLocation(location: Location) {
        lifecycleScope.launch(Dispatchers.IO) {
            uploadUserLocation(location)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (WebRtcService.isCallOnGoing.value == false) {
            RtcEngine.destroy()
        }
        inAppUpdateManager = null
        inAppUpdateManager?.onDestroy()
    }

    override fun onClick(inboxEntity: InboxEntity) {
        PrefManager.put(ONBOARDING_STAGE, OnBoardingStage.COURSE_OPENED.value)
        ConversationActivity.startConversionActivity(this, inboxEntity)
    }

    companion object {

        fun getInboxIntent(context: Context) = Intent(context, InboxActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    }

    override fun onBackPressed() {
        super.onBackPressed()
        applicationClosed()
    }

    private fun applicationClosed() {
        val remoteServiceIntent =
            Intent(
                AppObjectController.joshApplication,
                CallingRemoteService::class.java
            )
        remoteServiceIntent.action = SERVICE_ACTION_MAIN_PROCESS_IN_BACKGROUND
        AppObjectController.joshApplication.startService(remoteServiceIntent)
    }

}
