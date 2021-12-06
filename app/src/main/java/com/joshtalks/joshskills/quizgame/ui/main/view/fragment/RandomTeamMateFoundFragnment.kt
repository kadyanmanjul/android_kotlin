package com.joshtalks.joshskills.quizgame.ui.main.view.fragment

import android.app.Dialog
import android.graphics.Color
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
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.card.MaterialCardView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.databinding.FragmentTeamMateFoundFragnmentBinding
import com.joshtalks.joshskills.databinding.RandomFragmentTeamMateFoundFragnmentBinding
import com.joshtalks.joshskills.quizgame.calling.WebRtcEngine
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.repository.FavouriteRepo
import com.joshtalks.joshskills.quizgame.ui.data.repository.TeamMateFoundRepo
import com.joshtalks.joshskills.quizgame.ui.main.adapter.ImageAdapter
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.*
import com.joshtalks.joshskills.quizgame.util.AudioManagerQuiz
import com.joshtalks.joshskills.quizgame.util.P2pRtc
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.voip.WebRtcService
import io.agora.rtc.RtcEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

//Channel Name = team_id
class RandomTeamMateFoundFragnment : Fragment() {
    private lateinit var binding:RandomFragmentTeamMateFoundFragnmentBinding
    private var randomTeamMateFoundViewModel:RandomTeamMateFoundViewModel?=null
    private var roomId:String?=null
    private var currentUserId:String?=null
    private var opponentUserImage:String?=null
    private var opponentUserName:String?=null
    private var engine: RtcEngine? = null
    private var flag=1
    private var flagSound =1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            roomId = it.getString("roomId")
            opponentUserImage = it.getString("opponentUserImage")
            opponentUserName = it.getString("opponentUserName")
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.random_fragment_team_mate_found_fragnment,
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
        setData()
        moveFragment()

        try {
            engine = P2pRtc().initEngine(requireActivity())
        }catch (ex:Exception){
            Timber.d(ex)
        }

        binding.imageMute.setOnClickListener {
           muteUnmute()
        }

        binding.imageSound.setOnClickListener {
            engine?.setDefaultAudioRoutetoSpeakerphone(true)
        }

        onBackPress()
    }

    private fun muteCall() {
        engine?.muteLocalAudioStream(true)
    }
    private fun unMuteCall() {
        engine?.muteLocalAudioStream(false)
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

    private fun setCurrentUserData() {
        binding.userName1.text = Mentor.getInstance().getUser()?.firstName
        val imageUrl=Mentor.getInstance().getUser()?.photo?.replace("\n","")
        binding.image.setUserImageOrInitials(imageUrl,Mentor.getInstance().getUser()?.firstName?:"",30,isRound = true)

//        activity?.let {
//            Glide.with(it)
//                .load(imageUrl)
//                .apply(RequestOptions.placeholderOf(R.drawable.ic_josh_course).error(R.drawable.ic_josh_course))
//                .into(binding.image)
    }
    private fun setData(){
        binding.txtQuiz1.text = opponentUserName +" is your team mate"
        val imageUrl=opponentUserImage?.replace("\n","")
        binding.image2.setUserImageOrInitials(imageUrl,opponentUserName?:"",30,isRound = true)

//        activity?.let {
//            Glide.with(it)
//                .load(imageUrl)
//                .apply(RequestOptions.placeholderOf(R.drawable.ic_josh_course).error(R.drawable.ic_josh_course))
//                .into(binding.image2)
//        }
        binding.userName2.text = opponentUserName
    }
    companion object {
        @JvmStatic
        fun newInstance(roomId:String?, opponentUserImage:String, opponentUserName:String?) =
            RandomTeamMateFoundFragnment().apply {
                arguments = Bundle().apply {
                    putString("roomId",roomId)
                    putString("opponentUserImage",opponentUserImage)
                    putString("opponentUserName",opponentUserName)
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
                    QuestionFragment.newInstance(roomId,startTime,"Random"),"SearchingOpponentTeam")
                ?.commit()
        }, 4000)
    }
    fun onBackPress() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                //CustomDialogQuiz(activity!!).show()
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
            dialog.dismiss()
            AudioManagerQuiz.audioRecording.stopPlaying()
            openChoiceScreen()
            engine?.leaveChannel()
            binding.callTime.stop()
        }
        noBtn.setOnClickListener {
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
    fun openChoiceScreen(){
        val fm = activity?.supportFragmentManager
        fm?.popBackStackImmediate()
        fm?.beginTransaction()
            ?.replace(R.id.container,
                ChoiceFragnment.newInstance(),"Question")
            ?.remove(this)
            ?.commit()
    }
}