package com.joshtalks.joshskills.ui.assessment.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FragmentQuizSuccessBinding

class QuizSuccessFragment : Fragment() {
    private lateinit var binding: FragmentQuizSuccessBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,

        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_quiz_success, container, false)
        binding.lifecycleOwner = this
        binding.fragment = this
        return binding.root
    }

    fun backToCourse() {
        requireActivity().finish()
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            QuizSuccessFragment()
    }
}
