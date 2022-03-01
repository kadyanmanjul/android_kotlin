package com.joshtalks.joshskills.ui.userprofile.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.databinding.FragmentEnrolledCoursesBinding
import com.joshtalks.joshskills.ui.userprofile.models.EnrolledCoursesList
import com.joshtalks.joshskills.ui.userprofile.adapters.EnrolledCoursesListAdapter
import com.joshtalks.joshskills.ui.userprofile.viewmodel.UserProfileViewModel

class EnrolledCoursesFragement : DialogFragment() {
    lateinit var binding: FragmentEnrolledCoursesBinding
    private var startTime = 0L
    private var impressionId : String? =null
    private val viewModel by lazy {
        ViewModelProvider(requireActivity()).get(
            UserProfileViewModel::class.java
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BaseBottomSheetDialogBlank)
        changeDialogConfiguration()
        startTime = System.currentTimeMillis()
    }

    private fun changeDialogConfiguration() {
        val params: WindowManager.LayoutParams? = dialog?.window?.attributes
        params?.width = WindowManager.LayoutParams.MATCH_PARENT
        params?.height = WindowManager.LayoutParams.WRAP_CONTENT
        params?.gravity = Gravity.BOTTOM
        dialog?.window?.attributes = params
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        isCancelable = true
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_enrolled_courses,
            container,
            false
        )
        binding.lifecycleOwner = viewLifecycleOwner
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addObservers()
        addListeners()
    }

    private fun addObservers() {
        viewModel.coursesList.observe(
            this
        ) {
            hideProgressBar()
            initView(it)
        }

        viewModel.apiCallStatusForCoursesList.observe(this) {
            when (it) {
                ApiCallStatus.SUCCESS -> {
                    hideProgressBar()
                }
                ApiCallStatus.FAILED -> {
                    hideProgressBar()
                    this.dismiss()
                }
                ApiCallStatus.START -> {
                    showProgressBar()
                }
                else -> {
                    hideProgressBar()
                    this.dismiss()
                }
            }
        }
        viewModel.sectionImpressionResponse.observe(this){
            impressionId=it.sectionImpressionId
        }
    }

    private fun addListeners() {
        binding.ivBack.setOnClickListener {
            dismiss()
        }
    }
    override fun onPause() {
        startTime = System.currentTimeMillis().minus(startTime).div(1000)
        if (startTime > 0 && impressionId!!.isBlank().not()) {
            viewModel.engageUserProfileSectionTime(impressionId!!, startTime.toString())
        }
        super.onPause()
    }

    private fun initView(enrolledCoursesList: EnrolledCoursesList) {
        val recyclerView: RecyclerView = binding.rvCourses
        recyclerView.setHasFixedSize(true)
        recyclerView.apply {
            this.layoutManager = LinearLayoutManager(context)
            this.adapter = EnrolledCoursesListAdapter( enrolledCoursesList.courses)

        }
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        binding.progressBar.visibility = View.GONE
    }

    companion object {
        @JvmStatic
        fun newInstance() = EnrolledCoursesFragement()
    }
}
