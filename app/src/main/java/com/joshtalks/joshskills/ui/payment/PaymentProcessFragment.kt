package com.joshtalks.joshskills.ui.payment

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.COURSE_ID
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.PaymentProcessFragmentBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.OTPReceivedEventBus
import com.joshtalks.joshskills.repository.server.CourseExploreModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.*

class PaymentProcessFragment : DialogFragment() {
    private var compositeDisposable = CompositeDisposable()
    private var courseModel: CourseExploreModel? = null
    private lateinit var paymentProcessFragmentBinding: PaymentProcessFragmentBinding
    private var timer = Timer()

    private var animAlpha: Animation? = null
    private var animMoveToTop: Animation? = null

    companion object {
        @JvmStatic
        fun newInstance(courseModel: CourseExploreModel) =
            PaymentProcessFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(COURSE_ID, courseModel)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        activity?.run {
            animMoveToTop = AnimationUtils.loadAnimation(applicationContext, R.anim.translate)
            animAlpha = AnimationUtils.loadAnimation(applicationContext, R.anim.fade_out)
        }
        arguments?.let {
            courseModel = it.getParcelable(COURSE_ID) as CourseExploreModel?
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
        animAlpha?.reset()
        if (courseModel?.amount == 0.0) {
            paymentProcessFragmentBinding.textView.visibility = View.GONE
        }

        paymentProcessFragmentBinding.tvCourse.text = courseModel?.courseName
        paymentProcessFragmentBinding.tvAmount.text =
            "INR " + String.format("%.2f", courseModel?.amount)
        activity?.let {
            Glide.with(it)
                .load(courseModel?.courseIcon)
                .optionalTransform(
                    WebpDrawable::class.java,
                    WebpDrawableTransformation(CircleCrop())
                )
                .into(paymentProcessFragmentBinding.courseImage)
        }


        animAlpha!!.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {

            }

            override fun onAnimationStart(animation: Animation?) {
                paymentProcessFragmentBinding.successIv.visibility = View.VISIBLE

            }

        })
        animMoveToTop!!.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {

                paymentProcessFragmentBinding.rlContainer.visibility = View.VISIBLE
                paymentProcessFragmentBinding.successIv.visibility = View.GONE
                paymentProcessFragmentBinding.btnInbox.visibility = View.VISIBLE
            }

            override fun onAnimationStart(animation: Animation?) {

            }

        })



        Handler(Looper.getMainLooper()).postDelayed({
            paymentProcessFragmentBinding.successIv.startAnimation(animAlpha)
        }, 750)

        paymentProcessFragmentBinding.btnInbox.setOnClickListener {
            startActivity((activity as BaseActivity).getInboxActivityIntent())
            activity?.finish()
        }
    }

    fun gotToCourse() {
        startActivity((activity as BaseActivity).getInboxActivityIntent())
        activity?.finish()

    }

    override fun onResume() {
        super.onResume()
        addObserver()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        timer.cancel()
    }

    private fun addObserver() {
        compositeDisposable.add(
            RxBus2.listen(OTPReceivedEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    AppObjectController.uiHandler.postDelayed({
                        startActivity((requireActivity() as BaseActivity).getInboxActivityIntent())
                        requireActivity().finish()
                    }, 1000 * 60)
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
}