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
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.databinding.FragmentPreviousProfilePicsBinding
import com.joshtalks.joshskills.ui.userprofile.models.ProfilePicture
import com.joshtalks.joshskills.ui.userprofile.models.PreviousProfilePictures
import com.joshtalks.joshskills.ui.userprofile.adapters.PreviousPicsAdapter
import com.joshtalks.joshskills.ui.userprofile.viewmodel.UserProfileViewModel
import java.lang.Exception

class PreviousProfilePicsFragment : DialogFragment() {
    lateinit var binding: FragmentPreviousProfilePicsBinding
    lateinit var mentorId:String
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
        arguments?.let {
            it.getString(MENTOR_ID)?.let {
                mentorId = it
            }
        }
        changeDialogConfiguration()
        startTime = System.currentTimeMillis()
        viewModel.userProfileSectionImpression(mentorId,"PROFILE_PICTURE")
        viewModel.getPreviousProfilePics(mentorId)
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
            if(it==null||it.profilePictures.isNullOrEmpty()){
                dismiss()
            }else{
                initView(it)
            }
        }
        viewModel.sectionImpressionResponse.observe(this){
            impressionId=it.sectionImpressionId
        }

    }

    private fun addListeners() {
        binding.ivBack.setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
            dismiss()
        }
    }

    override fun onPause() {
        try {
            startTime = System.currentTimeMillis().minus(startTime).div(1000)
            if (startTime > 0 && impressionId?.isBlank()?.not() == true) {
                viewModel.engageUserProfileSectionTime(impressionId?: EMPTY, startTime.toString())
            }
            super.onPause()
        }catch (ex:Exception){}
    }

    private fun initView(previousProfilePics: PreviousProfilePictures) {
        var imagesUrls : Array<String> = Array(previousProfilePics.profilePictures.size){""}
        var count=0
        previousProfilePics?.profilePictures?.forEach{
            imagesUrls[count] = it.photoUrl
            count++
        }
        var imageIds : Array<String> = Array(previousProfilePics.profilePictures.size){""}
        count=0
        previousProfilePics?.profilePictures?.forEach{
            imageIds[count] = it.id.toString()
            count++
        }
        previousProfilePics?.profilePictures?.sortedBy { it.timestamp?.time }
            ?.let { picsList ->
                val recyclerView: RecyclerView = binding.rvPreviousPics
                val layoutManager = GridLayoutManager(context, 3)
                recyclerView.layoutManager = layoutManager
                recyclerView.setHasFixedSize(true)
                    recyclerView.adapter = PreviousPicsAdapter(
                    picsList,
                    object : PreviousPicsAdapter.OnPreviousPicClickListener {
                        override fun onPreviousPicClick(profilePicture: ProfilePicture,position:Int) {
                            ProfileImageShowFragment.newInstance(
                                mentorId,
                                true,
                                imagesUrls,
                                position,
                                imageIds
                            )
                                .show(activity!!.supportFragmentManager, "ImageShow")
                        }
                    },
                    mentorId)
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
        fun newInstance(mentorId: String) = PreviousProfilePicsFragment().apply {
            arguments =Bundle().apply {
                putString(MENTOR_ID,mentorId)
            }
        }
    }

}
