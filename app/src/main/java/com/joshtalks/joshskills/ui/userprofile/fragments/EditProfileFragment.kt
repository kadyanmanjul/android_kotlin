package com.joshtalks.joshskills.ui.userprofile.fragments

import android.app.AlertDialog
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Html
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.NumberPicker
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.MAX_YEAR
import com.joshtalks.joshskills.core.DD_MM_YYYY
import com.joshtalks.joshskills.core.DATE_FORMATTER
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.core.getRandomName
import com.joshtalks.joshskills.core.hideKeyboard
import com.joshtalks.joshskills.core.custom_ui.spinnerdatepicker.DatePickerDialog
import com.joshtalks.joshskills.core.custom_ui.spinnerdatepicker.SpinnerDatePickerDialogBuilder
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentEditProfileBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.SaveProfileClickedEvent
import com.joshtalks.joshskills.ui.userprofile.FOR_BASIC_DETAILS
import com.joshtalks.joshskills.ui.userprofile.FOR_EDIT_SCREEN
import com.joshtalks.joshskills.ui.userprofile.FOR_REST
import com.joshtalks.joshskills.ui.userprofile.models.UpdateProfilePayload
import com.joshtalks.joshskills.ui.userprofile.models.UserProfileResponse
import com.joshtalks.joshskills.ui.userprofile.viewmodel.UserProfileViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_edit_profile.editTxtJoshTalk
import kotlinx.android.synthetic.main.fragment_edit_profile.editTxtCollegeName
import kotlinx.android.synthetic.main.fragment_edit_profile.editTxtHometown
import kotlinx.android.synthetic.main.fragment_edit_profile.editTxtOccupationPlace
import kotlinx.android.synthetic.main.fragment_edit_profile.editTxtCompletionDate
import kotlinx.android.synthetic.main.fragment_edit_profile.txtEducationName
import kotlinx.android.synthetic.main.fragment_edit_profile.editTxtName
import kotlinx.android.synthetic.main.fragment_edit_profile.editTxtFutureGoals
import kotlinx.android.synthetic.main.fragment_edit_profile.txtOccupationName
import java.util.Calendar
import java.util.Locale

const val CLICKED_FROM="CLICKED_FROM"
class EditProfileFragment : DialogFragment(){

