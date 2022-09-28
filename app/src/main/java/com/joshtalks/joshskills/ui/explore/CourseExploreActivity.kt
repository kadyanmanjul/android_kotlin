package com.joshtalks.joshskills.ui.explore

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.tabs.TabLayoutMediator
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.*
import com.joshtalks.joshskills.databinding.ActivityCourseExploreBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.*
import com.joshtalks.joshskills.repository.server.CourseExploreModel
import com.joshtalks.joshskills.ui.course_details.CourseDetailsActivity
import com.joshtalks.joshskills.ui.inbox.PAYMENT_FOR_COURSE_CODE
import com.joshtalks.joshskills.ui.signup.FLOW_FROM
import com.joshtalks.joshskills.ui.signup.SignUpActivity
import com.joshtalks.joshskills.ui.subscription.StartSubscriptionActivity
import com.joshtalks.joshskills.util.showAppropriateMsg
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.set

const val COURSE_EXPLORER_SCREEN_NAME = "Course Explorer"
const val USER_COURSES = "user_courses"
const val IS_COURSES_CLICKABLE = "is_courses_clickable"
const val PREV_ACTIVITY = "previous_activity"

class CourseExploreActivity : CoreJoshActivity() {
    private var compositeDisposable = CompositeDisposable()
    private lateinit var courseExploreBinding: ActivityCourseExploreBinding
    private lateinit var appAnalytics: AppAnalytics
    private var prevAct: String? = EMPTY
    private var isClickable: Boolean = true
    private var screenEngagementModel: ScreenEngagementModel =
        ScreenEngagementModel(COURSE_EXPLORER_SCREEN_NAME)
    private val tabName: MutableList<String> = ArrayList()

