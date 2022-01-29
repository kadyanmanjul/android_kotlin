package com.joshtalks.joshskills.ui.userprofile

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.custom_ui.spinnerdatepicker.DatePickerDialog
import com.joshtalks.joshskills.core.custom_ui.spinnerdatepicker.SpinnerDatePickerDialogBuilder
import com.joshtalks.joshskills.databinding.FragmentEditProfileBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.SaveProfileClickedEvent
import com.joshtalks.joshskills.repository.server.UserProfileResponse
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.Calendar
import java.util.Locale

class EditProfileFragment : DialogFragment() {

    private var datePicker: DatePickerDialog? = null
    private var userDateOfBirth: String? = null
    private var compositeDisposable = CompositeDisposable()
    lateinit var binding: FragmentEditProfileBinding
    private val viewModel by lazy {
        ViewModelProvider(requireActivity()).get(
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
    ): View? {
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
        initDOBPicker()
        addObservers()
        addListeners()
    }

    private fun initDOBPicker() {
        val now = Calendar.getInstance()
        val minYear = now.get(Calendar.YEAR) - 99
        val maxYear = now.get(Calendar.YEAR) - MAX_YEAR
        datePicker = SpinnerDatePickerDialogBuilder()
            .context(requireActivity())
            .callback { _, year, monthOfYear, dayOfMonth ->
                val calendar = Calendar.getInstance()
                calendar.set(year, monthOfYear, dayOfMonth)
                binding.editTxtDob.setText(DD_MM_YYYY.format(calendar.time))
                userDateOfBirth = DATE_FORMATTER.format(calendar.time)
            }
            .spinnerTheme(R.style.DatePickerStyle)
            .showTitle(true)
            .showDaySpinner(true)
            .defaultDate(
                maxYear,
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
            )
            .minDate(
                minYear,
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
            )
            .maxDate(
                maxYear,
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
            )
            .build()
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

        compositeDisposable.add(
            RxBus2.listenWithoutDelay(SaveProfileClickedEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError { showToast("Something Went Wrong") }
                .subscribe {
                    dismiss()
                })
    }

    private fun addListeners() {
        binding.ivBack.setOnClickListener {
            hideKeyboard(requireActivity(),binding.editTxtHometown)
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
        val newName = binding.editTxtName.text?.trim()?.toString()
        if (newName.isNullOrBlank()) {
            showToast(getString(R.string.name_error_toast))
            return
        }

        if (userDateOfBirth.isNullOrBlank()) {
            showToast(getString(R.string.dob_error_toast))
            return
        }

        val homeTown = binding.editTxtHometown.text?.trim().toString()
        if (homeTown.isNullOrBlank()) {
            showToast(getString(R.string.hometown_error_toast))
            return
        }

        viewModel.saveProfileInfo(
            viewModel.userProfileUrl.value ?: EMPTY,
            newName,
            userDateOfBirth!!,
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
        userData.dateOfBirth?.let { dobStr ->
            try {
                val date = DD_MM_YYYY.parse(dobStr)
                userDateOfBirth = DATE_FORMATTER.format(date)
                binding.editTxtDob.setText(dobStr)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        binding.editTxtHometown.setText(userData.hometown)
        if (userData.hometown.isNullOrBlank()) {
            if (binding.editTxtHometown.requestFocus()) {
                getActivity()?.getWindow()
                    ?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            }
        }
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

    fun selectDateOfBirth() {
        datePicker?.show()
    }

    companion object {
        @JvmStatic
        fun newInstance() = EditProfileFragment()
    }
}
