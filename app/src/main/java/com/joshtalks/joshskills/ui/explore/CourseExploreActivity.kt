package com.joshtalks.joshskills.ui.explore

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.INSTANCE_ID
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_UNIQUE_ID
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.databinding.ActivityCourseExploreBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.ScreenEngagementModel
import com.joshtalks.joshskills.repository.server.CourseExploreModel
import com.joshtalks.joshskills.ui.course_details.CourseDetailsActivity
import com.joshtalks.joshskills.ui.inbox.PAYMENT_FOR_COURSE_CODE
import com.joshtalks.joshskills.ui.signup.FLOW_FROM
import com.joshtalks.joshskills.ui.signup.SignUpActivity
import com.joshtalks.joshskills.util.showAppropriateMsg
import com.vanniktech.emoji.Utils
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_course_explore.language_chip_group
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.collections.set

const val COURSE_EXPLORER_SCREEN_NAME = "Course Explorer"
const val USER_COURSES = "user_courses"
const val PREV_ACTIVITY = "previous_activity"

class CourseExploreActivity : CoreJoshActivity() {
    private lateinit var adapter: CourseExploreAdapter
    private lateinit var courseList: ArrayList<CourseExploreModel>
    private lateinit var filteredCourseList: ArrayList<CourseExploreModel>
    private var selectedLanguage: String? = null
    private var compositeDisposable = CompositeDisposable()
    private lateinit var courseExploreBinding: ActivityCourseExploreBinding
    private lateinit var appAnalytics: AppAnalytics
    private var prevAct: String? = EMPTY
    private var screenEngagementModel: ScreenEngagementModel =
        ScreenEngagementModel(COURSE_EXPLORER_SCREEN_NAME)
    private lateinit var languageSet: HashSet<String>

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

        languageSet = HashSet()
        courseList = ArrayList()
        filteredCourseList = ArrayList()
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

    private fun initRV() {
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.isSmoothScrollbarEnabled = true
        courseExploreBinding.recyclerView
            .setHasFixedSize(true)
        courseExploreBinding.recyclerView.layoutManager = linearLayoutManager
        courseExploreBinding.recyclerView.itemAnimator = null
        courseExploreBinding.recyclerView.addItemDecoration(
            LayoutMarginDecoration(
                Utils.dpToPx(
                    this,
                    4f
                )
            )
        )

        adapter = CourseExploreAdapter(this, filteredCourseList)
        courseExploreBinding.recyclerView.adapter = adapter
    }

    private fun loadCourses() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = HashMap<String, String>()
                if (PrefManager.getStringValue(USER_UNIQUE_ID).isNotEmpty()) {
                    data["gaid"] = PrefManager.getStringValue(USER_UNIQUE_ID)
                }
                if (PrefManager.getStringValue(INSTANCE_ID, true).isNotEmpty()) {
                    data["instance"] = PrefManager.getStringValue(INSTANCE_ID, true)
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
                        languageSet.add(courseExploreModel.language ?: "")


                        courseList.add(courseExploreModel)
                        /*courseExploreBinding.recyclerView.addView(
                            CourseExplorerViewHolder(
                                courseExploreModel
                            )
                        )*/
                    }
                    renderLanguageChips()
                    filteredCourseList.clear()
                    filteredCourseList.addAll(courseList)
                    adapter.notifyDataSetChanged()
                    courseExploreBinding.progressBar.visibility = View.GONE
                }

            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
                CoroutineScope(Dispatchers.Main).launch {
                    courseExploreBinding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    fun filterCourses() {
        filteredCourseList.clear()
        if ("".equals(selectedLanguage))
            filteredCourseList.addAll(courseList)
        else
            filteredCourseList.addAll(courseList.filter {
                it.language.equals(
                    selectedLanguage,
                    true
                )
            })
        adapter.notifyDataSetChanged()
        courseExploreBinding.recyclerView.scrollToPosition(0)
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
            it.id?.let {
                CourseDetailsActivity.startCourseDetailsActivity(
                    this,
                    it,
                    this@CourseExploreActivity.javaClass.simpleName
                )
            }
        })

        language_chip_group.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == -1)
                selectedLanguage = ""
            else
                selectedLanguage = languageSet.filter { languageSet.indexOf(it) == checkedId }[0]
            filterCourses()
        }
    }

    private fun renderLanguageChips() {
//        txtCategoryName.text = selectedLanguage ?: ""
        language_chip_group.removeAllViews()
        languageSet.forEach {
            val chip = LayoutInflater.from(this)
                .inflate(R.layout.language_filter_item, language_chip_group, false) as Chip
            chip.text = it.capitalize()
            chip.tag = it
            chip.id = languageSet.indexOf(it)
            language_chip_group.addView(chip)
        }
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
        appAnalytics.addParam(AnalyticsEvent.BACK_BTN_EXPLORESCREEN.NAME, true)
        val resultIntent = Intent()
        setResult(Activity.RESULT_CANCELED, resultIntent)
        this.finish()
    }

}
