package com.joshtalks.joshskills.ui.explore

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.crashlytics.android.Crashlytics
import com.google.android.material.appbar.MaterialToolbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.databinding.ActivityCourseExploreBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.ScreenEngagementModel
import com.joshtalks.joshskills.repository.server.CourseExploreModel
import com.joshtalks.joshskills.ui.inbox.PAYMENT_FOR_COURSE_CODE
import com.joshtalks.joshskills.ui.payment.PaymentActivity
import com.joshtalks.joshskills.ui.signup_v2.FLOW_FROM
import com.joshtalks.joshskills.ui.signup_v2.SignUpV2Activity
import com.joshtalks.joshskills.ui.view_holders.CourseExplorerViewHolder
import com.vanniktech.emoji.Utils
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
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

    companion object {
        fun startCourseExploreActivity(
            context: Activity,
            requestCode: Int,
            list: MutableSet<InboxEntity>?, clearBackStack: Boolean = false, state: ActivityEnum
        ) {
            val intent = Intent(context, CourseExploreActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
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
        initRV()
        initView()
        loadCourses()
        appAnalytics = AppAnalytics.create(AnalyticsEvent.COURSE_EXPLORE.NAME)
            .addUserDetails()
            .addBasicParam()
        if (prevAct.isNullOrBlank().not())
            appAnalytics.addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, prevAct)

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun initView() {
        val titleView = findViewById<AppCompatTextView>(R.id.text_message_title)
        titleView.text = getString(R.string.explorer_courses)
        findViewById<View>(R.id.iv_back).visibility = View.VISIBLE
        findViewById<View>(R.id.iv_back).setOnClickListener {
            onCancelResult()
        }
        if (Mentor.getInstance().hasId()) {
            findViewById<MaterialToolbar>(R.id.toolbar).inflateMenu(R.menu.logout_menu)
        }
        findViewById<MaterialToolbar>(R.id.toolbar).setOnMenuItemClickListener {
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
                                SignUpV2Activity::class.java
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

    private fun initRV() {
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.isSmoothScrollbarEnabled = true
        courseExploreBinding.recyclerView.builder
            .setHasFixedSize(true)
            .setLayoutManager(linearLayoutManager)
        courseExploreBinding.recyclerView.itemAnimator = null
        // TODO on Scrolled Event
        courseExploreBinding.recyclerView.addItemDecoration(
            LayoutMarginDecoration(
                Utils.dpToPx(
                    this,
                    4f
                )
            )
        )
    }

    private fun loadCourses() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = HashMap<String, String>()
                if (PrefManager.getStringValue(USER_UNIQUE_ID).isNotEmpty()) {
                    data["gaid"] = PrefManager.getStringValue(USER_UNIQUE_ID)
                }
                if (PrefManager.getStringValue(INSTANCE_ID).isNotEmpty()) {
                    data["instance"] = PrefManager.getStringValue(INSTANCE_ID)
                }
                if (Mentor.getInstance().getId().isNotEmpty()) {
                    data["mentor"] = Mentor.getInstance().getId()
                }
                if (data.isNullOrEmpty()) {
                    data["is_default"] = "true"
                }
                val response: List<CourseExploreModel> =
                    AppObjectController.signUpNetworkService.explorerCourse(data)
                CoroutineScope(Dispatchers.Main).launch {
                    var list: ArrayList<InboxEntity>? = null
                    if (intent.hasExtra(USER_COURSES)) {
                        list = intent.getParcelableArrayListExtra(USER_COURSES)
                    }
                    if (intent.hasExtra(PREV_ACTIVITY)) {
                        prevAct = intent.getStringExtra(PREV_ACTIVITY)
                    }
                    courseExploreBinding.recyclerView.removeAllViews()
                    response.forEach { courseExploreModel ->
                        list?.let { it ->
                            val entity: InboxEntity? =
                                it.find { it.courseId == courseExploreModel.course.toString() }
                            if (entity != null) {
                                return@forEach
                            }
                        }
                        courseExploreBinding.recyclerView.addView(
                            CourseExplorerViewHolder(
                                courseExploreModel
                            )
                        )
                    }
                    courseExploreBinding.progressBar.visibility = View.GONE
                }

            } catch (ex: Throwable) {
                LogException.catchException(ex)
                CoroutineScope(Dispatchers.Main).launch {
                    courseExploreBinding.progressBar.visibility = View.GONE
                }
                when (ex) {
                    is HttpException -> {
                        showToast(getString(R.string.generic_message_for_error))
                    }
                    is SocketTimeoutException, is UnknownHostException -> {
                        showToast(getString(R.string.internet_not_available_msz))
                    }
                    else -> {
                        Crashlytics.logException(ex)
                    }
                }
            }
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
        compositeDisposable.add(RxBus2.listen(CourseExploreModel::class.java).subscribe {
            val extras: HashMap<String, String> = HashMap()
            extras["test_id"] = it.id?.toString() ?: EMPTY
            extras["course_name"] = it.courseName
            AppAnalytics.create(AnalyticsEvent.COURSE_THUMBNAIL_CLICKED.NAME)
                .addBasicParam()
                .addUserDetails()
                .addParam(AnalyticsEvent.COURSE_NAME.NAME, it.courseName)
                .addParam(AnalyticsEvent.COURSE_PRICE.NAME, it.amount)
                .push()
            PaymentActivity.startPaymentActivity(this, PAYMENT_FOR_COURSE_CODE, it)
        })
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
        WorkMangerAdmin.screenAnalyticsWorker(screenEngagementModel)
        appAnalytics.push()
        super.onStop()
    }

    private fun onCancelResult() {
        appAnalytics.addParam(AnalyticsEvent.BACK_BTN_EXPLORESCREEN.NAME, true).push()
        val resultIntent = Intent()
        setResult(Activity.RESULT_CANCELED, resultIntent)
        this.finish()
    }

}
