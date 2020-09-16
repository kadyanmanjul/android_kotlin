package com.joshtalks.joshskills.ui.conversation_practice.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentQuizPractiseBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.VPPageChangeEventBus
import com.joshtalks.joshskills.repository.server.conversation_practice.QuizModel
import com.joshtalks.joshskills.ui.conversation_practice.adapter.OnChoiceClickListener2
import com.joshtalks.joshskills.ui.conversation_practice.adapter.QuizPractiseAdapter

class QuizPractiseFragment private constructor() : Fragment(), OnChoiceClickListener2 {
    private lateinit var quizModelList: List<QuizModel>
    private lateinit var binding: FragmentQuizPractiseBinding
    private var isEvaluate = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            quizModelList = it.getParcelableArrayList(ARG_QUIZ_LIST) ?: emptyList()
            quizModelList = quizModelList.sortedBy { it.sortOrder }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_quiz_practise,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    companion object {
        private const val ARG_QUIZ_LIST = "quiz-list"

        @JvmStatic
        fun newInstance(quizModelList: List<QuizModel>) =
            QuizPractiseFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(ARG_QUIZ_LIST, ArrayList(quizModelList))
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewPager.orientation = ORIENTATION_VERTICAL
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.adapter = QuizPractiseAdapter(quizModelList, this)
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.questionNumberTv.text = (position + 1).toString().plus("/")
                binding.btnSubmit.isEnabled = false
                binding.btnSubmit.isClickable = false
                binding.btnSubmit.backgroundTintList = ContextCompat.getColorStateList(
                    AppObjectController.joshApplication,
                    R.color.light_grey
                )
                binding.btnSubmit.text = getString(R.string.submit)
                isEvaluate = false
            }
        })
        binding.totalQuestionTv.text = quizModelList.size.toString()
        logQuizAnalyticsEvents()
    }

    private fun logQuizAnalyticsEvents() {
        AppAnalytics.create(AnalyticsEvent.QUIZ_TEST_OPENED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam("flow", "Conversational prac")
            .push()
    }


    private fun logChoiceSelectedEvent(answersModelID: String) {
        AppAnalytics.create(AnalyticsEvent.CONVO_OPTION_SELECTED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.OPTION_TYPE.NAME,answersModelID)
            .addParam("flow", "Conversational prac")
            .push()
    }

    private fun logSubmitButtonAnalyticEvent(answersModelID: String) {
        AppAnalytics.create(AnalyticsEvent.CON_QUIZ_SUBMIT_BUTTON_CLICKED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.CHOICE_ID.NAME,answersModelID)
            .addParam("flow", "Conversational prac")
            .push()
    }

    fun submit() {
        if (isEvaluate) {
            if (binding.viewPager.currentItem == (quizModelList.size - 1)) {
                RxBus2.publish(VPPageChangeEventBus())
            }
            binding.viewPager.currentItem = binding.viewPager.currentItem + 1
        } else {
            val quizModel = quizModelList[binding.viewPager.currentItem].apply {
                isAttempted = true
            }
            //quizModel.isAttempted = true
            val resp = quizModel.answersModel.find { it.isCorrect && it.isSelectedByUser }
            if (resp != null) {
                showToast("Your answer is Correct")
            } else {
                showToast("Your answer is Wrong")
            }

            quizModel.answersModel.listIterator().forEach {
                it.isEvaluate = true
            }
            logSubmitButtonAnalyticEvent(quizModel.id.toString())
            binding.viewPager.adapter?.notifyDataSetChanged()
            isEvaluate = true
            btnTextSetup()
        }
    }

    override fun onChoiceSelectListener(answersModelId: Int) {
        logChoiceSelectedEvent(answersModelId.toString())
        binding.btnSubmit.isEnabled = true
        binding.btnSubmit.isClickable = true
        binding.btnSubmit.backgroundTintList = ContextCompat.getColorStateList(
            AppObjectController.joshApplication,
            R.color.button_color
        )
    }

    private fun btnTextSetup() {
        if (binding.viewPager.currentItem == (quizModelList.size - 1)) {
            binding.btnSubmit.text = getString(R.string.finish)
        } else {
            binding.btnSubmit.text = getString(R.string.next)
        }
    }

}