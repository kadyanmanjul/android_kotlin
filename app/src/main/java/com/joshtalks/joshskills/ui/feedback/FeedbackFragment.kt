package com.joshtalks.joshskills.ui.feedback

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.RATING_DETAILS_KEY
import com.joshtalks.joshskills.databinding.LayoutDialogFeedbackBinding
import com.joshtalks.joshskills.repository.server.feedback.RatingDetails
import com.joshtalks.joshskills.repository.server.feedback.RatingModel
import io.reactivex.disposables.CompositeDisposable
import java.lang.reflect.Type

const val QUESTION_ID = "question_id"

class FeedbackFragment : DialogFragment(), FeedbackOptionAdapter.OnFeedbackItemListener {

    private lateinit var binding: LayoutDialogFeedbackBinding
    private var compositeDisposable = CompositeDisposable()
    private val ratingDetailsTypeToken: Type = object : TypeToken<List<RatingDetails>>() {}.type
    private var ratingDetailsList: List<RatingDetails> = emptyList()
    private lateinit var viewModel: FeedbackViewModel
    private var issueLabel: String? = null
    private var questionId: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = activity?.run { ViewModelProvider(this).get(FeedbackViewModel::class.java) }
            ?: throw Exception("Invalid Activity")
        arguments?.let {
            questionId = it.getString(QUESTION_ID)
        }
        setStyle(STYLE_NO_FRAME, R.style.full_dialog)
    }


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
    }

    private fun setUpRatingBar() {
        try {
            val layoutManager = FlexboxLayoutManager(requireContext())
            layoutManager.flexDirection = FlexDirection.ROW
            layoutManager.justifyContent = JustifyContent.CENTER
            binding.ratingOptionRv.layoutManager = layoutManager
            ratingDetailsList = AppObjectController.gsonMapper.fromJson(
                PrefManager.getStringValue(RATING_DETAILS_KEY),
                ratingDetailsTypeToken
            )
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
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        )
        scaleAnimation.interpolator = AccelerateDecelerateInterpolator()
        scaleAnimation.startOffset = 500
        scaleAnimation.duration = 750
        scaleAnimation.fillAfter = true
        scaleAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {

            }

            override fun onAnimationStart(animation: Animation?) {
            }

        })
        binding.successIv.startAnimation(scaleAnimation)
    }

    fun submitFeedback() {
        if (issueLabel.isNullOrEmpty()) {
            return
        }
        viewModel.submitFeedback(
            questionId!!,
            binding.feedbackRatingBar.rating,
            issueLabel!!,
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

    companion object {
        fun newInstance(questionId: String) = FeedbackFragment().apply {
            arguments = Bundle().apply {
                putString(QUESTION_ID, questionId)
            }
        }
    }
}


