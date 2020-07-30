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
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentQuizPractiseBinding
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
        binding.viewPager.adapter = QuizPractiseAdapter(quizModelList, this)
        binding.viewPager.orientation = ORIENTATION_VERTICAL
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.offscreenPageLimit = 4
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
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
    }

    fun submit() {
        if (isEvaluate) {
            binding.viewPager.currentItem = binding.viewPager.currentItem + 1
        } else {
            val cItem = binding.viewPager.currentItem
            val quizModel = quizModelList[cItem]

            val resp = quizModel.answersModel.find { it.isCorrect && it.isSelectedByUser }
            if (resp != null) {
                showToast("Sahi hai")
            } else {
                showToast("Galat hai")
            }
            quizModel.answersModel.listIterator().forEach { it.isEvaluate = true }
            binding.viewPager.adapter?.notifyItemChanged(cItem)
            binding.btnSubmit.text = getString(R.string.next)
            isEvaluate = true
        }
    }

    override fun onChoiceSelectListener() {
        binding.btnSubmit.isEnabled = true
        binding.btnSubmit.isClickable = true
        binding.btnSubmit.backgroundTintList = ContextCompat.getColorStateList(
            AppObjectController.joshApplication,
            R.color.button_primary_color
        )
        if (binding.viewPager.currentItem == (quizModelList.size - 1)) {
            binding.btnSubmit.text = getString(R.string.finish)
        } else {
            binding.btnSubmit.text = getString(R.string.submit)
        }
    }

}