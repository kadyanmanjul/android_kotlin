package com.joshtalks.joshskills.ui.payment

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
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.databinding.FragmentCourseDetailType1FragmentBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.BuyCourseEventBus
import com.joshtalks.joshskills.repository.server.course_detail.CourseDetailsResponse
import com.joshtalks.joshskills.ui.payment.viewholder.CourseDetailDataViewHeader
import com.joshtalks.joshskills.ui.payment.viewholder.CourseMentorViewHolder
import com.joshtalks.joshskills.ui.payment.viewholder.CourseStructureViewHolder
import com.joshtalks.joshskills.ui.tooltip.BalloonFactory
import com.joshtalks.joshskills.ui.video_player.FullScreenVideoFragment
import com.joshtalks.joshskills.ui.view_holders.DefaultTextViewHolder
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
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*


const val TEST_ID = "test_ID"
const val COURSE_AMOUNT = "course_amount"


class CourseDetailType1Fragment : Fragment() {

    private var testId = 0
    private var courseId = 1
    private var amount: Double = 0.0
    private var isUserValidForOffer = false
    private var dayRemain = "7"

    private lateinit var binding: FragmentCourseDetailType1FragmentBinding
    private var videoUrl: String? = null
    private var compositeDisposable = CompositeDisposable()
    private var profileBalloon: Balloon? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            testId = it.getInt(TEST_ID)
            courseId = it.getInt(COURSE_ID)
            amount = it.getDouble(COURSE_AMOUNT, 0.0)
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


            val typeToken = object : TypeToken<List<CourseDetailsResponse>>() {}.type
            val list = AppObjectController.gsonMapperForLocal.fromJson<List<CourseDetailsResponse>>(
                AppObjectController.getFirebaseRemoteConfig().getString("course_details"),
                typeToken
            )
            val obj: CourseDetailsResponse? = list.find { it.courseId == courseId }
            obj?.let { courseDetailsResponse ->
                binding.courseTitle.text = courseDetailsResponse.courseTitle
                binding.courseDesc.text = courseDetailsResponse.courseDescription

                val df = DecimalFormat("###,###", DecimalFormatSymbols(Locale.US))
                val enrollUser = df.format(courseDetailsResponse.courseEnrolledUser)
                binding.tvEnrollUsers.text = enrollUser + " enrolled"

                binding.tvRating.text = courseDetailsResponse.courseRating.toString()
                binding.tvDuration.text = courseDetailsResponse.courseDuration.toString() + " Days"
                videoUrl = courseDetailsResponse.videoUrl


                Glide.with(requireActivity())
                    .load(courseDetailsResponse.bgImage)
                    .override(SIZE_ORIGINAL)
                    .into(binding.bgTopIv)


                val multi = MultiTransformation<Bitmap>(
                    RoundedCornersTransformation(
                        com.joshtalks.joshskills.core.Utils.dpToPx(ROUND_CORNER),
                        0,
                        RoundedCornersTransformation.CornerType.ALL
                    )
                )
                Glide.with(requireActivity())
                    .load(courseDetailsResponse.videoThumbnail)
                    .apply(RequestOptions.bitmapTransform(multi))
                    .override(SIZE_ORIGINAL)
                    .into(binding.imageView)



                binding.tvUpActualPrice.text =
                    "₹" + String.format("%.2f", courseDetailsResponse.coursePrice)
                binding.tvDownActualPrice.text =
                    "₹" + String.format("%.2f", courseDetailsResponse.coursePrice)


                if (amount > 0) {
                    binding.tvUpOfferPrice.text =
                        "₹" + String.format("%.2f", amount)
                    binding.tvDownOfferPrice.text =
                        "₹" + String.format("%.2f", amount)
                } else {
                    binding.tvUpOfferPrice.text =
                        "₹" + String.format("%.2f", courseDetailsResponse.courseDiscountPrice)
                    binding.tvDownOfferPrice.text =
                        "₹" + String.format("%.2f", courseDetailsResponse.courseDiscountPrice)
                }

                binding.tvUpActualPrice.paintFlags =
                    binding.tvUpActualPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvDownActualPrice.paintFlags =
                    binding.tvDownActualPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

                val courseInformationList =
                    courseDetailsResponse.courseInformation.sortedWith(compareBy { it.id })
                courseInformationList.forEach {
                    binding.courseDetailRv.addView(CourseDetailDataViewHeader(it))
                }

                binding.courseDetailRv.addView(DefaultTextViewHolder("Mujhe Kaun Padhayega"))
                val sortedList = courseDetailsResponse.courseMentor.sortedWith(compareBy { it.id })
                sortedList.forEach {
                    binding.courseDetailRv.addView(CourseMentorViewHolder(it))
                }

                binding.courseDetailRv.addView(DefaultTextViewHolder("Course Curriculum"))

                val listCourseStructure =
                    courseDetailsResponse.courseStructure.sortedWith(compareBy { it.id })
                listCourseStructure.forEach {
                    binding.courseDetailRv.addView(CourseStructureViewHolder(it))
                }

                binding.courseDetailRv.addView(SingleImageViewHolder(courseDetailsResponse.feedbackImageUrl))

                val titleView =
                    requireActivity().findViewById<AppCompatTextView>(R.id.text_message_title)
                titleView.text = courseDetailsResponse.courseName
            }


        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        AppObjectController.uiHandler.postDelayed({
            if (binding.courseDesc.lineCount > 3) {
                val lp = binding.upperContainer.layoutParams as ConstraintLayout.LayoutParams
                lp.matchConstraintPercentHeight = 0.75f
                binding.upperContainer.layoutParams = lp

            }
            //var set =  ConstraintSet()

        }, 500)


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
        activity?.finish()
    }

    fun buyCourse() {
        RxBus2.publish(BuyCourseEventBus(testId.toString(), isUserValidForOffer))
    }

    companion object {

        @JvmStatic
        fun newInstance(testId: Int, courseId: Int, courseAmount: Double) =
            CourseDetailType1Fragment().apply {
                arguments = Bundle().apply {
                    putInt(TEST_ID, testId)
                    putInt(COURSE_ID, courseId)
                    putDouble(COURSE_AMOUNT, courseAmount)
                }
            }
    }
}
