package com.joshtalks.joshskills.ui.cohort_based_course.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.databinding.FragmentPromiseBinding
import com.joshtalks.joshskills.ui.cohort_based_course.viewmodels.CommitmentFormViewModel

class PromiseFragment : BaseFragment() {

    lateinit var binding: FragmentPromiseBinding

    val vm by lazy {
        ViewModelProvider(this)[CommitmentFormViewModel::class.java]
    }

    override fun initViewBinding() {
        binding.executePendingBindings()
    }

    override fun initViewState() {}

    override fun setArguments() {}


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_promise, container, false)
        return binding.root    }

}