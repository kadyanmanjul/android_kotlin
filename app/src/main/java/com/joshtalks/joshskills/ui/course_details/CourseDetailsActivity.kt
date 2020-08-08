package com.joshtalks.joshskills.ui.course_details

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Slide
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.Target
import com.google.android.material.appbar.AppBarLayout
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.EXPLORE_TYPE
import com.joshtalks.joshskills.core.IS_TRIAL_STARTED
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.STARTED_FROM
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.VERSION
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.ActivityCourseDetailsBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.DownloadSyllabusEvent
import com.joshtalks.joshskills.repository.local.eventbus.GotoCourseCard
import com.joshtalks.joshskills.repository.local.eventbus.ImageShowEvent
import com.joshtalks.joshskills.repository.local.eventbus.VideoShowEvent
import com.joshtalks.joshskills.repository.local.model.ExploreCardType
import com.joshtalks.joshskills.repository.server.course_detail.AboutJosh
import com.joshtalks.joshskills.repository.server.course_detail.Card
import com.joshtalks.joshskills.repository.server.course_detail.CardType
import com.joshtalks.joshskills.repository.server.course_detail.CourseOverviewData
import com.joshtalks.joshskills.repository.server.course_detail.DemoLesson
import com.joshtalks.joshskills.repository.server.course_detail.FAQData
import com.joshtalks.joshskills.repository.server.course_detail.Guidelines
import com.joshtalks.joshskills.repository.server.course_detail.LocationStats
import com.joshtalks.joshskills.repository.server.course_detail.LongDescription
import com.joshtalks.joshskills.repository.server.course_detail.OtherInfo
import com.joshtalks.joshskills.repository.server.course_detail.Reviews
import com.joshtalks.joshskills.repository.server.course_detail.StudentFeedback
import com.joshtalks.joshskills.repository.server.course_detail.SyllabusData
import com.joshtalks.joshskills.repository.server.course_detail.TeacherDetails
import com.joshtalks.joshskills.ui.course_details.extra.TeacherDetailsFragment
import com.joshtalks.joshskills.ui.course_details.viewholder.AboutJoshViewHolder
import com.joshtalks.joshskills.ui.course_details.viewholder.CourseDetailsBaseCell
import com.joshtalks.joshskills.ui.course_details.viewholder.CourseOverviewViewHolder
import com.joshtalks.joshskills.ui.course_details.viewholder.DemoLessonViewHolder
import com.joshtalks.joshskills.ui.course_details.viewholder.GuidelineViewHolder
import com.joshtalks.joshskills.ui.course_details.viewholder.LocationStatViewHolder
import com.joshtalks.joshskills.ui.course_details.viewholder.LongDescriptionViewHolder
import com.joshtalks.joshskills.ui.course_details.viewholder.MasterFaqViewHolder
import com.joshtalks.joshskills.ui.course_details.viewholder.OtherInfoViewHolder
import com.joshtalks.joshskills.ui.course_details.viewholder.ReviewRatingViewHolder
import com.joshtalks.joshskills.ui.course_details.viewholder.StudentFeedbackViewHolder
import com.joshtalks.joshskills.ui.course_details.viewholder.SyllabusViewHolder
import com.joshtalks.joshskills.ui.course_details.viewholder.TeacherDetailsViewHolder
import com.joshtalks.joshskills.ui.extra.ImageShowFragment
import com.joshtalks.joshskills.ui.payment.order_summary.PaymentSummaryActivity
import com.joshtalks.joshskills.ui.subscription.SUBSCRIPTION_TEST_ID
import com.joshtalks.joshskills.ui.subscription.TRIAL_TEST_ID
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import com.joshtalks.joshskills.util.DividerItemDecoration
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.mindorks.placeholderview.SmoothLinearLayoutManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CourseDetailsActivity : BaseActivity() {

    private lateinit var binding: ActivityCourseDetailsBinding
    private val viewModel by lazy { ViewModelProvider(this).get(CourseDetailsViewModel::class.java) }
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var compositeDisposable = CompositeDisposable()
    private var testId: Int = 0
    private var flowFrom: String? = null
    private var downloadID: Long = -1
    private val appAnalytics by lazy { AppAnalytics.create(AnalyticsEvent.COURSE_OVERVIEW.NAME) }


    private var onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadID == id) {
                showToast(getString(R.string.downloaded_syllabus))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)
        window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.black)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_course_details)
        binding.lifecycleOwner = this
        binding.handler = this
        testId = intent.getIntExtra(KEY_TEST_ID, 0)
        if (intent.hasExtra(STARTED_FROM)) {
            flowFrom = intent.getStringExtra(STARTED_FROM)
        }
        if (testId != 0) {
            getCourseDetails(testId)
        } else {
            finish()
        }
        AppAnalytics.create(AnalyticsEvent.LANDING_SCREEN.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, flowFrom)

            .push()
        appAnalytics.addBasicParam()
            .addUserDetails()
            .addParam("test_id", testId)
            .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, flowFrom)
        initView()
        subscribeLiveData()
    }

    private fun initView() {
        linearLayoutManager = SmoothLinearLayoutManager(this)
        linearLayoutManager.isSmoothScrollbarEnabled = true
        binding.placeHolderView.builder.setHasFixedSize(true).setLayoutManager(linearLayoutManager)
        binding.placeHolderView.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (linearLayoutManager.findFirstCompletelyVisibleItemPosition() > 0) {
                    visibleBuyButton()
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 100) {
                    visibleBuyButton()
                }
            }
        })
        binding.placeHolderView.addItemDecoration(
            DividerItemDecoration(
                this,
                R.drawable.list_divider
            )
        )
    }

    fun visibleBuyButton() {
        if (binding.buyCourseLl.visibility == View.GONE) {
            val transition: Transition = Slide(Gravity.BOTTOM)
            transition.duration = 800
            transition.interpolator = LinearInterpolator()
            transition.addTarget(binding.buyCourseLl)
            TransitionManager.beginDelayedTransition(binding.coordinator, transition)
            binding.buyCourseLl.visibility = View.VISIBLE
        }
    }

    private fun subscribeLiveData() {
        viewModel.courseDetailsLiveData.observe(this, Observer { data ->
            binding.txtActualPrice.text = data.paymentData.actualAmount
            binding.txtDiscountedPrice.text = data.paymentData.discountedAmount
            if (data.paymentData.discountText.isNullOrEmpty().not()) {
                binding.txtExtraHint.text = data.paymentData.discountText
                binding.txtExtraHint.visibility = View.VISIBLE
                appAnalytics.addParam(
                    AnalyticsEvent.COURSE_PRICE.NAME,
                    data.paymentData.actualAmount
                )
                    .addParam(
                        AnalyticsEvent.SHOWN_COURSE_PRICE.NAME,
                        data.paymentData.discountedAmount
                    )
            }
            if (data.version.isNotBlank()) {
                appAnalytics.addParam(VERSION, PrefManager.getStringValue(VERSION))

                PrefManager.put(VERSION, data.version)
            }
            binding.txtActualPrice.paintFlags =
                binding.txtActualPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

            data.cards.sortedBy { it.sequenceNumber }.forEach { card ->
                getViewHolder(card)?.run {
                    binding.placeHolderView.addView(this)
                }
            }.also {
                binding.placeHolderView.addView(
                    OtherInfoViewHolder(
                        CardType.OTHER_INFO,
                        -1,
                        null,
                        this
                    )
                )
            }

            updateButtonText(data.paymentData.discountedAmount.substring(1).toDouble())

        })

        viewModel.apiCallStatusLiveData.observe(this, Observer {
            binding.progressBar.visibility = View.GONE
            if (it == ApiCallStatus.FAILED) {
                val imageUrl =
                    AppObjectController.getFirebaseRemoteConfig().getString("ERROR_API_IMAGE_URL")
                val imageView = ImageView(this).apply {
                    adjustViewBounds = true
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    layoutParams = CoordinatorLayout.LayoutParams(
                        CoordinatorLayout.LayoutParams.WRAP_CONTENT,
                        CoordinatorLayout.LayoutParams.MATCH_PARENT
                    ).apply {
                        gravity = Gravity.CENTER
                    }
                }

                binding.coordinator.addView(imageView)
                Glide.with(this)
                    .load(imageUrl)
                    .override(Target.SIZE_ORIGINAL)
                    .optionalTransform(
                        WebpDrawable::class.java,
                        WebpDrawableTransformation(CircleCrop())
                    ).into(imageView)
            }
        })
    }

    private fun getCourseDetails(testId: Int) {
        viewModel.fetchCourseDetails(testId.toString())
    }

    private fun getViewHolder(card: Card): CourseDetailsBaseCell? {
        when (card.cardType) {
            CardType.COURSE_OVERVIEW -> {
                val data = AppObjectController.gsonMapperForLocal.fromJson(
                    card.data.toString(),
                    CourseOverviewData::class.java
                )
                if (data.courseName.isNotBlank())
                    appAnalytics.addParam(AnalyticsEvent.COURSE_NAME.NAME, data.courseName)
                return CourseOverviewViewHolder(
                    card.cardType,
                    card.sequenceNumber,
                    data,
                    this
                )
            }
            CardType.LONG_DESCRIPTION -> {
                val data = AppObjectController.gsonMapperForLocal.fromJson(
                    card.data.toString(),
                    LongDescription::class.java
                )
                return LongDescriptionViewHolder(
                    card.cardType,
                    card.sequenceNumber,
                    data,
                    this
                )
            }
            CardType.TEACHER_DETAILS -> {
                val data = AppObjectController.gsonMapperForLocal.fromJson(
                    card.data.toString(),
                    TeacherDetails::class.java
                )
                return TeacherDetailsViewHolder(
                    card.cardType,
                    card.sequenceNumber,
                    data,
                    this
                )
            }
            CardType.SYLLABUS -> {
                val data = AppObjectController.gsonMapperForLocal.fromJson(
                    card.data.toString(),
                    SyllabusData::class.java
                )
                return SyllabusViewHolder(
                    card.cardType,
                    card.sequenceNumber,
                    data,
                    this
                )
            }
            CardType.GUIDELINES -> {
                val data = AppObjectController.gsonMapperForLocal.fromJson(
                    card.data.toString(),
                    Guidelines::class.java
                )
                return GuidelineViewHolder(
                    card.cardType,
                    card.sequenceNumber,
                    data,
                    supportFragmentManager
                )
            }
            CardType.DEMO_LESSON -> {
                val data = AppObjectController.gsonMapperForLocal.fromJson(
                    card.data.toString(),
                    DemoLesson::class.java
                )
                return DemoLessonViewHolder(
                    card.cardType,
                    card.sequenceNumber,
                    data,
                    this
                )
            }
            CardType.REVIEWS -> {
                val data = AppObjectController.gsonMapperForLocal.fromJson(
                    card.data.toString(),
                    Reviews::class.java
                )
                return ReviewRatingViewHolder(
                    card.cardType,
                    card.sequenceNumber,
                    data
                )
            }
            CardType.LOCATION_STATS -> {
                val data = AppObjectController.gsonMapperForLocal.fromJson(
                    card.data.toString(),
                    LocationStats::class.java
                )
                return LocationStatViewHolder(
                    card.cardType,
                    card.sequenceNumber,
                    data,
                    this,
                    this
                )
            }
            CardType.STUDENT_FEEDBACK -> {
                val data = AppObjectController.gsonMapperForLocal.fromJson(
                    card.data.toString(),
                    StudentFeedback::class.java
                )
                return StudentFeedbackViewHolder(
                    card.cardType,
                    card.sequenceNumber,
                    data,
                    this
                )
            }
            CardType.FAQ -> {
                val data = AppObjectController.gsonMapperForLocal.fromJson(
                    card.data.toString(),
                    FAQData::class.java
                )
                return MasterFaqViewHolder(
                    card.cardType,
                    card.sequenceNumber,
                    data
                )
            }
            CardType.ABOUT_JOSH -> {
                val data = AppObjectController.gsonMapperForLocal.fromJson(
                    card.data.toString(),
                    AboutJosh::class.java
                )
                return AboutJoshViewHolder(
                    card.cardType,
                    card.sequenceNumber,
                    data,
                    this
                )
            }
            CardType.OTHER_INFO -> {
                val data = AppObjectController.gsonMapperForLocal.fromJson(
                    card.data.toString(),
                    OtherInfo::class.java
                )
                return OtherInfoViewHolder(
                    card.cardType,
                    card.sequenceNumber,
                    data,
                    this
                )
            }
        }
    }

    private fun scrollToPosition(pos: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var tempView: CourseDetailsBaseCell
                binding.placeHolderView.allViewResolvers.let {
                    it.forEachIndexed { index, view ->
                        if (view is CourseDetailsBaseCell) {
                            tempView = view
                            if (tempView.sequenceNumber == pos) {
                                AppObjectController.uiHandler.post {
                                    linearLayoutManager.scrollToPositionWithOffset(index, 0)
                                }
                            }
                        }
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun scrollToCard(type: CardType) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var tempView: CourseDetailsBaseCell
                binding.placeHolderView.allViewResolvers.let {
                    it.forEachIndexed { index, view ->
                        if (view is CourseDetailsBaseCell) {
                            tempView = view
                            if (tempView.type == type) {
                                AppObjectController.uiHandler.post {
                                    linearLayoutManager.scrollToPositionWithOffset(index, 0)
                                }
                            }
                        }
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
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
        compositeDisposable.add(RxBus2.listen(GotoCourseCard::class.java).subscribe {
            scrollToPosition(it.pos)
        })

        compositeDisposable.add(RxBus2.listen(TeacherDetails::class.java).subscribe {
            logMeetMeAnalyticEvent(it.name)
            TeacherDetailsFragment.newInstance(it).show(supportFragmentManager, "Teacher Details")
        })
        compositeDisposable.add(
            RxBus2.listen(DownloadSyllabusEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it.syllabusData.syllabusDownloadUrl.isBlank().not()) {
                        logDownloadFileAnalyticEvent()
                        getPermissionAndDownloadSyllabus(it.syllabusData)
                    }
                }, {
                    it.printStackTrace()
                })
        )

        compositeDisposable.add(
            RxBus2.listen(CardType::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    scrollToCard(it)
                }, {
                    it.printStackTrace()
                })
        )
        compositeDisposable.add(
            RxBus2.listen(ImageShowEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        Utils.fileUrl(it.localPath, it.serverPath)?.run {
                            ImageShowFragment.newInstance(this, null, null)
                                .show(supportFragmentManager, "ImageShow")
                        }
                    },
                    {
                        it.printStackTrace()
                    })
        )
        compositeDisposable.add(
            RxBus2.listen(VideoShowEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    AppAnalytics.create(AnalyticsEvent.DEMO_VIDEO_PLAYED.NAME)
                        .addBasicParam()
                        .addUserDetails()
                        .addParam(VERSION, PrefManager.getStringValue(VERSION))
                        .addParam(AnalyticsEvent.TEST_ID_PARAM.NAME, testId).push()
                    VideoPlayerActivity.startVideoActivity(
                        this,
                        it.videoTitle,
                        it.videoId,
                        it.videoUrl
                    )
                }, {
                    it.printStackTrace()
                })
        )
    }

    fun buyCourse() {
        val exploreTypeStr = PrefManager.getStringValue(EXPLORE_TYPE, true)
        val discountedPrice =
            viewModel.courseDetailsLiveData.value!!.paymentData.discountedAmount.substring(1)
                .toDouble()
        if (exploreTypeStr.isNotBlank()
            && exploreTypeStr != ExploreCardType.NORMAL.name
            && exploreTypeStr != ExploreCardType.FFCOURSE.name
        ) {
            val isTrialStarted = PrefManager.getBoolValue(IS_TRIAL_STARTED, true)
            val tempTestId = if (isTrialStarted && discountedPrice > 0.0) SUBSCRIPTION_TEST_ID
            else if (isTrialStarted.not()) TRIAL_TEST_ID
            else testId
            logStartCourseAnalyticEvent(tempTestId)
            PaymentSummaryActivity.startPaymentSummaryActivity(
                this,
                tempTestId.toString()
            )
        } else {
            logStartCourseAnalyticEvent(testId)
            PaymentSummaryActivity.startPaymentSummaryActivity(this, testId.toString())
        }
        appAnalytics.addParam(AnalyticsEvent.START_COURSE_NOW.NAME, "Clicked")
    }

    private fun logStartCourseAnalyticEvent(testId: Int) {
        AppAnalytics.create(AnalyticsEvent.START_COURSE_NOW.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(VERSION, PrefManager.getStringValue(VERSION))
            .addParam(AnalyticsEvent.TEST_ID_PARAM.NAME, testId).push()
    }

    private fun logMeetMeAnalyticEvent(name: String) {
        AppAnalytics.create(AnalyticsEvent.MEET_ME_CLICKED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(VERSION, PrefManager.getStringValue(VERSION))
            .addParam("Name", name)
            .addParam(AnalyticsEvent.TEST_ID_PARAM.NAME, testId).push()
    }

    private fun logDownloadFileAnalyticEvent() {
        AppAnalytics.create(AnalyticsEvent.DOWNLOAD_FILE_CLICKED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(VERSION, PrefManager.getStringValue(VERSION))
            .push()
    }

    private fun getPermissionAndDownloadSyllabus(syllabusData: SyllabusData) {
        PermissionUtils.storageReadAndWritePermission(this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            downloadDigitalCopy(syllabusData)
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.permissionPermanentlyDeniedDialog(this@CourseDetailsActivity)
                            return
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            })
    }

    private fun downloadDigitalCopy(syllabusData: SyllabusData) {
        registerDownloadReceiver()
        var fileName = Utils.getFileNameFromURL(syllabusData.syllabusDownloadUrl)
        if (fileName.isEmpty()) {
            syllabusData.title.run {
                fileName = this + "_syllabus.pdf"
            }
        }
        val request: DownloadManager.Request =
            DownloadManager.Request(Uri.parse(syllabusData.syllabusDownloadUrl))
                .setTitle(getString(R.string.app_name))
                .setDescription("Downloading syllabus")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            request.setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
        }

        val downloadManager =
            AppObjectController.joshApplication.getSystemService(Context.DOWNLOAD_SERVICE) as (DownloadManager)
        downloadID = downloadManager.enqueue(request)
        showToast(getString(R.string.downloading_start))
    }

    private fun registerDownloadReceiver() {
        AppObjectController.joshApplication.registerReceiver(
            onDownloadComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
    }

    override fun onDestroy() {
        try {
            this.unregisterReceiver(onDownloadComplete)
        } catch (ex: Exception) {
        }
        super.onDestroy()
    }

    fun goToTop() {
        val params: CoordinatorLayout.LayoutParams =
            binding.appBarLayout.layoutParams as CoordinatorLayout.LayoutParams
        val behavior = params.behavior as AppBarLayout.Behavior
        behavior.onNestedFling(
            binding.coordinator,
            binding.appBarLayout,
            binding.coordinator,
            0f,
            10000f,
            true
        )
    }


    companion object {
        const val KEY_TEST_ID = "test-id"

        fun startCourseDetailsActivity(
            activity: Activity,
            testId: Int,
            startedFrom: String = EMPTY, flags: Array<Int> = arrayOf()
        ) {
            Intent(activity, CourseDetailsActivity::class.java).apply {
                putExtra(KEY_TEST_ID, testId)
                if (startedFrom.isNotBlank())
                    putExtra(STARTED_FROM, startedFrom)
                flags.forEach { flag ->
                    this.addFlags(flag)
                }
            }.run {
                activity.startActivity(this)
            }
        }

        fun getIntent(
            context: Context,
            testId: Int,
            startedFrom: String = EMPTY, flags: Array<Int> = arrayOf()
        ) = Intent(context, CourseDetailsActivity::class.java).apply {
            putExtra(KEY_TEST_ID, testId)
            if (startedFrom.isNotBlank())
                putExtra(STARTED_FROM, startedFrom)
            flags.forEach { flag ->
                this.addFlags(flag)
            }
        }
    }

    private fun updateButtonText(discountedPrice: Double) {

        if (discountedPrice == 0.0) {
            binding.btnStartCourse.text = getString(R.string.start_free_course)
            binding.btnStartCourse.textSize = 16f
        }

        val exploreTypeStr = PrefManager.getStringValue(EXPLORE_TYPE, true)
        if (exploreTypeStr.isNotBlank()) {
            when (ExploreCardType.valueOf(exploreTypeStr)) {
                ExploreCardType.FREETRIAL,
                ExploreCardType.SUBSCRIPTION -> {
                    if (discountedPrice > 0) {
                        binding.btnStartCourse.text = getString(R.string.get_one_year_pass)
                        binding.btnStartCourse.textSize = 16f
                    }
                }
            }
        }
    }
}
