package com.joshtalks.joshskills.ui.payment

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.PaymentProcessFragmentBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.OTPReceivedEventBus
import com.joshtalks.joshskills.repository.server.CourseExploreModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class PaymentProcessFragment : DialogFragment() {
    private var compositeDisposable = CompositeDisposable()
    private lateinit var courseModel: CourseExploreModel
    private lateinit var paymentProcessFragmentBinding: PaymentProcessFragmentBinding


    companion object {
        @JvmStatic
        fun newInstance(courseModel: CourseExploreModel) =
            PaymentProcessFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(COURSE_ID, courseModel)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            courseModel = it.getSerializable(COURSE_ID) as CourseExploreModel
        }
        setStyle(STYLE_NO_FRAME, R.style.full_dialog)

    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window?.setLayout(width, height)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        paymentProcessFragmentBinding =
            DataBindingUtil.inflate(inflater, R.layout.payment_process_fragment, container, false)
        paymentProcessFragmentBinding.lifecycleOwner = this
        paymentProcessFragmentBinding.handler = this
        return paymentProcessFragmentBinding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        paymentProcessFragmentBinding.tvCourse.text = courseModel.courseName
        paymentProcessFragmentBinding.tvAmount.text = "INR " + (courseModel.amount / 100).toString()
        paymentProcessFragmentBinding.tvCourseDuration.text = courseModel.courseDuration

        activity?.let {
            Glide.with(it)
                .load(courseModel.courseIcon)
                .optionalTransform(
                    WebpDrawable::class.java,
                    WebpDrawableTransformation(CircleCrop())
                )
                .into(paymentProcessFragmentBinding.ivCourse)
        }
    }

    override fun onResume() {
        super.onResume()
        addObserver()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    fun exploreMoreCourse() {
        (activity as PaymentActivity).finish()
    }

    private fun addObserver() {
        compositeDisposable.add(
            RxBus2.listen(OTPReceivedEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Handler().postDelayed({
                        startActivity((activity as PaymentActivity).getInboxActivityIntent())
                        (activity as PaymentActivity).finish()
                    }, 1000 * 60)
                }, {
                    it.printStackTrace()
                })
        )


    }
}