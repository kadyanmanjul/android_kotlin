package com.joshtalks.joshskills.ui.payment

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
import com.crashlytics.android.Crashlytics
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_UNIQUE_ID
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.databinding.FragmentCourseDetailType1FragmentBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.eventbus.BuyCourseEventBus
import com.joshtalks.joshskills.repository.local.eventbus.ImageShowEvent
import com.joshtalks.joshskills.repository.local.eventbus.VideoShowEvent
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.CourseExploreModel
import com.joshtalks.joshskills.repository.server.course_detail.CourseDetailsResponse
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.payment.viewholder.CourseDetailDataViewHeader
import com.joshtalks.joshskills.ui.tooltip.BalloonFactory
import com.joshtalks.joshskills.ui.video_player.FullScreenVideoFragment
import com.joshtalks.joshskills.ui.view_holders.ROUND_CORNER
import com.joshtalks.joshskills.ui.view_holders.SingleImageViewHolder
import com.joshtalks.skydoves.balloon.Balloon
import com.joshtalks.skydoves.balloon.OnBalloonClickListener
import com.vanniktech.emoji.Utils
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*


const val TEST_ID = "test_ID"


class CourseDetailType1Fragment : Fragment() {

    private var testId = 0
    private var courseId = 1
    private var isUserValidForOffer = false
    private var dayRemain = "7"

