package com.joshtalks.joshskills.quizgame.ui.main.view.fragment

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_LEAVE_THE_GAME
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.databinding.RandomFragmentTeamMateFoundFragnmentBinding
import com.joshtalks.joshskills.quizgame.ui.data.model.SaveCallDuration
import com.joshtalks.joshskills.quizgame.ui.data.model.SaveCallDurationRoomData
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.RandomTeamMateFoundViewModel
import com.joshtalks.joshskills.quizgame.util.*
import com.joshtalks.joshskills.repository.local.model.Mentor
import io.agora.rtc.RtcEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

//Channel Name = team_id
const val OPPONENT_USER_IMAGE: String = "opponentUserImage"
const val OPPONENT_USER_NAME: String = "opponentUserName"
const val CURRENT_USER_TEAM_ID: String = "current_user_team_id"

class RandomTeamMateFoundFragment : Fragment() {
    private lateinit var binding: RandomFragmentTeamMateFoundFragnmentBinding
    val randomTeamMateFoundViewModel by lazy {
        ViewModelProvider(requireActivity())[RandomTeamMateFoundViewModel::class.java]
    }
    private var roomId: String? = null
    private var currentUserId: String? = null
    private var opponentUserImage: String? = null
    private var opponentUserName: String? = null
    private var engine: RtcEngine? = null
    private var flag = 1
    private var flagSound = 1
    private var currentUserTeamId: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            roomId = it.getString("roomId")
            opponentUserImage = it.getString(OPPONENT_USER_IMAGE)
            opponentUserName = it.getString(OPPONENT_USER_NAME)
            currentUserTeamId = it.getString(CURRENT_USER_TEAM_ID)
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
        binding.container.setBackgroundColor(Color.WHITE)
        currentUserId = Mentor.getInstance().getUserId()
        setCurrentUserData()
        setData()
        moveFragment()
        if (PrefManager.getBoolValue(USER_LEAVE_THE_GAME)) {
            binding.userName2.alpha = 0.5f
            binding.shadowImg2.visibility = View.VISIBLE
        }
        try {
            engine = P2pRtc().getEngineObj()
            P2pRtc().addListener(callback)
        } catch (ex: Exception) {
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

    private fun muteUnmute() {
        if (flag == 0) {
            flag = 1
            unMuteCall()

            binding.imageMute.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.blue33)

            binding.imageMute.imageTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.white)

        } else {
            flag = 0
            muteCall()
            binding.imageMute.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.white)

            binding.imageMute.imageTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.grey_61)
        }
    }

    private fun setCurrentUserData() {
        binding.userName1.text = Mentor.getInstance().getUser()?.firstName
        val imageUrl = Mentor.getInstance().getUser()?.photo?.replace("\n", "")
        binding.image.setUserImageOrInitials(
            imageUrl,
            Mentor.getInstance().getUser()?.firstName ?: "",
            30,
            isRound = true
        )
    }

    private fun setData() {
        binding.txtQuiz1.text = opponentUserName + " is your team mate"
        val imageUrl = opponentUserImage?.replace("\n", "")
        binding.image2.setUserImageOrInitials(imageUrl, opponentUserName ?: "", 30, isRound = true)

        binding.userName2.text = opponentUserName
    }

    companion object {
        @JvmStatic
        fun newInstance(
            roomId: String?,
            opponentUserImage: String,
            opponentUserName: String?,
            currentUserTeamId: String
        ) =
            RandomTeamMateFoundFragment().apply {
                arguments = Bundle().apply {
                    putString("roomId", roomId)
                    putString(OPPONENT_USER_IMAGE, opponentUserImage)
                    putString(OPPONENT_USER_NAME, opponentUserName)
                    putString(CURRENT_USER_TEAM_ID, currentUserTeamId)
                }
            }
    }

    private fun moveFragment() {
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            val startTime: String =
                (SystemClock.elapsedRealtime() - binding.callTime.base).toString()
            val fm = activity?.supportFragmentManager
            fm?.beginTransaction()
                ?.replace(
                    R.id.container,
                    QuestionFragment.newInstance(roomId, startTime, RANDOM),
                    "SearchingOpponentTeam"
                )
                ?.commit()
        }, 4000)
    }

    fun onBackPress() {
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    CustomDialogQuiz(requireActivity()).showDialog(::positiveBtnAction)
                }
            })
    }

    fun positiveBtnAction() {
        val startTime: String = (SystemClock.elapsedRealtime() - binding.callTime.base).toString()
        randomTeamMateFoundViewModel.saveCallDuration(
            SaveCallDuration(
                currentUserTeamId ?: "",
                startTime.toInt().div(1000).toString(),
                currentUserId ?: ""
            )
        )

        randomTeamMateFoundViewModel.getClearRadius(
            SaveCallDurationRoomData(
                roomId ?: "",
                currentUserId ?: "",
                currentUserTeamId ?: "",
                startTime.toInt().div(1000).toString()
            )
        )

        activity?.let {
            randomTeamMateFoundViewModel.saveCallDuration.observe(it, Observer {
                if (it.message == CALL_DURATION_RESPONSE) {
                    val points = it.points
                    lifecycleScope.launch(Dispatchers.Main) {
                        UtilsQuiz.showSnackBar(
                            binding.container,
                            Snackbar.LENGTH_SHORT,
                            "You earned +$points for speaking in English"
                        )
                    }
                    AudioManagerQuiz.audioRecording.stopPlaying()
                    openChoiceScreen()
                    engine?.leaveChannel()
                    binding.callTime.stop()
                }
            })
        }
    }

    fun openChoiceScreen() {
        val fm = activity?.supportFragmentManager
        fm?.popBackStackImmediate()
        fm?.beginTransaction()
            ?.replace(
                R.id.container,
                ChoiceFragment.newInstance(), "Question"
            )
            ?.remove(this)
            ?.commit()
    }

    private var callback: P2pRtc.WebRtcEngineCallback = object : P2pRtc.WebRtcEngineCallback {
        override fun onPartnerLeave() {
            super.onPartnerLeave()
            try {
                requireActivity().runOnUiThread {
                    PrefManager.put(USER_LEAVE_THE_GAME, true)
                    binding.userName2.alpha = 0.5f
                    binding.shadowImg2.visibility = View.VISIBLE
                }
            } catch (ex: Exception) {
                Timber.d(ex)
            }
        }
    }
}