    companion object {
        fun startCourseExploreActivity(
            context: Activity,
            requestCode: Int,
            list: MutableSet<InboxEntity>?,
            clearBackStack: Boolean = false,
            state: ActivityEnum,
            isClickable: Boolean = true
        ) {
            val intent = Intent(context, CourseExploreActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            list?.run {
                intent.putParcelableArrayListExtra(USER_COURSES, ArrayList(this))
            }
            intent.putExtra(IS_COURSES_CLICKABLE, isClickable)
            intent.putExtra(PREV_ACTIVITY, state.toString())
            if (clearBackStack) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivityForResult(intent, requestCode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        courseExploreBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_course_explore)
        courseExploreBinding.lifecycleOwner = this
        courseExploreBinding.handler = this
        appAnalytics = AppAnalytics.create(AnalyticsEvent.COURSE_EXPLORE.NAME)
            .addUserDetails()
            .addBasicParam()
        if (intent.hasExtra(PREV_ACTIVITY)) {
            prevAct = intent.getStringExtra(PREV_ACTIVITY)
            if (prevAct.isNullOrBlank().not()) {
                appAnalytics.addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, prevAct)
            }
        }
        initView()
        loadCourses()
        //  registerUserGAID()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun initView() {
        courseExploreBinding.titleTv.text = getString(R.string.explorer_courses)
        if (User.getInstance().isVerified) {
            courseExploreBinding.toolbar.inflateMenu(R.menu.logout_menu)
            courseExploreBinding.toolbar.setOnMenuItemClickListener {
                if (it?.itemId == R.id.menu_logout) {
                    MaterialDialog(this@CourseExploreActivity).show {
                        message(R.string.logout_message)
                        positiveButton(R.string.ok) {
                            MixPanelTracker.publishEvent(MixPanelEvent.LOGOUT_CLICKED)
                                .addParam(ParamKeys.LOGOUT,"ok")
                                .push()
                            AppAnalytics.create(AnalyticsEvent.LOGOUT_CLICKED.NAME)
                                .addUserDetails()
                                .addParam(AnalyticsEvent.USER_LOGGED_OUT.NAME, true).push()
                            val intent = Intent(
                                AppObjectController.joshApplication,
                                SignUpActivity::class.java
                            )
                            intent.apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                putExtra(FLOW_FROM, "CourseExploreActivity")
                            }
                            lifecycleScope.launch(Dispatchers.IO) {
                                PrefManager.logoutUser()
                                AppObjectController.joshApplication.startActivity(intent)
                            }
                        }
                        negativeButton(R.string.cancel) {
                            MixPanelTracker.publishEvent(MixPanelEvent.LOGOUT_CLICKED)
                                .addParam(ParamKeys.LOGOUT,"cancel")
                                .push()
                            AppAnalytics.create(AnalyticsEvent.LOGOUT_CLICKED.NAME)
                                .addUserDetails()
                                .addParam(AnalyticsEvent.USER_LOGGED_OUT.NAME, false).push()
                        }
                    }
                }
                return@setOnMenuItemClickListener true
            }
        }
        courseExploreBinding.toolbar.setOnMenuItemClickListener {
            if (it?.itemId == R.id.menu_logout) {
                MaterialDialog(this@CourseExploreActivity).show {
                    message(R.string.logout_message)
                    positiveButton(R.string.ok) {
                        MixPanelTracker.publishEvent(MixPanelEvent.LOGOUT_CLICKED)
                            .addParam(ParamKeys.LOGOUT,"ok")
                            .push()
                        logout()
                    }
                    negativeButton(R.string.cancel) {
                        MixPanelTracker.publishEvent(MixPanelEvent.LOGOUT_CLICKED)
                            .addParam(ParamKeys.LOGOUT,"cancel")
                            .push()
                        AppAnalytics.create(AnalyticsEvent.LOGOUT_CLICKED.NAME)
                            .addUserDetails()
                            .addParam(AnalyticsEvent.USER_LOGGED_OUT.NAME, false).push()
                    }
                }
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun initViewPagerTab() {
        TabLayoutMediator(
            courseExploreBinding.tabLayout, courseExploreBinding.courseListingRv
        ) { tab, position ->
            tab.text = tabName[position]
            logChipSelectedEvent(tabName[position])
        }.attach()
        courseExploreBinding.courseListingRv.offscreenPageLimit = 10
        courseExploreBinding.courseListingRv.isSaveEnabled = true
    }

    private fun loadCourses() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                var list: ArrayList<InboxEntity>? = null
                if (intent.hasExtra(USER_COURSES)) {
                    list = intent.getParcelableArrayListExtra(USER_COURSES)
                }
                if (intent.hasExtra(IS_COURSES_CLICKABLE)) {
                    isClickable = intent.getBooleanExtra(IS_COURSES_CLICKABLE, true)
                }
                var response = emptyList<CourseExploreModel>()
                if (isClickable) {
                    val data = HashMap<String, String>()
                    if (PrefManager.getStringValue(USER_UNIQUE_ID).isNotEmpty()) {
                        data["gaid"] = PrefManager.getStringValue(USER_UNIQUE_ID)
                    }
                    if (Mentor.getInstance().getId().isNotEmpty()) {
                        data["mentor"] = Mentor.getInstance().getId()
                    }
                    if (data.isNullOrEmpty()) {
                        data["is_default"] = "true"
                    }
                    response =
                        AppObjectController.signUpNetworkService.exploreCourses(data)
                } else {
                    response =
                        AppObjectController.signUpNetworkService.getFreeTrialCourses()
                }

                val languageSet: LinkedHashSet<String> = linkedSetOf()

                val listIterator =
                    response.toMutableList().listIterator()
                while (listIterator.hasNext()) {
                    val courseExploreModel = listIterator.next()
                    val resp = list?.find { it.courseId == courseExploreModel.course.toString() }
                    if (resp != null) {
                        listIterator.remove()
                    }
                    languageSet.add(courseExploreModel.language)
                }
                tabName.addAll(languageSet)

                val courseByMap: Map<Int, List<CourseExploreModel>> =
                    response.groupBy { it.languageId }

                withContext(Dispatchers.Main) {
                    courseExploreBinding.courseListingRv.adapter =
                        PractiseViewPagerAdapter(
                            this@CourseExploreActivity,
                            courseByMap,
                            isClickable
                        )
                    courseExploreBinding.progressBar.visibility = View.GONE
                    initViewPagerTab()
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
                withContext(Dispatchers.Main) {
                    courseExploreBinding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun registerUserGAID() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val requestRegisterGAId = RequestRegisterGAId()
                requestRegisterGAId.gaid = PrefManager.getStringValue(USER_UNIQUE_ID)
                requestRegisterGAId.installOn =
                    InstallReferrerModel.getPrefObject()?.installOn ?: (Date().time / 1000)
                requestRegisterGAId.utmMedium =
                    InstallReferrerModel.getPrefObject()?.utmMedium ?: EMPTY
                requestRegisterGAId.utmSource =
                    InstallReferrerModel.getPrefObject()?.utmSource ?: EMPTY
                val exploreType = PrefManager.getStringValue(EXPLORE_TYPE, false)
                requestRegisterGAId.exploreCardType =
                    if (exploreType.isNotBlank()) ExploreCardType.valueOf(exploreType) else null
                val resp =
                    AppObjectController.commonNetworkService.registerGAIdAsync(requestRegisterGAId)
                        .await()
                PrefManager.put(SERVER_GID_ID, resp.id)
                PrefManager.put(EXPLORE_TYPE, resp.exploreCardType!!.name, false)
            } catch (ex: Throwable) {
                // LogException.catchException(ex)
            }
            loadCourses()
        }
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

    private fun addObserver() {
        compositeDisposable.add(
            RxBus2.listen(CourseExploreModel::class.java).subscribe { courseExploreModel ->
                val extras: HashMap<String, String> = HashMap()
                extras["test_id"] = courseExploreModel.id?.toString() ?: EMPTY
                extras["course_name"] = courseExploreModel.courseName

                MixPanelTracker.publishEvent(MixPanelEvent.SHOW_COURSE_DETAILS)
                    .addParam(ParamKeys.TEST_ID,courseExploreModel.id)
                    .addParam(ParamKeys.COURSE_NAME,courseExploreModel.courseName)
                    .addParam(ParamKeys.COURSE_PRICE,courseExploreModel.amount)
                    .addParam(ParamKeys.COURSE_ID,PrefManager.getStringValue(CURRENT_COURSE_ID, false, DEFAULT_COURSE_ID))
                    .push()

                AppAnalytics.create(AnalyticsEvent.COURSE_THUMBNAIL_CLICKED.NAME)
                    .addBasicParam()
                    .addUserDetails()
                    .addParam(AnalyticsEvent.COURSE_NAME.NAME, courseExploreModel.courseName)
                    .addParam(AnalyticsEvent.COURSE_PRICE.NAME, courseExploreModel.amount)
                    .push()
//                MarketingAnalytics.viewContentEvent(
//                    applicationContext,
//                    courseExploreModel
//                )
                if (isClickable) {
                    when (courseExploreModel.cardType) {

                        ExploreCardType.NORMAL -> {
                            courseExploreModel.id?.let { testId ->
                                CourseDetailsActivity.startCourseDetailsActivity(
                                    activity = this,
                                    testId = testId,
                                    whatsappUrl = courseExploreModel.whatsappUrl,
                                    startedFrom = this@CourseExploreActivity.javaClass.simpleName,
                                    buySubscription = false

                                )
                            }
                        }

                        ExploreCardType.FFCOURSE,
                        ExploreCardType.FREETRIAL -> {
                            courseExploreModel.id?.let { testId ->
                                StartSubscriptionActivity.startActivity(
                                    this,
                                    testId,
                                    courseExploreModel.cardType,
                                    this::class.simpleName!!
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    private fun logChipSelectedEvent(selectedLanguage: String) {
        AppAnalytics.create(AnalyticsEvent.LANGUAGE_FILTER_CLICKED.NAME)
            .addUserDetails()
            .addBasicParam()
            .addParam(AnalyticsEvent.LANGUAGE_SELECTED.name, selectedLanguage)
            .push()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PAYMENT_FOR_COURSE_CODE && resultCode == Activity.RESULT_OK) {
            val resultIntent = Intent()
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    override fun onBackPressed() {
        onCancelResult()
        MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
        super.onBackPressed()
    }

    override fun onStart() {
        super.onStart()
        screenEngagementModel.startTime = System.currentTimeMillis()
    }

    override fun onStop() {
        screenEngagementModel.endTime = System.currentTimeMillis()
        appAnalytics.push()
        super.onStop()
    }

    private fun onCancelResult() {
        appAnalytics.addParam(AnalyticsEvent.BACK_BTN_EXPLORESCREEN.NAME, true)
        val resultIntent = Intent()
        setResult(Activity.RESULT_CANCELED, resultIntent)
        this.finish()
    }
}
