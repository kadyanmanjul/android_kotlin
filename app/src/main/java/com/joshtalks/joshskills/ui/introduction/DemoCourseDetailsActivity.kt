package com.joshtalks.joshskills.ui.introduction

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.EXPLORE_TYPE
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.STARTED_FROM
import com.joshtalks.joshskills.databinding.ActivityDemoCourseDetailsBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.OpenDemoLessonEventBus
import com.joshtalks.joshskills.repository.local.model.ExploreCardType
import com.joshtalks.joshskills.repository.server.course_detail.AboutJosh
import com.joshtalks.joshskills.repository.server.course_detail.Card
import com.joshtalks.joshskills.repository.server.course_detail.CardType
import com.joshtalks.joshskills.repository.server.course_detail.demoCourseDetails.CourseAToZResponse
import com.joshtalks.joshskills.repository.server.course_detail.demoCourseDetails.DemoLesson2Response
import com.joshtalks.joshskills.repository.server.course_detail.demoCourseDetails.FactsResponse
import com.joshtalks.joshskills.repository.server.course_detail.demoCourseDetails.SuperStarResponse
import com.joshtalks.joshskills.ui.course_details.CourseDetailsViewModel
import com.joshtalks.joshskills.ui.course_details.viewholder.AboutJoshViewHolder
import com.joshtalks.joshskills.ui.course_details.viewholder.CourseDetailsBaseCell
import com.joshtalks.joshskills.ui.demo_course_details.view_holders.CourseA2ZViewHolder
import com.joshtalks.joshskills.ui.demo_course_details.view_holders.DemoLesson2ViewHolder
import com.joshtalks.joshskills.ui.demo_course_details.view_holders.DemoTitleCardsViewHolder
import com.joshtalks.joshskills.ui.demo_course_details.view_holders.SuperStarViewHolder
import com.joshtalks.joshskills.ui.leaderboard.EmptyItemViewHolder
import com.joshtalks.joshskills.ui.lesson.LessonActivity
import com.joshtalks.joshskills.ui.lesson.LessonViewModel
import com.joshtalks.joshskills.ui.payment.order_summary.PaymentSummaryActivity
import com.mindorks.placeholderview.SmoothLinearLayoutManager
import io.reactivex.disposables.CompositeDisposable

class DemoCourseDetailsActivity : BaseActivity() {

    private lateinit var binding: ActivityDemoCourseDetailsBinding
    private val viewModel by lazy { ViewModelProvider(this).get(CourseDetailsViewModel::class.java) }
    private val lessonViewModel by lazy { ViewModelProvider(this).get(LessonViewModel::class.java) }
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var compositeDisposable = CompositeDisposable()
    private var lessonId: Int = 0
    private var isLessonCompleted: Boolean = false

