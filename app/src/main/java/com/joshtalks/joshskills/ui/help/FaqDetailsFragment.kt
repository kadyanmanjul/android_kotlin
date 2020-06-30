package com.joshtalks.joshskills.ui.help

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.FragmentFaqDetailBinding
import com.joshtalks.joshskills.repository.server.FAQ
import kotlinx.android.synthetic.main.fragment_faq_detail.no_btn
import kotlinx.android.synthetic.main.fragment_faq_detail.yes_btn
import org.jetbrains.anko.textColor

class FaqDetailsFragment : Fragment() {
    private lateinit var binding: FragmentFaqDetailBinding
    private lateinit var viewModel: HelpViewModel
    private lateinit var faq: FAQ
    private lateinit var appAnalytics: AppAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            faq = it.getParcelable(FAQ_DETAILS)!!
        }
        viewModel = ViewModelProvider(this).get(HelpViewModel::class.java)
        appAnalytics=AppAnalytics.create(AnalyticsEvent.FAQ_QUESTION_SCREEN.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.FAQ_SLECTED.NAME.plus(" id"),faq.id)
            .addParam(AnalyticsEvent.FAQ_QUESTION_FEEDBACK.NAME,"none")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_faq_detail, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.faqQuestion.text = faq.question
        binding.faqAnswers.text = faq.answer
    }

    fun setIsAnswerHelpful(isAnswerHelpful: Boolean) {
        if (isAnswerHelpful) {
            yes_btn.backgroundTintList =
                ContextCompat.getColorStateList(requireActivity(), R.color.button_color)
            yes_btn.textColor = ContextCompat.getColor(requireActivity(), R.color.white)
        } else {
            no_btn.backgroundTintList =
                ContextCompat.getColorStateList(requireActivity(), R.color.button_color)
            no_btn.textColor = ContextCompat.getColor(requireActivity(), R.color.white)
        }
        yes_btn.isEnabled = false
        no_btn.isEnabled = false
        appAnalytics.addParam(AnalyticsEvent.FAQ_QUESTION_FEEDBACK.NAME,isAnswerHelpful)
        patchRequestForAnswer(isAnswerHelpful)
    }

    private fun patchRequestForAnswer(isAnswerHelpful: Boolean) {
        viewModel.postFaqFeedback(faq.id.toString(), isAnswerHelpful)
    }

    fun dismiss() {
        requireActivity().finish()
    }

    override fun onStop() {
        appAnalytics.push()
        super.onStop()
    }

    companion object {
        const val FAQ_DETAILS = "faq_details"

        @JvmStatic
        fun newInstance(faq: FAQ) =
            FaqDetailsFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(FAQ_DETAILS, faq)
                }
            }
    }
}
