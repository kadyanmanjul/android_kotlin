package com.joshtalks.joshskills.ui.voip.new_arch.ui.call_rating

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.IS_COURSE_BOUGHT
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.CallRatingDialogBinding
import com.joshtalks.joshskills.quizgame.util.MyBounceInterpolator
import com.joshtalks.joshskills.ui.call.data.local.VoipPref
import com.joshtalks.joshskills.ui.voip.new_arch.ui.feedback.FeedbackDialogFragment
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    var prevSelectedRating = 0

    var callerName = EMPTY
    var callDuration = 0
    var agoraCallId = 0
    var callerProfileUrl : String = ""
    var callerMentorId = EMPTY
    var agoraMentorId = EMPTY
    private var checked=0
    private var count = 0

    val vm : CallRatingsViewModel by lazy {
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

        binding.howCallTxt.text=getString(R.string.how_was_your_call_name,callerName)
        binding.callDurationText.text=getString(R.string.you_spoke_for_minutes,vm.getCallDurationString())
        binding.block.text=getString(R.string.block_caller,callerName)
        if(PrefManager.getBoolValue(IS_COURSE_BOUGHT).not()) {
            binding.cross.visibility = VISIBLE
        }
        else {
            binding.cross.visibility = GONE
        }
            binding.cImage.setImage(VoipPref.getLastProfileImage())

    }
    private fun addListner() {
       with(binding) {
           block.setOnClickListener {
               if (!isBlockSelected) {
                   if(binding.block.text.equals(getString(R.string.block_caller,callerName))){
                        selectChange("block")
                   }else{
                       selectChange("fpp")
                   }

               }else{
                   if(binding.block.text.equals(getString(R.string.block_caller,callerName))){
                       unSelectChange("block")

                   }else{
                       unSelectChange("fpp")
                   }
               }
           }
           ratingList.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { group, checkedId ->
               val myAnim = AnimationUtils.loadAnimation(activity, R.anim.zoom_in)
               val interpolator = MyBounceInterpolator(0.8, 10.0)
               myAnim.interpolator = interpolator
               group.findViewById<RadioButton>(checkedId).startAnimation(myAnim)
               if(checked>0){
                   group.findViewById<RadioButton>(checked).setTextColor(resources.getColor(R.color.black))
               }
               selectedRating=group.findViewById<RadioButton>(checkedId).text.toString().toInt()
               group.findViewById<RadioButton>(checkedId).setTextColor(resources.getColor(R.color.white))
               if(selectedRating<=6){
                   if(prevSelectedRating in 9..10)
                       unSelectChange("block")

                   block.text = getString(R.string.block_caller,callerName)
                   block.visibility= VISIBLE
                   submit.visibility= VISIBLE
               }else if(selectedRating in 7..8){
                   isBlockSelected = false
                   submitAutomatically(checkedId,group,myAnim)
               }
               else{
                   if(vm.ifDialogShow==1 && PrefManager.getBoolValue(IS_COURSE_BOUGHT)) {
                       if (prevSelectedRating in 0..6)
                           unSelectChange("fpp")
                       block.text = resources.getText(R.string.send_fpp_text)
                       block.visibility = VISIBLE
                       submit.visibility= VISIBLE
                   }else{
                       isBlockSelected = false
                       submitAutomatically(checkedId,group,myAnim)
                   }
               }
               prevSelectedRating = selectedRating
               checked =checkedId
           })

           submit.setOnClickListener {
               if(isBlockSelected){
                   if(binding.block.text.equals(getString(R.string.block_caller,callerName))){
                       vm.blockUser(VoipPref.getLastRemoteUserAgoraId().toString())
                   }else{
                       vm.sendFppRequest(VoipPref.getLastRemoteUserMentorId())
                   }
               }
               if (selectedRating >= 0){
                   CoroutineScope(Dispatchers.Main).launch{
                       showToast("Your feedback has been successfully submitted")
                   }
                   vm.submitCallRatings(VoipPref.getLastCallId().toString(), selectedRating, VoipPref.getLastRemoteUserAgoraId().toString())
               }

               closeSheet()
           }
           cross.setOnClickListener{
               closeSheet()
           }
       }
    }

    private fun submitAutomatically(checkedId: Int, group: RadioGroup, myAnim: Animation) {
        binding.block.visibility= GONE
        CoroutineScope(Dispatchers.Main).launch{
            showToast("Your feedback has been successfully submitted")
        }
        group.findViewById<RadioButton>(checkedId).startAnimation(myAnim)
        binding. submit.visibility= GONE
        vm.submitCallRatings(VoipPref.getLastCallId().toString(), selectedRating, VoipPref.getLastRemoteUserAgoraId().toString())
        closeSheet()
    }

    private fun selectChange(s: String) {
        if(s == "fpp" && vm.ifDialogShow==1  && PrefManager.getBoolValue(IS_COURSE_BOUGHT)){
            binding.block.chipStrokeColor = AppCompatResources.getColorStateList(requireContext(), R.color.colorPrimary)
            binding.block.chipBackgroundColor = AppCompatResources.getColorStateList(requireContext(), R.color.white)
            binding.block.setTextColor(resources.getColor(R.color.colorPrimary))
        }else{
            binding.block.chipStrokeColor = AppCompatResources.getColorStateList(requireContext(), R.color.pitch_black)
            binding.block.chipBackgroundColor = AppCompatResources.getColorStateList(requireContext(), R.color.pitch_black)
            binding.block.setTextColor(Color.WHITE)
        }
        isBlockSelected = true

    }
    private fun unSelectChange(s: String) {
        if(s=="fpp"&& vm.ifDialogShow==1 && PrefManager.getBoolValue(IS_COURSE_BOUGHT)){
            binding.block.chipStrokeColor = AppCompatResources.getColorStateList(requireContext(), R.color.pitch_black)
            binding.block.setTextColor(resources.getColor(R.color.pitch_black))
            binding.block.setTextColor(Color.BLACK)
            binding.block.background = AppCompatResources.getDrawable(requireContext(),R.drawable.rectangle_with_grey_round_stroke)
            binding.block.chipBackgroundColor = AppCompatResources.getColorStateList(requireContext(), R.color.white)
        }else{
            binding.block.chipStrokeColor = AppCompatResources.getColorStateList(requireContext(), R.color.pitch_black)
            binding.block.background = AppCompatResources.getDrawable(requireContext(),R.drawable.rectangle_with_grey_round_stroke)
            binding.block.chipBackgroundColor = AppCompatResources.getColorStateList(requireContext(), R.color.white)
            binding.block.setTextColor(Color.BLACK)
        }
        isBlockSelected = false
    }

    private fun closeSheet(){
        if(vm.ifDialogShow==0){
            showFeedBackDialog()
            dismiss()
        }else{
            dismiss()
        }
    }

    private fun showFeedBackDialog() {
        val function = fun() {}
        FeedbackDialogFragment.newInstance(function)
            .show(requireActivity().supportFragmentManager, "FeedBackDialogFragment")
    }

    private fun addObserver() {}

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
                        closeSheet()
                    }
                    count+=1
                }
                true
            }
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        val ft = manager.beginTransaction()
        ft.add(this, tag)
        ft.commitAllowingStateLoss()
    }
    private fun CircleImageView.setImage(url: String) {
        val requestOptions = RequestOptions().placeholder(R.drawable.ic_call_placeholder)
            .error(R.drawable.ic_call_placeholder)
            .format(DecodeFormat.PREFER_RGB_565)
            .disallowHardwareConfig().dontAnimate().encodeQuality(75)
        Glide.with(this)
            .load(url)
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .apply(requestOptions)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(this)
    }
}