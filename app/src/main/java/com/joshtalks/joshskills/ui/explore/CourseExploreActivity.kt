package com.joshtalks.joshskills.ui.explore

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.Target
import com.crashlytics.android.Crashlytics
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.EXPLORE_TYPE
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.core.INSTANCE_ID
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.SERVER_GID_ID
import com.joshtalks.joshskills.core.USER_UNIQUE_ID
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.ActivityCourseExploreBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.ExploreCardType
import com.joshtalks.joshskills.repository.local.model.InstallReferrerModel
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.RequestRegisterGAId
import com.joshtalks.joshskills.repository.local.model.ScreenEngagementModel
import com.joshtalks.joshskills.repository.server.CourseExploreModel
import com.joshtalks.joshskills.ui.course_details.CourseDetailsActivity
import com.joshtalks.joshskills.ui.inbox.PAYMENT_FOR_COURSE_CODE
import com.joshtalks.joshskills.ui.signup.FLOW_FROM
import com.joshtalks.joshskills.ui.signup.SignUpActivity
import com.joshtalks.joshskills.ui.subscription.StartSubscriptionActivity
import com.joshtalks.joshskills.util.showAppropriateMsg
import com.vanniktech.emoji.Utils
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_course_explore.language_chip_group
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.collections.set

const val COURSE_EXPLORER_SCREEN_NAME = "Course Explorer"
const val USER_COURSES = "user_courses"
const val PREV_ACTIVITY = "previous_activity"

