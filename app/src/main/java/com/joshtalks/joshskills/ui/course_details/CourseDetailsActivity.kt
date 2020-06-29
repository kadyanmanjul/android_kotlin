package com.joshtalks.joshskills.ui.course_details

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.databinding.ActivityCourseDetailsBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.GotoCourseCard
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
import com.joshtalks.joshskills.ui.extra.setOnSingleClickListener
import com.joshtalks.joshskills.ui.payment.order_summary.PaymentSummaryActivity
import com.joshtalks.joshskills.ui.payment.viewholder.AboutJoshViewHolder
import com.joshtalks.joshskills.ui.payment.viewholder.LocationStatViewHolder
import com.joshtalks.joshskills.ui.payment.viewholder.LongDescriptionViewHolder
import com.joshtalks.joshskills.ui.payment.viewholder.StudentFeedbackViewHolder
import com.joshtalks.joshskills.ui.payment.viewholder.SyllabusViewHolder
import com.joshtalks.joshskills.ui.view_holders.CourseDetailsBaseCell
import com.joshtalks.joshskills.ui.view_holders.CourseOverviewViewHolder
import com.joshtalks.joshskills.ui.view_holders.DemoLessonViewHolder
import com.joshtalks.joshskills.ui.view_holders.GuidelineViewHolder
import com.joshtalks.joshskills.ui.view_holders.MasterFaqViewHolder
import com.joshtalks.joshskills.ui.view_holders.OtherInfoViewHolder
import com.joshtalks.joshskills.ui.view_holders.ReviewRatingViewHolder
import com.joshtalks.joshskills.ui.view_holders.TeacherDetailsViewHolder
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CourseDetailsActivity : CoreJoshActivity() {

    private lateinit var binding: ActivityCourseDetailsBinding
    private val viewModel by lazy { ViewModelProvider(this).get(CourseDetailsViewModel::class.java) }
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var compositeDisposable = CompositeDisposable()
    private var testId: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.black)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_course_details)
        binding.lifecycleOwner = this
        binding.handler = this
        initView()
        testId = intent.getIntExtra(KEY_TEST_ID, 0)
        if (testId != 0) {
            getCourseDetails(testId)
        } else {
            finish()
        }
        subscribeLiveData()
    }

    private fun initView() {
        linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.isSmoothScrollbarEnabled = true
        binding.placeHolderView.builder.setHasFixedSize(true).setLayoutManager(linearLayoutManager)
    }

    private fun subscribeLiveData() {
        viewModel.courseDetailsLiveData.observe(this, Observer { data ->
            if (data.paymentdata.discountText.isNotBlank()) {
                binding.txtActualPrice.text = String.format("%.2f", data.paymentdata.actualAmount)
                binding.txtDiscountedPrice.text =
                    String.format("%.2f", data.paymentdata.discountedAmount)
                binding.txtExtraHint.text = data.paymentdata.discountText
                binding.txtExtraHint.visibility = View.VISIBLE
            }
            data.cards.sortedBy { it.sequenceNumber }.forEach { card ->
                getViewHolder(card)?.run {
                    binding.placeHolderView.addView(this)
                }
            }.also {
                binding.placeHolderView.addView(OtherInfoViewHolder(-1, null, this))
            }
        })

        viewModel.apiCallStatusLiveData.observe(this, Observer {
            binding.progressBar.visibility = View.GONE
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
                return CourseOverviewViewHolder(card.sequenceNumber, data, this)
            }
            CardType.LONG_DESCRIPTION -> {
                val data = AppObjectController.gsonMapperForLocal.fromJson(
                    card.data.toString(),
                    LongDescription::class.java
                )
                return LongDescriptionViewHolder(card.sequenceNumber, data, this)
            }
            CardType.TEACHER_DETAILS -> {
                val data = AppObjectController.gsonMapperForLocal.fromJson(
                    card.data.toString(),
                    TeacherDetails::class.java
                )
                return TeacherDetailsViewHolder(card.sequenceNumber, data, this)
            }
            CardType.SYLLABUS -> {
                val data = AppObjectController.gsonMapperForLocal.fromJson(
                    card.data.toString(),
                    SyllabusData::class.java
                )
                return SyllabusViewHolder(card.sequenceNumber, data, this)
            }
            CardType.GUIDELINES -> {
                val data = AppObjectController.gsonMapperForLocal.fromJson(
                    card.data.toString(),
                    Guidelines::class.java
                )
                return GuidelineViewHolder(card.sequenceNumber, data, supportFragmentManager)
            }
            CardType.DEMO_LESSON -> {
                val data = AppObjectController.gsonMapperForLocal.fromJson(
                    card.data.toString(),
                    DemoLesson::class.java
                )
                return DemoLessonViewHolder(card.sequenceNumber, data, this)
            }
            CardType.REVIEWS -> {
                val data = AppObjectController.gsonMapperForLocal.fromJson(
                    card.data.toString(),
                    Reviews::class.java
                )
                return ReviewRatingViewHolder(card.sequenceNumber, data)
            }
            CardType.LOCATION_STATS -> {
                val data = AppObjectController.gsonMapperForLocal.fromJson(
                    card.data.toString(),
                    LocationStats::class.java
                )
                return LocationStatViewHolder(card.sequenceNumber, data, this, this)
            }
            CardType.STUDENT_FEEDBACK -> {
                val data = AppObjectController.gsonMapperForLocal.fromJson(
                    card.data.toString(),
                    StudentFeedback::class.java
                )
                return StudentFeedbackViewHolder(card.sequenceNumber, data, this)
            }
            CardType.FAQ -> {
                val data = AppObjectController.gsonMapperForLocal.fromJson(
                    card.data.toString(),
                    FAQData::class.java
                )
                return MasterFaqViewHolder(card.sequenceNumber, data)
            }
            CardType.ABOUT_JOSH -> {
                val data = AppObjectController.gsonMapperForLocal.fromJson(
                    card.data.toString(),
                    AboutJosh::class.java
                )
                return AboutJoshViewHolder(card.sequenceNumber, data, this)
            }
            CardType.OTHER_INFO -> {
                val data = AppObjectController.gsonMapperForLocal.fromJson(
                    card.data.toString(),
                    OtherInfo::class.java
                )
                return OtherInfoViewHolder(card.sequenceNumber, data, this)
            }
        }
        return OtherInfoViewHolder(card.sequenceNumber, null, this)
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
            TeacherDetailsFragment.newInstance(it).show(supportFragmentManager, "Teacher Details")
        })

        binding.btnStartCourse.setOnSingleClickListener(View.OnClickListener {
            PaymentSummaryActivity.startPaymentSummaryActivity(this, testId.toString())
        })


    }

    companion object {
        const val KEY_TEST_ID = "test-id"

        fun startCourseDetailsActivity(activity: Activity, testId: Int) {
            Intent(activity, CourseDetailsActivity::class.java).apply {
                putExtra(KEY_TEST_ID, testId)
            }.run {
                activity.startActivity(this)
            }
        }
    }
}
