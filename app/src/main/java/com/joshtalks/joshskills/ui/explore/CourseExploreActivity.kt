package com.joshtalks.joshskills.ui.explore

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.tabs.TabLayoutMediator
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.databinding.ActivityCourseExploreBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.ExploreCardType
import com.joshtalks.joshskills.repository.local.model.ScreenEngagementModel
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.CourseExploreModel
import com.joshtalks.joshskills.ui.course_details.CourseDetailsActivity
import com.joshtalks.joshskills.ui.explore.v2.SegmentedViewPagerAdapter
import com.joshtalks.joshskills.ui.inbox.PAYMENT_FOR_COURSE_CODE
import com.joshtalks.joshskills.ui.signup.FLOW_FROM
import com.joshtalks.joshskills.ui.signup.SignUpActivity
import com.joshtalks.joshskills.ui.subscription.StartSubscriptionActivity
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.collections.set

const val COURSE_EXPLORER_SCREEN_NAME = "Course Explorer"
const val USER_COURSES = "user_courses"
const val PREV_ACTIVITY = "previous_activity"

class CourseExploreActivity : CoreJoshActivity() {
    private var compositeDisposable = CompositeDisposable()
    private lateinit var courseExploreBinding: ActivityCourseExploreBinding
    private lateinit var appAnalytics: AppAnalytics
    private var prevAct: String? = EMPTY
    private var screenEngagementModel: ScreenEngagementModel =
        ScreenEngagementModel(COURSE_EXPLORER_SCREEN_NAME)
    private val tabName: MutableList<String> = ArrayList()

    private val viewModel: CourseExploreViewModel by lazy {
        ViewModelProvider(this).get(CourseExploreViewModel::class.java)
    }

    companion object {
        fun startCourseExploreActivity(
            context: Activity,
            requestCode: Int,
            list: MutableSet<InboxEntity>?, clearBackStack: Boolean = false, state: ActivityEnum
        ) {
            val intent = Intent(context, CourseExploreActivity::class.java)
            list?.run {
                intent.putParcelableArrayListExtra(USER_COURSES, ArrayList(this))
            }
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
        addObserver()

        val list: ArrayList<InboxEntity>? = if (intent.hasExtra(USER_COURSES)) {
            intent.getParcelableArrayListExtra(USER_COURSES)
        } else {
            null
        }
        //viewModel.getCourse(list)
        viewModel.getRecommendCourses()
        //courseExploreBinding.courseListingRv.isUserInputEnabled = false
    }

    private fun addObserver() {
        viewModel.apiCallStatusLiveData.observe(this, {
            courseExploreBinding.progressBar.visibility = View.GONE
        })
        viewModel.languageListLiveData.observe(this, {
            tabName.addAll(it)
        })
        viewModel.courseListLiveData.observe(this, {
            courseExploreBinding.courseListingRv.adapter =
                CourseListingAdapter(this@CourseExploreActivity, it)
            courseExploreBinding.progressBar.visibility = View.GONE
            initViewPagerTab()
        })
        viewModel.recommendSegment.observe(this, {
            courseExploreBinding.courseListingRv.adapter =
                SegmentedViewPagerAdapter(this@CourseExploreActivity, it)
            courseExploreBinding.progressBar.visibility = View.GONE
            initViewPagerTab()
        })
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
                            AppAnalytics.create(AnalyticsEvent.LOGOUT_CLICKED.NAME)
                                .addUserDetails()
                                .addParam(AnalyticsEvent.USER_LOGGED_OUT.NAME, true).push()
                            val intent =
                                Intent(
                                    AppObjectController.joshApplication,
                                    SignUpActivity::class.java
                                )
                            intent.apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                putExtra(FLOW_FROM, "CourseExploreActivity")
                            }
                            CoroutineScope(Dispatchers.IO).launch {
                                PrefManager.clearUser()
                                AppObjectController.joshApplication.startActivity(intent)
                            }
                        }
                        negativeButton(R.string.cancel) {
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
                        logout()
                    }
                    negativeButton(R.string.cancel) {
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
    }


    override fun onResume() {
        super.onResume()
        Runtime.getRuntime().gc()
        addRXBusObserver()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    private fun addRXBusObserver() {
        compositeDisposable.add(
            RxBus2.listen(CourseExploreModel::class.java).subscribe { courseExploreModel ->
                val extras: HashMap<String, String> = HashMap()
                extras["test_id"] = courseExploreModel.id?.toString() ?: EMPTY
                extras["course_name"] = courseExploreModel.courseName
                AppAnalytics.create(AnalyticsEvent.COURSE_THUMBNAIL_CLICKED.NAME)
                    .addBasicParam()
                    .addUserDetails()
                    .addParam(AnalyticsEvent.COURSE_NAME.NAME, courseExploreModel.courseName)
                    .addParam(AnalyticsEvent.COURSE_PRICE.NAME, courseExploreModel.amount)
                    .push()
                MarketingAnalytics.viewContentEvent(
                    applicationContext,
                    courseExploreModel
                )

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
            })
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
