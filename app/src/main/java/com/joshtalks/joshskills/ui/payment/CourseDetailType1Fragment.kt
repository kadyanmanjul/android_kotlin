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
import com.joshtalks.joshskills.ui.video_player.FullScreenVideoFragment
import com.joshtalks.joshskills.ui.view_holders.DefaultTextViewHolder
import com.joshtalks.joshskills.ui.view_holders.ROUND_CORNER
import com.joshtalks.joshskills.ui.view_holders.SingleImageViewHolder
import com.vanniktech.emoji.Utils
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*


const val TEST_ID = "test_ID"

class CourseDetailType1Fragment : Fragment() {

    private var testId = 0
    private var courseId = 1

    private lateinit var binding: FragmentCourseDetailType1FragmentBinding
    private var videoUrl: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            testId = it.getInt(TEST_ID)
            courseId = it.getInt(COURSE_ID)
        }
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
                        context!!,
                        2f
                    )
                )
            )
            binding.courseDetailRv.itemAnimator = null

            binding.nestedScrollView.viewTreeObserver.addOnScrollChangedListener {
                if (binding.nestedScrollView.scrollY == 0) {
                    binding.belowBuyCv.visibility = View.GONE
                    return@addOnScrollChangedListener
                }
                val rect = Rect()
                if (binding.btnBuyCourse.getGlobalVisibleRect(rect) && binding.btnBuyCourse.height == rect.height() && binding.btnBuyCourse.width == rect.width()
                ) {
                    binding.belowBuyCv.visibility = View.GONE

                } else {
                    binding.belowBuyCv.visibility = View.VISIBLE

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


                Glide.with(activity!!)
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
                Glide.with(activity!!)
                    .load(courseDetailsResponse.videoThumbnail)
                    .apply(RequestOptions.bitmapTransform(multi))
                    .override(SIZE_ORIGINAL)
                    .into(binding.imageView)


                binding.textViewOfferPrice.text =
                    "₹" + String.format("%.2f", courseDetailsResponse.courseDiscountPrice)

                binding.tvOfferPrice.text =
                    "₹" + String.format("%.2f", courseDetailsResponse.courseDiscountPrice)

                binding.textViewPrice.text =
                    "₹" + String.format("%.2f", courseDetailsResponse.coursePrice)
                binding.tvPrice.text =
                    "₹" + String.format("%.2f", courseDetailsResponse.coursePrice)

                binding.textViewPrice.paintFlags =
                    binding.textViewPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvPrice.paintFlags =
                    binding.tvPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

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

                val titleView = activity!!.findViewById<AppCompatTextView>(R.id.text_message_title)
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


    fun playVideo() {
        videoUrl?.let {
            FullScreenVideoFragment.newInstance(it)
                .show(activity!!.supportFragmentManager, "Video Play")
        }

    }

    fun backPress() {
        activity?.finish()
    }

    fun buyCourse() {
        RxBus2.publish(BuyCourseEventBus(testId.toString()))
    }

    companion object {

        @JvmStatic
        fun newInstance(testId: Int, courseId: Int) =
            CourseDetailType1Fragment().apply {
                arguments = Bundle().apply {
                    putInt(TEST_ID, testId)
                    putInt(COURSE_ID, courseId)
                }
            }
    }
}