    private lateinit var binding: FragmentCourseDetailType1FragmentBinding
    private var videoUrl: String? = null
    private var compositeDisposable = CompositeDisposable()
    private var profileBalloon: Balloon? = null
    private var courseModel: CourseExploreModel = CourseExploreModel()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            testId = it.getInt(TEST_ID)
            courseId = it.getInt(COURSE_ID)
        }

        compositeDisposable.add(AppObjectController.appDatabase
            .courseDao()
            .isUserOldThen7Days()
            .concatMap {
                val (flag, dayRemain) = com.joshtalks.joshskills.core.Utils.isUser7DaysOld(it.courseCreatedDate)
                this.dayRemain = dayRemain.toString()
                return@concatMap Maybe.just(flag)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { value ->
                    isUserValidForOffer = value
                    profileBalloon = BalloonFactory.getCourseOfferBalloon(requireActivity(),
                        this.dayRemain,
                        this,
                        object :
                            OnBalloonClickListener {
                            override fun onBalloonClick(view: View) {
                            }
                        })
                },
                { error ->
                    error.printStackTrace()
                }
            ))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_course_detail_type1_fragment,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            binding.progressBar.visibility = View.VISIBLE
            val linearLayoutManager = LinearLayoutManager(context)
            linearLayoutManager.isSmoothScrollbarEnabled = true
            binding.courseDetailRv.builder
                .setHasFixedSize(true)
                .setLayoutManager(linearLayoutManager)
            binding.courseDetailRv.addItemDecoration(
                LayoutMarginDecoration(
                    Utils.dpToPx(
                        requireContext(),
                        2f
                    )
                )
            )
            binding.courseDetailRv.itemAnimator = null

            binding.nestedScrollView.viewTreeObserver.addOnScrollChangedListener {
                if (binding.nestedScrollView.scrollY == 0) {
                    profileBalloon?.dismiss()
                    binding.belowBuyCv.visibility = View.GONE
                    return@addOnScrollChangedListener
                }
                val rect = Rect()
                if (binding.btnBuyCourse.getGlobalVisibleRect(rect) && binding.btnBuyCourse.height == rect.height() && binding.btnBuyCourse.width == rect.width()
                ) {
                    if (profileBalloon != null && profileBalloon!!.isShowing) {
                        profileBalloon?.dismiss()
                    }
                    binding.belowBuyCv.visibility = View.GONE
                } else {
                    binding.belowBuyCv.visibility = View.VISIBLE
                    showHint()
                }
            }
            getTestCourseDetails()

        } catch (ex: Exception) {
            ex.printStackTrace()
        }

    }

    private fun getTestCourseDetails() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = HashMap<String, String>()
                data["test"] = testId.toString()
                data["gaid"] = PrefManager.getStringValue(USER_UNIQUE_ID)

                if (Mentor.getInstance().getId().isNotEmpty()) {
                    data["mentor"] = Mentor.getInstance().getId()
                }

                val response: List<CourseDetailsResponse> =
                    AppObjectController.signUpNetworkService.explorerCourseDetailsApiV2Async(data)
                        .await()

                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val course = response[0].course

                        binding.courseTitle.text = course.name
                        binding.courseDesc.text = course.description
                        courseModel.courseName = course.name
                        courseModel.course = testId


                        val df = DecimalFormat("###,###", DecimalFormatSymbols(Locale.US))
                        val enrollUser = df.format(course.totalEnrolled)
                        binding.tvEnrollUsers.text = enrollUser + " enrolled"
                        binding.tvRating.text = course.rating.toString()
                        binding.tvDuration.text = course.duration.toString() + " Days"

                        val test = response[0].test

                        videoUrl = test.videoLink
                        val multi = MultiTransformation<Bitmap>(
                            RoundedCornersTransformation(
                                com.joshtalks.joshskills.core.Utils.dpToPx(ROUND_CORNER),
                                0,
                                RoundedCornersTransformation.CornerType.ALL
                            )
                        )

                        Glide.with(requireActivity())
                            .load(test.thumbnail)
                            .apply(RequestOptions.bitmapTransform(multi))
                            .override(SIZE_ORIGINAL)
                            .into(binding.imageView)

                        binding.tvUpActualPrice.text =
                            "₹" + String.format("%.2f", test.showAmount)
                        binding.tvDownActualPrice.text =
                            "₹" + String.format("%.2f", test.showAmount)

                        binding.tvUpOfferPrice.text =
                            "₹" + String.format("%.2f", test.amount)
                        binding.tvDownOfferPrice.text =
                            "₹" + String.format("%.2f", test.amount)
                        courseModel.amount = test.amount


                        binding.tvUpActualPrice.paintFlags =
                            binding.tvUpActualPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                        binding.tvDownActualPrice.paintFlags =
                            binding.tvDownActualPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG


                        val courseInformationList = response.sortedWith(compareBy { it.sequenceNo })
                        courseInformationList.forEach {
                            if (it.type == BASE_MESSAGE_TYPE.IM) {
                                binding.courseDetailRv.addView(
                                    SingleImageViewHolder(
                                        it.url,
                                        it.title
                                    )
                                )
                            } else {
                                binding.courseDetailRv.addView(CourseDetailDataViewHeader(it))
                            }
                        }
                        binding.nestedScrollView.visibility = View.VISIBLE
                        binding.progressBar.visibility = View.GONE

                        val titleView =
                            requireActivity().findViewById<AppCompatTextView>(R.id.text_message_title)
                        //titleView.text = courseDetailsResponse.courseName
                        WorkMangerAdmin.newCourseScreenEventWorker(course.name, testId.toString())
                    } catch (ex: Exception) {
                        Crashlytics.logException(ex)
                    }
                }

                AppObjectController.uiHandler.postDelayed({
                    if (binding.courseDesc.lineCount > 3) {
                        val lp =
                            binding.upperContainer.layoutParams as ConstraintLayout.LayoutParams
                        lp.matchConstraintPercentHeight = 0.75f
                        binding.upperContainer.layoutParams = lp

                    }
                    //var set =  ConstraintSet()
                }, 500)

            } catch (ex: HttpException) {
                if (ex.code() == 500) {
                    invalidCourseId()
                }
                ex.printStackTrace()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

        }

    }

    private fun invalidCourseId() {
        startActivity(Intent(requireContext(), CourseExploreActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        })
        requireActivity().finishAndRemoveTask()
    }


    private fun showHint() {
        if (isUserValidForOffer && profileBalloon != null && profileBalloon!!.isShowing.not()) {
            profileBalloon?.showAlignTopWithException(binding.belowBuyCv)
        }
    }


    fun playVideo() {
        videoUrl?.let {
            FullScreenVideoFragment.newInstance(it)
                .show(requireActivity().supportFragmentManager, "Video Play")
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    fun backPress() {
        activity?.onBackPressed()
        // activity?.finish()
    }

    fun buyCourse() {
        RxBus2.publish(BuyCourseEventBus(testId.toString(), isUserValidForOffer, courseModel))
    }

    override fun onResume() {
        super.onResume()
        addObserver()
    }


    private fun addObserver() {
        compositeDisposable.add(
            RxBus2.listen(ImageShowEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({

                }, {
                    it.printStackTrace()
                })
        )

        compositeDisposable.add(
            RxBus2.listen(VideoShowEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({

                }, {
                    it.printStackTrace()
                })
        )
    }
    override fun onDestroyView() {
        super.onDestroyView()
        AppAnalytics.create(AnalyticsEvent.BACK_PRESSED.NAME)
            .addParam("name", javaClass.simpleName)
            .push()
    }

    companion object {

        @JvmStatic
        fun newInstance(
            testId: Int,
            courseId: Int
        ) =
            CourseDetailType1Fragment().apply {
                arguments = Bundle().apply {
                    putInt(TEST_ID, testId)
                    putInt(COURSE_ID, courseId)
                }
            }
    }
}
