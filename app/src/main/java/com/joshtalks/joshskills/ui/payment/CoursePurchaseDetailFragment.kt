package com.joshtalks.joshskills.ui.payment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.FragmentCoursePurchaseDetailBinding
import com.joshtalks.joshskills.repository.server.CourseExploreModel
import com.joshtalks.joshskills.ui.tooltip.BalloonFactory
import com.joshtalks.skydoves.balloon.Balloon
import com.joshtalks.skydoves.balloon.OnBalloonDismissListener


class CoursePurchaseDetailFragment : DialogFragment() {
    private var listener: OnCourseDetailInteractionListener? = null
    private lateinit var binding: FragmentCoursePurchaseDetailBinding
    private var courseModel: CourseExploreModel?=null
    private var hasCertificate: Boolean = false

    private var balloonTooltip: Balloon? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            courseModel = it.getParcelable(COURSE_OBJECT)
            hasCertificate = it.getBoolean(HAS_CERTIFICATE)
        }

        setStyle(STYLE_NO_FRAME, R.style.full_dialog)
    }


    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = AppObjectController.screenWidth * .85
            dialog.window?.setLayout(width.toInt(), FrameLayout.LayoutParams.WRAP_CONTENT)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_course_purchase_detail,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvCourseName.text = courseModel?.courseName
        binding.tvCourseAmount.text = "₹" + String.format("%.2f", courseModel?.amount)
        Glide.with(view.context)
            .load(courseModel?.courseIcon)
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .into(binding.ivCourse)



        if (hasCertificate.not()) {
            binding.tvDigitalCertificate.visibility = View.GONE
            binding.tvCertificateIncl.visibility = View.GONE
            binding.tvDigitalInfo.visibility = View.GONE
            return
        }
        binding.tvDigitalInfo.setOnClickListener {
            showTooltip()
        }

        balloonTooltip = BalloonFactory.getTooltipForCertificate(
            requireActivity(),
            this,
            object : OnBalloonDismissListener {
                override fun onBalloonDismiss() {
                    balloonTooltip?.isShowing = false
                }

            })
    }

    private fun showTooltip() {
        balloonTooltip?.isShowing?.run {
            if (this.not()) {
                balloonTooltip?.showAlignTopWithException(binding.tvDigitalInfo)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnCourseDetailInteractionListener) {
            listener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    fun completePayment() {
        listener?.onCompletePayment()
        dismissAllowingStateLoss()

    }

    fun haveCouponCode() {

        listener?.onCouponCode()
        dismissAllowingStateLoss()
    }


    companion object {
        @JvmStatic
        fun newInstance(courseModel: CourseExploreModel, hasCertificate: Boolean) =
            CoursePurchaseDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(COURSE_OBJECT, courseModel)
                    putBoolean(HAS_CERTIFICATE, hasCertificate)
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AppAnalytics.create(AnalyticsEvent.BACK_PRESSED.NAME)
            .addParam("name", javaClass.simpleName)
            .push()
    }

    interface OnCourseDetailInteractionListener {
        fun onCompletePayment()
        fun onCouponCode()
    }
}
