package com.joshtalks.joshskills.ui.feedback

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.LayoutDialogFeedbackBinding
import com.joshtalks.joshskills.repository.server.feedback.FeedbackTypes
import com.joshtalks.joshskills.repository.server.feedback.RatingDetails
import com.joshtalks.joshskills.repository.server.feedback.RatingModel
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.progress_layout.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.reflect.Type

const val QUESTION_ID = "question_id"
const val FEEDBACK_TYPE = "feedback_type"

class FeedbackFragment : DialogFragment(), FeedbackOptionAdapter.OnFeedbackItemListener {

    private lateinit var binding: LayoutDialogFeedbackBinding
    private var compositeDisposable = CompositeDisposable()
    private val ratingDetailsTypeToken: Type = object : TypeToken<List<RatingDetails>>() {}.type
    private var ratingDetailsList: List<RatingDetails> = emptyList()
    private lateinit var viewModel: FeedbackViewModel
    private var issueLabel: String = EMPTY
    private var feedbackType: FeedbackTypes? = null
    private var questionId: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = activity?.run { ViewModelProvider(this).get(FeedbackViewModel::class.java) }
            ?: throw Exception("Invalid Activity")
        arguments?.let {
            questionId = it.getString(QUESTION_ID)
            feedbackType = it.getParcelable(FEEDBACK_TYPE)
        }
        android.R.style.Theme_Black_NoTitleBar_Fullscreen
        setStyle(STYLE_NO_FRAME, R.style.FullDialogWithAnimation)
        AppAnalytics.create(AnalyticsEvent.FEEDBACK_INITIATED.NAME)
            .addParam("QUESTION_ID", questionId)
            .addParam("type", feedbackType?.name)
            .push()

    }

/*

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {

            dialog.window?.setLayout(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            dialog.setCanceledOnTouchOutside(true)
            dialog.setCancelable(true)
            dialog.window?.setBackgroundDrawableResource(android.R.color.white)
        }
    }
*/


    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.layout_dialog_feedback,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        successIvShow()
        binding.feedbackRatingBar.setOnRatingBarChangeListener { ratingBar, rating, _ ->
            if (rating < 1.0f) {
                ratingBar.rating = 1.0f
                return@setOnRatingBarChangeListener
            }
            setupRatingOptions(rating.toInt())
        }
        setUpRatingBar()
        viewModel.apiCallStatusLiveData.observe(this, Observer {
            if (it == ApiCallStatus.SUCCESS) {
                AppAnalytics.create(AnalyticsEvent.FEEDBACK_SUBMITTED.NAME)
                    .addParam("QUESTION_ID", questionId)
                    .addParam("type", feedbackType?.name)
                    .push()
                CoroutineScope(Dispatchers.Main).launch {
                    delay(1500)
                    dismissAllowingStateLoss()
                }
            } else {
                progress_layout.visibility = View.GONE
            }
        })
        feedbackType?.run {
            if (this == FeedbackTypes.PRACTISE) {
                binding.topFeedbackTitle.text = getString(R.string.practice_submitted)
                binding.feedbackTitle.text = getString(R.string.feedback_sub_practise)
            } else if (this == FeedbackTypes.VIDEO) {
                binding.feedbackTitle.text = getString(R.string.feedback_sub_video)
            }
        }
    }

    private fun setUpRatingBar() {
        try {
            val layoutManager = FlexboxLayoutManager(requireContext())
            layoutManager.flexDirection = FlexDirection.ROW
            layoutManager.justifyContent = JustifyContent.CENTER
            layoutManager.flexWrap = FlexWrap.WRAP

            binding.ratingOptionRv.layoutManager = layoutManager
            ratingDetailsList = AppObjectController.gsonMapper.fromJson(
                PrefManager.getStringValue(RATING_DETAILS_KEY),
                ratingDetailsTypeToken
            )
            ratingDetailsList = ratingDetailsList.sortedWith(compareBy { it.rating })
            binding.feedbackRatingBar.numStars = ratingDetailsList.size
            binding.feedbackRatingBar.rating = ratingDetailsList.size.toFloat()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

    }

    private fun setupRatingOptions(ratingPos: Int) {
        binding.feedbackRatingInWords.text = ratingDetailsList[ratingPos - 1].infoText
        val ratingModelList: ArrayList<RatingModel> = arrayListOf()
        ratingDetailsList[ratingPos - 1].keywordsList.forEach {
            ratingModelList.add(RatingModel(it, ratingPos, false, enable = true))
        }
        ratingModelList.add(RatingModel("&#9679;&#9679;&#9679;", ratingPos, false, enable = false))
        binding.ratingOptionRv.adapter = FeedbackOptionAdapter(this, ratingModelList)

    }


    private fun successIvShow() {
        binding.successIv.visibility = View.VISIBLE
        val scaleAnimation = ScaleAnimation(
            0f,
            1f,
            0f,
            1f,
            Animation.RELATIVE_TO_SELF,
            0.50f,
            Animation.RELATIVE_TO_SELF,
            0.50f
        )
        scaleAnimation.interpolator = AccelerateDecelerateInterpolator()
        scaleAnimation.startOffset = 500
        scaleAnimation.duration = 750
        scaleAnimation.fillAfter = true
        scaleAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                binding.group.visibility = View.VISIBLE
                binding.group.updatePreLayout(binding.subRootView)
                if (binding.topFeedbackTitle.text.isNullOrEmpty().not()) {
                    binding.topFeedbackTitle.visibility = View.VISIBLE
                }
            }

            override fun onAnimationStart(animation: Animation?) {
            }

        })
        binding.successIv.startAnimation(scaleAnimation)

    }

    fun submitFeedback() {
        hideKeyboard(requireActivity(), binding.etFeedback)
        progress_layout.visibility = View.VISIBLE
        viewModel.submitFeedback(
            questionId!!,
            binding.feedbackRatingBar.rating,
            issueLabel,
            binding.etFeedback.text.toString()
        )
    }

    override fun onSelectOption(label: String) {
        issueLabel = label

    }

    override fun onWriteComment() {
        binding.etFeedback.visibility = View.VISIBLE
        binding.ratingOptionRv.visibility = View.GONE
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (viewModel.apiCallStatusLiveData.value == null) {
            AppAnalytics.create(AnalyticsEvent.FEEDBACK_IGNORE.NAME)
                .addParam("QUESTION_ID", questionId)
                .addParam("type", feedbackType?.name)
                .push()
        }
        viewModel.updateQuestionFeedbackStatus(questionId!!)
        super.onDismiss(dialog)
    }

    companion object {
        fun newInstance(feedbackType: FeedbackTypes, questionId: String) =
            FeedbackFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(FEEDBACK_TYPE, feedbackType)
                    putString(QUESTION_ID, questionId)
                }
            }
    }
}


