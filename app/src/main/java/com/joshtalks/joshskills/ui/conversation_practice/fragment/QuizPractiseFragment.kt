package com.joshtalks.joshskills.ui.conversation_practice.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FragmentQuizPractiseBinding
import com.joshtalks.joshskills.repository.server.conversation_practice.QuizModel

class QuizPractiseFragment private constructor() : Fragment() {
    private lateinit var quizModelList: List<QuizModel>
    private lateinit var binding: FragmentQuizPractiseBinding

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

    fun submit() {

    }

}