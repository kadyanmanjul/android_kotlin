package com.joshtalks.joshskills.quizgame.ui.main.view.fragment

import android.app.Dialog
import android.graphics.BlurMaskFilter
import android.graphics.Color
import android.graphics.MaskFilter
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.card.MaterialCardView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.databinding.FragmentTeamMateFoundFragnmentBinding
import com.joshtalks.joshskills.quizgame.ui.data.model.TeamDataDelete
import com.joshtalks.joshskills.quizgame.ui.data.model.UserDetails
import com.joshtalks.joshskills.quizgame.ui.data.repository.TeamMateFoundRepo
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.TeamMateFoundViewModel
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.TeamMateViewProviderFactory
import com.joshtalks.joshskills.quizgame.util.AudioManagerQuiz
import com.joshtalks.joshskills.quizgame.util.P2pRtc
import com.joshtalks.joshskills.repository.local.model.Mentor
import io.agora.rtc.RtcEngine
import timber.log.Timber


//Channel Name = team_id
class TeamMateFoundFragnment : Fragment(),P2pRtc.WebRtcEngineCallback {
    private lateinit var binding:FragmentTeamMateFoundFragnmentBinding
    private var userId:String?=null
    private var channelName:String?=null
    private var userDetails:UserDetails?=null
    private var teamMateFoundViewModel:TeamMateFoundViewModel?=null
    private var engine: RtcEngine? = null
    private var currentUserId :String?=null
    private var flag=1
    private var flagSound =1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString("userId")
            channelName = it.getString("channelName")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_team_mate_found_fragnment,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.clickHandler = this
        binding.callTime.visibility = View.VISIBLE
        binding.callTime.start()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentUserId = Mentor.getInstance().getUserId()

        setCurrentUserData()
        setUpData()
        moveFragment()


        try {
            engine = P2pRtc().getEngineObj()
            P2pRtc().addListener(callback)
        }catch (ex:Exception){
            Timber.d(ex)
        }
        binding.imageMute.setOnClickListener {
            muteUnmute()
        }
        binding.imageSound.setOnClickListener {
            speakerOnOff()
        }
        onBackPress()
    }

    private fun muteCall() {
        engine?.muteLocalAudioStream(true)
    }
    private fun unMuteCall() {
        engine?.muteLocalAudioStream(false)
    }
    private fun setCurrentUserData() {
        binding.userName1.text = Mentor.getInstance().getUser()?.firstName
        val imageUrl=Mentor.getInstance().getUser()?.photo?.replace("\n","")
        binding.image.setUserImageOrInitials(imageUrl,Mentor.getInstance().getUser()?.firstName?:"",30,isRound = true)

//        activity?.let {
//            Glide.with(it)
//                .load(imageUrl)
//                .apply(RequestOptions.placeholderOf(R.drawable.ic_josh_course).error(R.drawable.ic_josh_course))
//                .into(binding.image)
//        }
    }
    private fun setUpData(){
        val repository = TeamMateFoundRepo()
        val factory = activity?.application?.let { TeamMateViewProviderFactory(it, repository) }
        teamMateFoundViewModel = factory?.let {
            ViewModelProvider(this,
                it
            ).get(TeamMateFoundViewModel::class.java)
        }
        userId?.let { teamMateFoundViewModel?.getChannelData(it) }
        activity?.let {
            teamMateFoundViewModel?.userData?.observe(it, Observer {
                setData(it)
            })
        }
    }
    private fun setData(userDetails: UserDetails?){
        this.userDetails = userDetails
        binding.txtQuiz1.text = userDetails?.name +" is your team mate"
        val imageUrl=userDetails?.imageUrl?.replace("\n","")
        binding.image2.setUserImageOrInitials(imageUrl,userDetails?.name?:"",30,isRound = true)

//        activity?.let {
//            Glide.with(it)
//                .load(imageUrl)
//                .apply(RequestOptions.placeholderOf(R.drawable.ic_josh_course).error(R.drawable.ic_josh_course))
//                .into(binding.image2)
//        }
        binding.userName2.text = userDetails?.name
    }
    companion object {
        @JvmStatic
        fun newInstance(userId:String,channelName:String) =
            TeamMateFoundFragnment().apply {
                arguments = Bundle().apply {
                    putString("userId",userId)
                    putString("channelName",channelName)
                }
            }
    }
    private fun moveFragment(){
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            val startTime :String = (SystemClock.elapsedRealtime() - binding.callTime.base).toString()
            val fm = activity?.supportFragmentManager
            fm?.beginTransaction()
                ?.replace(R.id.container,
                    SearchingOpponentTeamFragment.newInstance(startTime,userDetails,channelName),"SearchingOpponentTeam")
                ?.remove(this)
                ?.commit()
            fm?.popBackStack()
        }, 4000)
    }
    private fun onBackPress() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showDialog()
            }
        })
    }
    private fun showDialog() {
        val dialog = Dialog(requireActivity())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        dialog.setCancelable(false)
        dialog.setContentView(R.layout.custom_dialog)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val yesBtn = dialog.findViewById<MaterialCardView>(R.id.btn_yes)
        val noBtn = dialog.findViewById<MaterialCardView>(R.id.btn_no)
        val btnCancel = dialog.findViewById<ImageView>(R.id.btn_cancel)

        yesBtn.setOnClickListener {
//            dialog.setCanceledOnTouchOutside(true);
//                   AudioManagerQuiz.audioRecording.stopPlaying()
//
            teamMateFoundViewModel?.deleteUserRadiusData(TeamDataDelete(channelName?:"",currentUserId?:""))
            activity?.let {
                teamMateFoundViewModel?.deleteData?.observe(it, Observer {
                    dialog.dismiss()
                    AudioManagerQuiz.audioRecording.stopPlaying()
                    openChoiceScreen()
                    engine?.leaveChannel()
                    binding.callTime.stop()
                })
            }
        }
        noBtn.setOnClickListener {
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
    private fun openChoiceScreen(){
        val fm = activity?.supportFragmentManager
        fm?.popBackStackImmediate()
        fm?.beginTransaction()
            ?.replace(R.id.container,
                ChoiceFragnment.newInstance(),"TeamMate")
            ?.remove(this)
            ?.commit()
    }
    private fun speakerOnOff(){
        if (flagSound == 0){
            flagSound = 1
            engine?.setDefaultAudioRoutetoSpeakerphone(false)


            binding.imageSound.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.blue33)

            binding.imageSound.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.white)

        }else{
            flagSound = 0
            engine?.setDefaultAudioRoutetoSpeakerphone(true)
            binding.imageSound.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.white)

            binding.imageSound.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.grey_61)
        }
    }
    private fun muteUnmute(){
        if (flag == 0){
            flag = 1
            unMuteCall()

            binding.imageMute.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.blue33)

            binding.imageMute.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.white)

        }else{
            flag = 0
            muteCall()
            binding.imageMute.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.white)

            binding.imageMute.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.grey_61)
        }
    }
    private var callback: P2pRtc.WebRtcEngineCallback = object : P2pRtc.WebRtcEngineCallback{
    override fun onPartnerLeave() {
            super.onPartnerLeave()
            try {
                requireActivity().runOnUiThread {
                    binding.userName2.alpha=0.5f
                    binding.shadowImg2.visibility = View.VISIBLE
                }
            }catch (ex:Exception){
                Log.d("error_res", "onPartnerLeave: "+ex.message?:"")
            }
        }
    }
}