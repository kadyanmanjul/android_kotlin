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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.databinding.FragmentPreviousProfilePicsBinding
import com.joshtalks.joshskills.repository.server.ProfilePicture
import com.joshtalks.joshskills.repository.server.PreviousProfilePictures
import com.joshtalks.joshskills.ui.extra.ImageShowFragment

class PreviousProfilePicsFragment : DialogFragment() {
    lateinit var binding: FragmentPreviousProfilePicsBinding
    private val viewModel by lazy {
        ViewModelProvider(activity as UserProfileActivity).get(
            UserProfileViewModel::class.java
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BaseBottomSheetDialogBlank)
        changeDialogConfiguration()
        // viewModel.getPreviousProfilePics()
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
            R.layout.fragment_previous_profile_pics,
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
            this, {
                hideProgressBar()
                initView(it?.previousProfilePictures)
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
            if (it == ApiCallStatus.SUCCESS) {
                hideProgressBar()
            } else if (it == ApiCallStatus.FAILED) {
                hideProgressBar()
            } else if (it == ApiCallStatus.START) {
                showProgressBar()
            }
        }

        viewModel.previousProfilePics.observe(this) {
            initView(it)
        }

    }

    private fun addListeners() {
        binding.ivBack.setOnClickListener {
            dismiss()
        }
    }

    private fun initView(previousProfilePics: PreviousProfilePictures?) {
        previousProfilePics?.profilePictures?.sortedBy { it.timestamp?.time }
            ?.let { picsList ->
                val recyclerView: RecyclerView = binding.rvPreviousPics
                val layoutManager = GridLayoutManager(context, 3)
                recyclerView.layoutManager = layoutManager
                recyclerView.setHasFixedSize(true)
                recyclerView.adapter = PreviousPicsAdapter(
                    picsList,
                    object : PreviousPicsAdapter.OnPreviousPicClickListener {
                        override fun onPreviousPicClick(profilePicture: ProfilePicture) {
                            ImageShowFragment.newInstance(profilePicture.photoUrl, null, null)
                                .show(activity!!.supportFragmentManager, "ImageShow")
                        }
                    })
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
        fun newInstance() = PreviousProfilePicsFragment()
    }
}