    val dayWiseActivityListener: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && lessonId != 0) {
                lessonViewModel.getLesson(lessonId)
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
        window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.white)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_demo_course_details)
        binding.lifecycleOwner = this
        binding.handler = this
        subscribeLiveData()
        getCourseDetails()
        initView()
    }

    private fun initView() {
        linearLayoutManager = SmoothLinearLayoutManager(this)
        linearLayoutManager.isSmoothScrollbarEnabled = true
        binding.placeHolderView.builder.setHasFixedSize(true)
            .setLayoutManager(linearLayoutManager)
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
        /*binding.placeHolderView.addItemDecoration(
            DividerItemDecoration(
                this,
                R.drawable.list_divider
            )
        )*/
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
        viewModel.demoCourseDetailsLiveData.observe(this, { data ->
            //PrefManager.put(INTRODUCTION_START_NOW_CLICKED,true)
            data.cards.sortedBy { it.sequenceNumber }.forEach { card ->
                getViewHolder(card)?.run {
                    binding.placeHolderView.addView(this)
                }
            }.also {
                binding.placeHolderView.addView(
                    EmptyItemViewHolder()
                )
                binding.placeHolderView.addView(
                    EmptyItemViewHolder()
                )
                binding.placeHolderView.addView(
                    EmptyItemViewHolder()
                )
            }
            updateButtonText(data.paymentData.discountedAmount.substring(1).toDouble())

        })

        viewModel.apiCallStatusLiveData.observe(this, {
            binding.progressBar.visibility = View.GONE
            if (it == ApiCallStatus.FAILED) {
                val imageUrl =
                    AppObjectController.getFirebaseRemoteConfig()
                        .getString("ERROR_API_IMAGE_URL")
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

        lessonViewModel.lessonLiveData.observe(this, Observer {
            if (it?.status == LESSON_STATUS.CO) {
                isLessonCompleted = true
            } else {
                isLessonCompleted = false
            }
            binding.placeHolderView.allViewResolvers.let {
                it.forEachIndexed { index, view ->
                    if (view is DemoLesson2ViewHolder) {
                        view.changeTextToCompleted(isLessonCompleted)
                        AppObjectController.uiHandler.postDelayed({
                            binding.placeHolderView.refreshView(index)
                        }, 250)
                    }
                }
            }
        })
    }

    private fun getCourseDetails() {
        viewModel.fetchDemoCourseDetails()
    }

    private fun getViewHolder(card: Card): CourseDetailsBaseCell? {
        when (card.cardType) {
            CardType.FACTS -> {
                val data = AppObjectController.gsonMapperForLocal.fromJson(
                    card.data.toString(),
                    FactsResponse::class.java
                )

                return DemoTitleCardsViewHolder(
                    card.cardType,
                    card.sequenceNumber,
                    data,
                    this
                )
            }
            CardType.SUPER_STAR -> {
                val data = AppObjectController.gsonMapperForLocal.fromJson(
                    card.data.toString(),
                    SuperStarResponse::class.java
                )
                return SuperStarViewHolder(
                    card.cardType,
                    card.sequenceNumber,
                    data,
                    this,
                    supportFragmentManager
                )
            }
            CardType.COURSE_A_TO_Z -> {
                val data = AppObjectController.gsonMapperForLocal.fromJson(
                    card.data.toString(),
                    CourseAToZResponse::class.java
                )
                return CourseA2ZViewHolder(
                    card.cardType,
                    card.sequenceNumber,
                    data,
                    this
                )
            }
            CardType.DEMO_LESSON_2 -> {
                val data = AppObjectController.gsonMapperForLocal.fromJson(
                    card.data.toString(),
                    DemoLesson2Response::class.java
                )
                lessonId = data.lessonId ?: 0
                lessonViewModel.getLesson(lessonId)
                return DemoLesson2ViewHolder(
                    card.cardType,
                    card.sequenceNumber,
                    data,
                    this,
                    isLessonCompleted
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
            else -> {
                return null
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
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(OpenDemoLessonEventBus::class.java).subscribe {
                it.lessonId?.let { lessonId ->
                    dayWiseActivityListener.launch(
                        LessonActivity.getActivityIntent(
                            this,
                            it.lessonId
                        )
                    )
                }
            })
    }

    fun buyCourse() {
        viewModel.demoCourseDetailsLiveData.value?.paymentData?.let {
            PaymentSummaryActivity.startPaymentSummaryActivity(this, it.testId.toString())

        }
    }

    fun openWhatsapp() {
        viewModel.demoCourseDetailsLiveData.value?.paymentData?.whatsappUrl?.let {
            val whatsappIntent = Intent(Intent.ACTION_VIEW)
            whatsappIntent.data = Uri.parse(it)
            whatsappIntent.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(whatsappIntent)
        }
    }

    private fun updateButtonText(discountedPrice: Double) {
        if (discountedPrice == 0.0) {
            binding.btnStartCourse.text = getString(R.string.start_free_course)
            binding.btnStartCourse.textSize = 16f
        }

        val exploreTypeStr = PrefManager.getStringValue(EXPLORE_TYPE, false)
        if (exploreTypeStr.isNotBlank()) {
            when (ExploreCardType.valueOf(exploreTypeStr)) {
                ExploreCardType.FREETRIAL -> {
                    if (discountedPrice > 0) {
                        binding.btnStartCourse.text = getString(R.string.get_one_year_pass)
                        binding.btnStartCourse.textSize = 16f
                    }
                }
            }
        }
    }


    companion object {
        const val KEY_TEST_ID = "test-id"

        fun startDemoCourseDetailsActivity(
            activity: Activity,
            testId: Int,
            startedFrom: String = EMPTY,
            flags: Array<Int> = arrayOf(),
        ) {
            Intent(activity, DemoCourseDetailsActivity::class.java).apply {
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
        ) = Intent(context, DemoCourseDetailsActivity::class.java).apply {
            putExtra(KEY_TEST_ID, testId)
            if (startedFrom.isNotBlank())
                putExtra(STARTED_FROM, startedFrom)
            flags.forEach { flag ->
                this.addFlags(flag)
            }
        }
    }

}