    private var datePicker: DatePickerDialog? = null
    private var userDateOfBirth: String? = null
    private var compositeDisposable = CompositeDisposable()
    lateinit var binding: FragmentEditProfileBinding
    private var isBasicDetailsExpanded=false
    private var isEducationDetailsExpanded=false
    private var isOccupationDetailsExpanded=false
    private var clickedFrom:String= EMPTY
    private val viewModel by lazy {
        ViewModelProvider(requireActivity()).get(
            UserProfileViewModel::class.java
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { it ->
            it.getString(CLICKED_FROM)?.let {
                clickedFrom=it
            }
        }
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

    override fun onResume() {
        super.onResume()
        subscribeObserver()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }
     fun createDialogWithoutDateField() {
         val alertDialog: AlertDialog?
        val builder = AlertDialog.Builder(activity, R.style.YearPickerAlertDialogStyle)
        val inflater = requireActivity().layoutInflater

        val cal = Calendar.getInstance()

        val dialog = inflater.inflate(R.layout.year_picker_dialog, null)
        val monthPicker = dialog.findViewById(R.id.picker_month) as NumberPicker
        val yearPicker = dialog.findViewById(R.id.picker_year) as NumberPicker

        monthPicker.minValue = 1
        monthPicker.maxValue = 12
        monthPicker.value = cal.get(Calendar.MONTH) + 1

        val year = cal.get(Calendar.YEAR)
        yearPicker.minValue = 1900
        yearPicker.maxValue = 3500
        yearPicker.value = year
         var value= EMPTY
        builder.setView(dialog).setPositiveButton(Html.fromHtml("<font color='#107BE5'>Ok</font>")){ dialogInterface, which ->
            binding.editTxtCompletionDate.setText(yearPicker.value.toString())
            dialogInterface.cancel()
        }

        builder.setNegativeButton(Html.fromHtml("<font color='#107BE5'>Cancel</font>")){dialogInterface, which ->
            dialogInterface.cancel()
        }
         alertDialog = builder.create()
        alertDialog.setCancelable(true)
        alertDialog.show()
    }
    private fun subscribeObserver() {
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(SaveProfileClickedEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError { showToast(AppObjectController.joshApplication.getString(R.string.something_went_wrong)) }
                .subscribe {
                    dismiss()
                })
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
            this
        ) {
            initView(it)
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
    }

    private fun addListeners() {
        binding.ivBack.setOnClickListener {
            hideKeyboard(requireActivity(),binding.editTxtHometown)
            dismiss()
        }

        binding.txtChangePicture.setOnClickListener {
            openChooser()
        }
        binding.basicDetailsLayout.setOnClickListener {view->
            if (isBasicDetailsExpanded) {
                isBasicDetailsExpanded = false
                binding.arrowDownImg.setImageDrawable(drawableDown)
                binding.basicDetailsContainer.visibility = View.GONE
            }else{
                isBasicDetailsExpanded = true
                binding.arrowDownImg.setImageDrawable(drawableUp)
                binding.basicDetailsContainer.visibility = View.VISIBLE
            }
        }
        binding.educationDetailsLayout.setOnClickListener {view->
            if (isEducationDetailsExpanded) {
                isEducationDetailsExpanded = false
                binding.educationDownImg.setImageDrawable(drawableDown)
                binding.educationDetailsContainer.visibility = View.GONE
            }else{
                isEducationDetailsExpanded = true
                binding.educationDownImg.setImageDrawable(drawableUp)
                binding.educationDetailsContainer.visibility = View.VISIBLE
            }
        }
        binding.occupationDetailsLayout.setOnClickListener {view->
            if (isOccupationDetailsExpanded) {
                isOccupationDetailsExpanded = false
                binding.occupationDownImg.setImageDrawable(drawableDown)
                binding.occupationDetailsContainer.visibility = View.GONE
            }else{
                isOccupationDetailsExpanded = true
                binding.occupationDownImg.setImageDrawable(drawableUp)
                binding.occupationDetailsContainer.visibility = View.VISIBLE
            }
        }

        binding.ivSave.setOnClickListener {
            saveData()
        }
    }

    private fun saveData() {
        val newName = binding.editTxtName.text?.trim()?.toString()
        if (newName.isNullOrBlank()) {
            binding.seperator1.setBackgroundColor(resources.getColor(R.color.red))
            binding.editTxtName.setHintTextColor(resources.getColor(R.color.red))
            binding.basicDetailsContainer.visibility = View.VISIBLE
            isBasicDetailsExpanded = true
            binding.arrowDownImg.setImageDrawable(drawableUp)
            binding.editTxtName.error = getString(R.string.name_error_message)
            return
        }
        val educationText=binding.txtEducationName
        val collegeName=binding.editTxtCollegeName
        if(!binding.editTxtCompletionDate.text.isNullOrBlank() && educationText.text.isNullOrBlank() && collegeName.text.isNullOrBlank()){
            binding.txtEducationNameSeperator.setBackgroundColor(resources.getColor(R.color.red))
            educationText.setHintTextColor(resources.getColor(R.color.red))
            educationText.error = getString(R.string.degree_error_message)
            binding.editTxtCollegeNameSeperator.setBackgroundColor(resources.getColor(R.color.red))
            isEducationDetailsExpanded = true
            binding.arrowDownImg.setImageDrawable(drawableUp)
            binding.educationDownImg.setImageDrawable(drawableUp)
            binding.educationDetailsContainer.visibility = View.VISIBLE
            collegeName.setHintTextColor(resources.getColor(R.color.red))
            collegeName.error = getString(R.string.college_error_message)
            return

        }
        if (userDateOfBirth.isNullOrBlank()) {
            binding.seperator2.setBackgroundColor(resources.getColor(R.color.red))
            binding.editTxtDob.setHintTextColor(resources.getColor(R.color.red))
            binding.basicDetailsContainer.visibility = View.VISIBLE
            isBasicDetailsExpanded = true
            binding.arrowDownImg.setImageDrawable(drawableUp)
            binding.editTxtDob.error = getString(R.string.dob_error_message)
            return
        }

        val homeTownTxt = binding.editTxtHometown
        if (homeTownTxt.text.isNullOrBlank()) {
            binding.seperator3.setBackgroundColor(resources.getColor(R.color.red))
            homeTownTxt.setHintTextColor(resources.getColor(R.color.red))
            binding.basicDetailsContainer.visibility = View.VISIBLE
            isBasicDetailsExpanded = true
            binding.arrowDownImg.setImageDrawable(drawableUp)
            homeTownTxt.error = getString(R.string.hometown_error_message)
            return
        }
        if(editTxtJoshTalk.text.toString()!=null && !isYoutubeUrl(editTxtJoshTalk.text.toString())){
            binding.seperator5.setBackgroundColor(resources.getColor(R.color.red))
            editTxtJoshTalk.setHintTextColor(resources.getColor(R.color.red))
            binding.basicDetailsContainer.visibility = View.VISIBLE
            isBasicDetailsExpanded = true
            binding.arrowDownImg.setImageDrawable(drawableUp)
            editTxtJoshTalk.error = getString(R.string.invalid_url_message)
            return
        }
        var updateProfilePayload = UpdateProfilePayload()
        updateProfilePayload.apply {
            basicDetails?.apply{
                photoUrl= viewModel.getUserProfileUrl()
                firstName= editTxtName.text.toString().ifEmpty { null }
                dateOfBirth= userDateOfBirth
                homeTown= editTxtHometown.text.toString().ifEmpty { null }
                futureGoals= editTxtFutureGoals.text.toString().ifEmpty { null }
                favouriteJoshTalk= editTxtJoshTalk.text.toString().ifEmpty { null }
            }
            educationDetails?.apply {
                degree=txtEducationName.text.toString().ifEmpty { null }
                college=editTxtCollegeName.text.toString() .ifEmpty { null }
                year=editTxtCompletionDate.text.toString().ifEmpty { null }
            }
            occupationDetails?.apply {
                designation=txtOccupationName.text.toString().ifEmpty { null }
                company=editTxtOccupationPlace.text.toString().ifEmpty { null }
            }
        }
        viewModel.saveProfileInfo(updateProfilePayload, true)
    }
    fun isYoutubeUrl(youTubeURl: String): Boolean {
        val pattern:String = "^(http(s)?:\\/\\/)?((w){3}.)?youtu(be|.be)?(\\.com)?\\/.+"
        return youTubeURl.matches(Regex(pattern))
    }

    private fun initView(userData: UserProfileResponse) {
        when(clickedFrom){
            FOR_BASIC_DETAILS->{
                binding.basicDetailsContainer.visibility = View.VISIBLE
                isBasicDetailsExpanded=true
            }
            FOR_REST->{
                binding.educationDetailsContainer.visibility=View.VISIBLE
                binding.occupationDetailsContainer.visibility=View.VISIBLE
                isEducationDetailsExpanded=true
                isOccupationDetailsExpanded=true
            }
            FOR_EDIT_SCREEN->{

            }
        }
        val resp = StringBuilder()
        userData.name?.split(" ")?.forEachIndexed { index, string ->
            if (index < 2) {
                resp.append(
                    string.lowercase(Locale.getDefault())
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
                    .append(" ")
            }
        }
        with(binding) {
            editTxtName.setText(resp.trim())
            userData.dateOfBirth?.let { dobStr ->
                try {
                    val date = DD_MM_YYYY.parse(dobStr)
                    userDateOfBirth = DATE_FORMATTER.format(date)
                    binding.editTxtDob.setText(dobStr)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
            editTxtHometown.paintFlags = editTxtHometown.paintFlags or Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG
            editTxtHometown.setText(userData.hometown)
            if (userData.hometown.isNullOrBlank()) {
                if (binding.editTxtHometown.requestFocus()) {
                    activity?.window
                        ?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                }
            }
            editTxtFutureGoals.setText(userData.futureGoals)
            editTxtJoshTalk.setText(userData.favouriteJoshTalk)
            txtEducationName.setText(userData.educationDetails?.degree)
            editTxtCollegeName.setText(userData.educationDetails?.college)
            editTxtCompletionDate.setText(userData.educationDetails?.year)
            txtOccupationName.setText(userData.occupationDetails?.designation)
            editTxtOccupationPlace.setText(userData.occupationDetails?.company)
        }
    }
        private val drawableDown: Drawable? by lazy {
        ResourcesCompat.getDrawable(
            AppObjectController.joshApplication.resources,
            R.drawable.ic_down_img,
            null
        )
    }
    val drawableUp: Drawable? by lazy {
        ResourcesCompat.getDrawable(
            AppObjectController.joshApplication.resources,
            R.drawable.ic_up_img,
            null
        )
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
        fun newInstance(clickedFrom: String?) = EditProfileFragment().apply{
            arguments=Bundle().apply {
                putString(CLICKED_FROM,clickedFrom)
            }
        }
    }
}
