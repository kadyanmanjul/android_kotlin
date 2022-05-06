package com.joshtalks.joshskills.ui.voip.voip_rating

import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.IS_FREE_TRIAL
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.CallRatingDialogBinding
import com.joshtalks.joshskills.quizgame.util.MyBounceInterpolator
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity

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
    private var checked=0
    private var count = 0

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
        if(PrefManager.getBoolValue(IS_FREE_TRIAL)) {
            Log.e("Ayaaz","is free trial")
            binding.cross.visibility = VISIBLE
        }
        else {
            Log.e("Ayaaz","is not in free trial")
            binding.cross.visibility = GONE
        }

        callerProfileUrl?.let {
            binding.cImage.setImage(callerProfileUrl!!)
        }
    }
    private fun addListner() {
       with(binding) {
           block.setOnClickListener {
               if (!isBlockSelected) {
                   block.setBackground(context?.getDrawable(R.drawable.block_button_round_stroke))
                   block.setTextColor(Color.WHITE)
               } else {
                   block.setBackground(context?.getDrawable(R.drawable.rectangle_with_grey_round_stroke))
                   block.setTextColor(Color.BLACK)
               }
               isBlockSelected = !isBlockSelected
           }
           ratingList.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { group, checkedId ->
//               AnimationUtils.loadAnimation(activity.get()!!, R.anim.blink)
//               UtilsQuiz.dipDown(group.findViewById<RadioButton>(checkedId), requireActivity())
//               AnimationUtils.loadAnimation(AppObjectController.joshApplication, R.anim.blink)
               val myAnim = AnimationUtils.loadAnimation(activity, R.anim.zoom_in)
               val interpolator = MyBounceInterpolator(0.8, 10.0)
               myAnim.interpolator = interpolator
//               myAnim.duration = 3000
//               myAnim.repeatCount = 1
               group.findViewById<RadioButton>(checkedId).startAnimation(myAnim)
//               animateIt(group.findViewById<RadioButton>(checkedId))
               if(checked>0){
                   group.findViewById<RadioButton>(checked).setTextColor(resources.getColor(R.color.black))
               }
               selectedRating=group.findViewById<RadioButton>(checkedId).text.toString().toInt()
               group.findViewById<RadioButton>(checkedId).setTextColor(resources.getColor(R.color.white))
               if(selectedRating<=6){
                   block.visibility=VISIBLE
                   submit.visibility= VISIBLE
               }else{
                   showToast("Your feedback has been successfully submitted")
                   group.findViewById<RadioButton>(checkedId).startAnimation(myAnim)
                   block.visibility= GONE
                   submit.visibility= GONE
                   vm.submitCallRatings(agoraCallId, selectedRating, callerMentorId)
                   dismiss()
                   activity?.finish()
               }
               checked =checkedId

           })

           submit.setOnClickListener {
               if(isBlockSelected){
                   vm.blockUser(callerMentorId)
               }
               if(selectedRating>0){
                   showToast("Your feedback has been successfully submitted")
                   vm.submitCallRatings(agoraCallId, selectedRating, callerMentorId)
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
        ): CallRatingsFragment {
            val fragment = CallRatingsFragment().apply {
                arguments = Bundle().apply {
                    putString(CALLER_NAME, callerName)
                    putInt(CALL_DURATION, callDuration)
                    putInt(AGORA_ID, agoraCallId)
                    putString(PROFILE_URL, callerProfileUrl)
                    putString(CALLER_MENTOR_ID, callerMentorId)
                    putString(AGORA_MENTOR_ID, agoraMentorId)
                }
            }
            fragment.isCancelable = false
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setOnKeyListener { dialogInterface, keyCode, keyEvent ->
                if(keyCode == KeyEvent.KEYCODE_BACK) {
                    if(count>=3) {
                        this@CallRatingsFragment.dismiss()
                        activity?.finish()
                    }
                    count+=1
                }
                true
            }
        }
    }

    fun animateIt(tv: TextView) {
        val a: ObjectAnimator = ObjectAnimator.ofInt(tv, "textColor", Color.WHITE, Color.WHITE)
        a.setInterpolator(LinearInterpolator())
        a.setDuration(500)
        a.setEvaluator(ArgbEvaluator())
        val t = AnimatorSet()
        t.play(a)
        t.start()
    }
}