class CourseExploreActivity : CoreJoshActivity() {
    private val languageMap: HashMap<String, ArrayList<CourseExploreModel>> = HashMap()
    private lateinit var adapter: CourseExploreAdapter
    private val courseList: ArrayList<CourseExploreModel> = ArrayList()
    private var selectedLanguage: String = EMPTY
    private var compositeDisposable = CompositeDisposable()
    private lateinit var courseExploreBinding: ActivityCourseExploreBinding
    private lateinit var appAnalytics: AppAnalytics
    private var prevAct: String? = EMPTY
    private var screenEngagementModel: ScreenEngagementModel =
        ScreenEngagementModel(COURSE_EXPLORER_SCREEN_NAME)
    private var languageList: MutableList<String> = ArrayList()

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
        registerUserGAID()
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
                    6f
                )
            )
        )

        adapter = CourseExploreAdapter(this, courseList, languageMap)
        courseExploreBinding.recyclerView.adapter = adapter
    }

    private fun loadCourses() {
        CoroutineScope(Dispatchers.IO).launch {

            val exploreType = PrefManager.getStringValue(EXPLORE_TYPE, true)
            WorkMangerAdmin.registerUserGAID(
                null,
                if (exploreType.isNotBlank()) exploreType else null
            )

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
                    AppObjectController.signUpNetworkService.exploreCourses(data)
                CoroutineScope(Dispatchers.Main).launch {
                    var list: ArrayList<InboxEntity>? = null
                    if (intent.hasExtra(USER_COURSES)) {
                        list = intent.getParcelableArrayListExtra(USER_COURSES)
                    }
                    if (intent.hasExtra(PREV_ACTIVITY)) {
                        prevAct = intent.getStringExtra(PREV_ACTIVITY)
                    }
                    courseExploreBinding.recyclerView.removeAllViews()
                    val languageSet: HashSet<String> = HashSet()
                    response.forEach { courseExploreModel ->
                        list?.let { inboxEntityList ->
                            val entity: InboxEntity? =
                                inboxEntityList.find { it.courseId == courseExploreModel.course.toString() }
                            if (entity != null) {
                                return@forEach
                            }
                        }
                        if (courseExploreModel.cardType == ExploreCardType.NORMAL) {
                            courseExploreModel.language?.let { languageSet.add(it.capitalize()) }
                            courseList.add(courseExploreModel)

                            courseExploreModel.language?.let {
                                //Creating language set for filter option chips
                                languageSet.add(it.capitalize())

                                //manage map to at the time of filtering.
                                val courses: ArrayList<CourseExploreModel>? =
                                    languageMap.get(it.capitalize())
                                if (courses != null) {
                                    //Map already has key with course of this language. so add course to same list.
                                    courses.add(courseExploreModel)
                                } else {
                                    //This is the first course with this language so add new key to map.
                                    val newLanguageCourse: ArrayList<CourseExploreModel> =
                                        ArrayList()
                                    newLanguageCourse.add(courseExploreModel)
                                    languageMap.put(it.capitalize(), newLanguageCourse)
                                }
                            }
                        } else {
                            setSubscriptionHeaderView(courseExploreModel)
                        }
                    }

                    languageList = languageSet.toMutableList()
                    if (languageList.contains("Hindi")) {
                        languageList.remove("Hindi")
                        languageList.add(0, "Hindi")
                    }
                    renderLanguageChips()
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

    private fun registerUserGAID() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val requestRegisterGAId = RequestRegisterGAId()
                requestRegisterGAId.gaid = PrefManager.getStringValue(USER_UNIQUE_ID)
                requestRegisterGAId.installOn =
                    InstallReferrerModel.getPrefObject()?.installOn ?: Date().time
                requestRegisterGAId.utmMedium =
                    InstallReferrerModel.getPrefObject()?.utmMedium ?: EMPTY
                requestRegisterGAId.utmSource =
                    InstallReferrerModel.getPrefObject()?.utmSource ?: EMPTY
                requestRegisterGAId.test = null
                val exploreType = PrefManager.getStringValue(EXPLORE_TYPE, true)
                requestRegisterGAId.exploreCardType =
                    if (exploreType.isNotBlank()) ExploreCardType.valueOf(exploreType) else null
                val resp =
                    AppObjectController.commonNetworkService.registerGAIdAsync(requestRegisterGAId)
                        .await()
                PrefManager.put(SERVER_GID_ID, resp.id)
                PrefManager.put(EXPLORE_TYPE, resp.exploreCardType!!.name, true)
            } catch (ex: Throwable) {
                //LogException.catchException(ex)
            }
            loadCourses()
        }
    }

    private fun filterCourses() {
        adapter.filter.filter(selectedLanguage)
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

                when (courseExploreModel.cardType) {

                    ExploreCardType.NORMAL,
                    ExploreCardType.SUBSCRIPTION -> {
                        courseExploreModel.id?.let { testId ->
                            CourseDetailsActivity.startCourseDetailsActivity(
                                this,
                                testId,
                                this@CourseExploreActivity.javaClass.simpleName
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

        language_chip_group.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == -1) {
                selectedLanguage = EMPTY
            } else {
                selectedLanguage = languageList.filter { languageList.indexOf(it) == checkedId }[0]
                logChipSelectedEvent(selectedLanguage)
            }
            filterCourses()
        }
    }

    private fun logChipSelectedEvent(selectedLanguage: String) {
        AppAnalytics.create(AnalyticsEvent.LANGUAGE_FILTER_CLICKED.NAME)
            .addUserDetails()
            .addBasicParam()
            .addParam(AnalyticsEvent.LANGUAGE_SELECTED.name, selectedLanguage)
            .push()
    }

    private fun renderLanguageChips() {
        language_chip_group.removeAllViews()
        languageList.forEach {
            val chip = LayoutInflater.from(this)
                .inflate(R.layout.language_filter_item, language_chip_group, false) as Chip
            chip.text = it.capitalize()
            chip.tag = it
            chip.id = languageList.indexOf(it)
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
        appAnalytics.push()
        super.onStop()
    }

    private fun onCancelResult() {
        appAnalytics.addParam(AnalyticsEvent.BACK_BTN_EXPLORESCREEN.NAME, true)
        val resultIntent = Intent()
        setResult(Activity.RESULT_CANCELED, resultIntent)
        this.finish()
    }

    private fun setSubscriptionHeaderView(courseExploreModel: CourseExploreModel) {
        val headerImage = findViewById<AppCompatImageView>(R.id.image_view)
        val headerBuyNowBtn = findViewById<MaterialButton>(R.id.buy_now_button)
        val headerContainer = findViewById<View>(R.id.courseListHeader)

        headerContainer.visibility = View.VISIBLE
        try {
            Glide.with(this@CourseExploreActivity)
                .load(courseExploreModel.imageUrl)
                .override(Target.SIZE_ORIGINAL)
                .optionalTransform(
                    WebpDrawable::class.java,
                    WebpDrawableTransformation(CircleCrop())
                )
                .into(headerImage)
        } catch (ex: Exception) {
            Crashlytics.logException(ex)
        }
        if (courseExploreModel.isClickable) {
            headerBuyNowBtn.visibility = View.VISIBLE
            headerBuyNowBtn.text =
                AppObjectController.getFirebaseRemoteConfig()
                    .getString("show_details_label")

            headerBuyNowBtn.setOnClickListener {
                RxBus2.publish(courseExploreModel)
            }

            headerImage.setOnClickListener {
                RxBus2.publish(courseExploreModel)
            }
        } else {
            headerBuyNowBtn.visibility = View.GONE
            headerImage.isClickable = false
            headerImage.isFocusable = false

            headerBuyNowBtn.setOnClickListener {
                showToast(
                    AppObjectController.getFirebaseRemoteConfig()
                        .getString(FirebaseRemoteConfigKey.FFCOURSE_CARD_CLICK_MSG)
                )
            }

            headerImage.setOnClickListener {
                showToast(
                    AppObjectController.getFirebaseRemoteConfig()
                        .getString(FirebaseRemoteConfigKey.FFCOURSE_CARD_CLICK_MSG)
                )
            }

        }
    }

}
