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
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.getRandomName
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentEditProfileBinding
import com.joshtalks.joshskills.repository.server.UserProfileResponse
import java.util.Locale

class EditProfileFragment : DialogFragment() {

    lateinit var binding: FragmentEditProfileBinding
    private val viewModel by lazy {
        ViewModelProvider(activity as UserProfileActivity).get(
            UserProfileViewModel::class.java
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BaseBottomSheetDialogBlank)
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
    ): View {
        isCancelable = true
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_edit_profile,
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
                initView(it)
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

        viewModel.userProfileUrl.observe(this) {
            binding.userPic.post {
                binding.userPic.setUserImageOrInitials(
                    url = it,
                    viewModel.userData.value?.name ?: getRandomName(),
                    28,
                    isRound = true
                )
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

        viewModel.isSaveClicked.observe(this) {
            dismiss()
        }

    }

    private fun addListeners() {
        binding.ivBack.setOnClickListener {
            dismiss()
        }

        binding.txtChangePicture.setOnClickListener {
            openChooser()
        }

        binding.btnSave.setOnClickListener {
            saveData()
        }
    }

    private fun saveData() {
        val newName = binding.editTxtName.text?.trim().toString()
        if (newName.isBlank()) {
            showToast("Please enter your full name")
            return
        }

        val dobStr = binding.editTxtDob.text?.trim().toString()
        if (dobStr.isBlank() || dobStr.split("/").size != 3) {
            showToast("Please enter your correct date of birth")
            return
        }

        val homeTown = binding.editTxtHometown.text?.trim().toString()
        if (homeTown.isBlank()) {
            showToast("Please enter your hometown")
            return
        }

        viewModel.saveProfileInfo(
            viewModel.userProfileUrl.value!!,
            newName,
            dobStr,
            homeTown,
            true
        )
    }

    private fun initView(userData: UserProfileResponse) {
        val resp = StringBuilder()
        userData.name?.split(" ")?.forEachIndexed { index, string ->
            if (index < 2) {
                resp.append(
                    string.lowercase(Locale.getDefault())
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
                    .append(" ")
            }
        }
        binding.editTxtName.setText(resp.trim())
        userData.dateOfBirth?.let {
            // val dateStr = DD_MM_YYYY.format(it).lowercase(Locale.getDefault())
            binding.editTxtDob.setText(it)
        }
        binding.editTxtHometown.setText(userData.hometown)
        binding.editTxtBatch.setText(userData.joinedOn)
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        binding.progressBar.visibility = View.GONE
    }

    private fun openChooser() {
        UserPicChooserFragment.showDialog(
            activity?.supportFragmentManager!!,
            viewModel.getUserProfileUrl().isNullOrBlank(),
            isFromRegistration = false
        )
    }

    companion object {
        @JvmStatic
        fun newInstance() = EditProfileFragment()
    }
}
