package com.joshtalks.joshskills.ui.conversation_practice.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.repository.server.conversation_practice.QuizModel

class QuizPractiseFragment private constructor() : Fragment() {
    private lateinit var quizModelList: List<QuizModel>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            quizModelList = it.getParcelableArrayList(ARG_QUIZ_LIST) ?: emptyList()
        }
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
}