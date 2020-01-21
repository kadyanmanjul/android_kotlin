package com.joshtalks.joshskills.ui.signup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.SignUpStepStatus
import com.joshtalks.joshskills.databinding.FragmentEmptyCourseBinding

class EmptyCourseFragment : Fragment() {

    private lateinit var emptyCourseBinding: FragmentEmptyCourseBinding
    private lateinit var viewModel: SignUpViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = activity?.run { ViewModelProviders.of(this)[SignUpViewModel::class.java] }
            ?: throw Exception("Invalid Activity")

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        emptyCourseBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_empty_course, container, false)
        emptyCourseBinding.lifecycleOwner = this
        emptyCourseBinding.handler = this
        return emptyCourseBinding.root
    }


    fun exploreMoreCourse() {
        viewModel.signUpStatus.postValue(SignUpStepStatus.SignUpWithoutRegister)

    }

    fun reLogin() {
        viewModel.signUpStatus.postValue(SignUpStepStatus.SignUpStepFirst)
    }

    companion object {

        @JvmStatic
        fun newInstance() =
            EmptyCourseFragment().apply {}
    }
}

