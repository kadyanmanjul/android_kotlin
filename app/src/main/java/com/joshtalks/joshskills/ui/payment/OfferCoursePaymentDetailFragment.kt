package com.joshtalks.joshskills.ui.payment

import android.content.Context
import android.graphics.Paint
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.ImprovedBulletSpan
import com.joshtalks.joshskills.databinding.FragmentOfferCoursePaymentDetailBinding
import com.joshtalks.joshskills.repository.server.CourseExploreModel
import com.joshtalks.joshskills.repository.server.PaymentDetailsResponse
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.support.v4.dip

class OfferCoursePaymentDetailFragment : DialogFragment() {
    private var compositeDisposable = CompositeDisposable()

    private lateinit var binding: FragmentOfferCoursePaymentDetailBinding
    private var courseModel: CourseExploreModel? = null
    private var paymentDetailResponse: PaymentDetailsResponse? = null
    private var listener: OnCourseBuyOfferInteractionListener? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            courseModel = it.getParcelable(COURSE_OBJECT)
            paymentDetailResponse = it.getParcelable(PAYMENT_DETAIL_OBJECT)

        }

        setStyle(STYLE_NO_FRAME, R.style.full_dialog)
    }


    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = AppObjectController.screenWidth * .9
            dialog.window?.setLayout(width.toInt(), FrameLayout.LayoutParams.WRAP_CONTENT)

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_offer_course_payment_detail,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.handler = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showTooltip()

        binding.tvCourse.text = courseModel?.courseName
        binding.tvCourseSubDetail.text =
            getString(R.string.course_duration, courseModel?.courseDuration)

        Glide.with(requireActivity())
            .load(courseModel?.courseIcon)
            .override(Target.SIZE_ORIGINAL)
            .into(binding.courseImage)
        binding.tvCourseFeesAmount.text =
            "₹" + String.format("%.2f", (paymentDetailResponse?.originalAmount))
        binding.tvDiscountAmount.text =
            "₹" + String.format("%.2f", (paymentDetailResponse!!.discountAmount / 100))
        binding.tvOfferInfo.text = getString(R.string.offer_bachat, binding.tvDiscountAmount.text)

        binding.tvCourseSellAmount.text =
            "₹" + String.format("%.2f", (paymentDetailResponse!!.amount / 100))
        binding.tvCourseActualAmount.text = binding.tvCourseFeesAmount.text

        binding.tvCourseActualAmount.paintFlags =
            binding.tvCourseActualAmount.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

        val spannableString = SpannableString(getString(R.string.tip_message))

        spannableString.setSpan(
            ImprovedBulletSpan(bulletRadius = dip(3), gapWidth = dip(8)),
            0,
            spannableString.length,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )

        binding.tvTip.text = spannableString

        compositeDisposable.add(AppObjectController.appDatabase
            .courseDao()
            .isUserInOfferDays()
            .concatMap {
                val (_, dayRemain) = com.joshtalks.joshskills.core.Utils.isUserInDaysOld(it.courseCreatedDate)
                return@concatMap Maybe.just(dayRemain)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { value ->
                    binding.tvTipValid.text = "VALID FOR $value DAYS"
                },
                { error ->
                    error.printStackTrace()
                }
            ))
    }

    fun buyCourse() {
        if (courseModel != null && paymentDetailResponse != null) {
            listener?.onCompleteOfferPayment(courseModel!!, paymentDetailResponse!!)
        }
        dismissAllowingStateLoss()

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnCourseBuyOfferInteractionListener) {
            listener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    private fun showTooltip() {
        HintTooltipDialog.newInstance().show(requireActivity().supportFragmentManager, "Hint")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AppAnalytics.create(AnalyticsEvent.BACK_PRESSED.NAME)
            .addParam("name", javaClass.simpleName)
            .push()
    }

    companion object {
        @JvmStatic
        fun newInstance(courseModel: CourseExploreModel, paymentDetail: PaymentDetailsResponse) =
            OfferCoursePaymentDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(COURSE_OBJECT, courseModel)
                    putParcelable(PAYMENT_DETAIL_OBJECT, paymentDetail)

                }
            }
    }


    interface OnCourseBuyOfferInteractionListener {
        fun onCompleteOfferPayment(
            courseModel: CourseExploreModel,
            paymentDetailResponse: PaymentDetailsResponse
        )
    }


}
