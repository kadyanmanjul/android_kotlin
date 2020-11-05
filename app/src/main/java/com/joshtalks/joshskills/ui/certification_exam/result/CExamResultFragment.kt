package com.joshtalks.joshskills.ui.certification_exam.result

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FragmentCexamResultBinding


class CExamResultFragment : Fragment() {

    companion object {
        fun newInstance() = CExamResultFragment().apply {
            arguments = Bundle().apply {
            }
        }
    }

    private lateinit var binding: FragmentCexamResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_cexam_result, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }


}