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
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.databinding.FragmentCoursePurchaseDetailBinding
import com.joshtalks.joshskills.repository.server.CourseExploreModel


class CoursePurchaseDetailFragment : DialogFragment() {
    private var listener: OnCourseDetailInteractionListener? = null
    private lateinit var binding: FragmentCoursePurchaseDetailBinding
    private lateinit var courseModel: CourseExploreModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            courseModel = it.getSerializable(COURSE_OBJECT) as CourseExploreModel
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

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvCourseName.text = courseModel.courseName
        binding.tvCourseAmount.text = "₹" + (courseModel.amount).toString()
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
        fun newInstance(courseModel: CourseExploreModel) =
            CoursePurchaseDetailFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(COURSE_OBJECT, courseModel)
                }
            }
    }

    interface OnCourseDetailInteractionListener {
        fun onCompletePayment()
        fun onCouponCode()
    }
}
