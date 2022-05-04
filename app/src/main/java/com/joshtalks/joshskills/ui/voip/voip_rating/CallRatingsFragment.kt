package com.joshtalks.joshskills.ui.voip.voip_rating

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.databinding.CallRatingDialogBinding

class CallRatingsFragment :BottomSheetDialogFragment() {


    val CALLER_NAME = "caller_name"
    val CALL_DURATION = "call_duration"
    val AGORA_ID = "agora_id"
    val PROFILE_URL = "profile_url"
    val CALLER_MENTOR_ID = "caller_mentor_id"
    val AGORA_MENTOR_ID = "agora_mentor_id"
    lateinit var binding : CallRatingDialogBinding
    var isBlockSelected = false
    var selectedRating = 0
    var callerName = EMPTY
    var callDuration = 0
    var agoraCallId = 0
    var callerProfileUrl : String? = null
    var callerMentorId = EMPTY
    var agoraMentorId = EMPTY

    val vm :CallRatingsViewModel by lazy {
        ViewModelProvider(requireActivity())[CallRatingsViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = CallRatingDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        addListner()
        addObserver()
    }

    private fun initView() {
        val mArgs = arguments
        callerName = mArgs?.getString(CALLER_NAME).toString()
        callDuration= mArgs?.getInt(CALL_DURATION)!!
        agoraCallId= mArgs.getInt(AGORA_ID)
        callerProfileUrl= mArgs.getString(PROFILE_URL).toString()
        callerMentorId = mArgs.getString(CALLER_MENTOR_ID).toString()
        agoraMentorId = mArgs.getString(AGORA_MENTOR_ID).toString()

        Log.d("abcd", "callerMentorId " + callerMentorId )
        Log.d("abcd", "agoraMentorId " + agoraMentorId )

        binding.howCallTxt.text=getString(R.string.how_was_your_call_name,callerName)
        binding.callDurationText.text=getString(R.string.you_spoke_for_minutes,callDuration.toString())
        binding.block.text=getString(R.string.block_caller,callerName)
        if(PrefManager.getBoolValue(IS_FREE_TRIAL)) binding.cross.visibility = View.VISIBLE
        else binding.cross.visibility = View.GONE

        callerProfileUrl?.let {
            binding.cImage.setImage(callerProfileUrl!!)
        }
    }
    private fun addListner() {
       with(binding) {
           block.setOnClickListener {
               if (!isBlockSelected) {
                   block.setBackgroundColor(Color.BLACK)
                   block.setTextColor(Color.WHITE)
               } else {
                   block.setBackground(context?.getDrawable(R.drawable.rectangle_with_grey_round_stroke))
                   block.setTextColor(Color.BLACK)
               }
               isBlockSelected = !isBlockSelected
           }
           ratingList.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { group, checkedId ->
                selectedRating=group.findViewById<RadioButton>(checkedId).text.toString().toInt()
               showToast("Thanks for rating  $selectedRating")
               if(selectedRating<=6){
                   block.visibility=VISIBLE
                   submit.visibility= VISIBLE
               }else{
                   block.visibility=INVISIBLE
                   submit.visibility= INVISIBLE
                   vm.submitCallRatings(agoraCallId, selectedRating, agoraMentorId)
                   dismiss()
                   activity?.finish()
               }

           })

           submit.setOnClickListener {
               if(isBlockSelected){
                   vm.blockUser(callerMentorId)
               }
               if(selectedRating>0){
                   vm.submitCallRatings(agoraCallId, selectedRating, agoraMentorId)
               }

               dismiss()
               activity?.finish()
           }
           cross.setOnClickListener{
               dismiss()
               activity?.finish()
           }
       }
    }
    private fun addObserver() {

    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.finish()
    }

    companion object {
        @JvmStatic
        fun newInstance(
            callerName: String,
            callDuration: Int,
            agoraCallId: Int,
            callerProfileUrl: String?,
            callerMentorId : String,
            agoraMentorId : String
        ) =
            CallRatingsFragment().apply {
                arguments = Bundle().apply {
                    putString(CALLER_NAME, callerName)
                    putInt(CALL_DURATION,callDuration)
                    putInt(AGORA_ID,agoraCallId)
                    putString(PROFILE_URL,callerProfileUrl)
                    putString(CALLER_MENTOR_ID, callerMentorId)
                    putString(AGORA_MENTOR_ID, agoraMentorId)
                }
            }
    }
}