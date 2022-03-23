package com.joshtalks.joshskills.ui.userprofile

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
import com.joshtalks.joshskills.databinding.FragmentMyGroupsBinding
import com.joshtalks.joshskills.repository.server.GroupInfo
import com.joshtalks.joshskills.ui.userprofile.adapter.MyGroupsListAdapter
import com.joshtalks.joshskills.ui.userprofile.viewmodel.UserProfileViewModel

class MyGroupsFragment : DialogFragment() {
    lateinit var binding: FragmentMyGroupsBinding
    private val viewModel by lazy {
        ViewModelProvider(requireActivity()).get(
            UserProfileViewModel::class.java
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BaseBottomSheetDialogBlank)
        changeDialogConfiguration()
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
            R.layout.fragment_my_groups,
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
        viewModel.userData.observe(
            this, { userProfileResponse ->
                hideProgressBar()
                userProfileResponse?.myGroupsList?.let{
                    initView(it)
                }

            })

        viewModel.apiCallStatusLiveData.observe(this) {
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

        viewModel.apiCallStatus.observe(this) {
            when (it) {
                ApiCallStatus.SUCCESS -> {
                    hideProgressBar()
                }
                ApiCallStatus.FAILED -> {
                    hideProgressBar()
                }
                ApiCallStatus.START -> {
                    showProgressBar()
                }
                else -> {
                    hideProgressBar()

                }
            }
        }

    }

    private fun addListeners() {
        binding.ivBack.setOnClickListener {
            dismiss()
        }
    }

    private fun initView(myGroupsList: List<GroupInfo>) {
        val recyclerView: RecyclerView = binding.rvGroups
            recyclerView.setHasFixedSize(true)
            recyclerView.apply {
                this.layoutManager = LinearLayoutManager(context)
                this.adapter = MyGroupsListAdapter( myGroupsList)

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
        fun newInstance() = MyGroupsFragment()
    }
}
