package com.joshtalks.joshskills.quizgame.ui.main.view.fragment

import android.graphics.Color
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.databinding.RandomFragmentTeamMateFoundFragnmentBinding
import com.joshtalks.joshskills.quizgame.ui.data.model.SaveCallDuration
import com.joshtalks.joshskills.quizgame.ui.data.model.SaveCallDurationRoomData
import com.joshtalks.joshskills.quizgame.ui.data.network.GameFirebaseDatabase
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.RandomTeamMateFoundViewModelGame
import com.joshtalks.joshskills.quizgame.ui.main.viewmodel.RandomTeamMateViewModelFactory
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

class RandomTeamMateFoundFragment : Fragment(), GameFirebaseDatabase.OnTimeChange {
    private lateinit var binding: RandomFragmentTeamMateFoundFragnmentBinding

    private var randomTeamMateFoundViewModel: RandomTeamMateFoundViewModelGame? = null

    private var roomId: String? = null
    private var currentUserId: String? = null
    private var opponentUserImage: String? = null
    private var opponentUserName: String? = null
    private var engine: RtcEngine? = null
    private var flag = 1
    private var flagSound = 1
    private var time: Long = 0
    private var currentUserTeamId: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentUserId = Mentor.getInstance().getId()
        arguments?.let {
            roomId = it.getString("roomId")
            opponentUserImage = it.getString(OPPONENT_USER_IMAGE)
            opponentUserName = it.getString(OPPONENT_USER_NAME)
            currentUserTeamId = it.getString(CURRENT_USER_TEAM_ID)
            time = it.getLong(TIME_DATA)
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
        PrefManager.put(USER_MUTE_OR_NOT, false)
        setCurrentUserData()
        GameFirebaseDatabase().getRoomTime(roomId ?: EMPTY, this)
        setData()
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

    fun setUpViewModel() {
        val factory = activity?.application?.let { RandomTeamMateViewModelFactory(it) }
        randomTeamMateFoundViewModel = factory?.let {
            ViewModelProvider(
                this,
                it
            ).get(RandomTeamMateFoundViewModelGame::class.java)
        }
    }

    fun switchAudioMode() {
        updateStatusLabel(binding.imageSound, P2pRtc().getSpeaker())
        P2pRtc().switchAudioSpeaker()
        requireActivity().volumeControlStream = AudioManager.STREAM_VOICE_CALL
    }

    private fun updateStatusLabel(view: AppCompatImageButton, enable: Boolean) {
        if (enable) {
            view.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.dis_color_10f)
            view.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.white)
        } else {
            view.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.white)
            view.imageTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.blue33)
        }
    }

    private fun muteUnmute() {
        if (flag == 0) {
            flag = 1
            unMuteCall()
            PrefManager.put(USER_MUTE_OR_NOT, false)
            binding.imageMute.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.blue33)

            binding.imageMute.imageTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.white)

        } else {
            flag = 0
            muteCall()
            PrefManager.put(USER_MUTE_OR_NOT, true)
            binding.imageMute.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.white)

            binding.imageMute.imageTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.grey_61)
        }
    }

    private fun setCurrentUserData() {
        binding.userName1.text = Mentor.getInstance().getUser()?.firstName
        val imageUrl = Mentor.getInstance().getUser()?.photo?.replace("\n", EMPTY)
        binding.image.setUserImageOrInitials(
            imageUrl,
            Mentor.getInstance().getUser()?.firstName ?: EMPTY,
            30,
            isRound = true
        )
    }

    private fun setData() {
        binding.txtQuiz1.text = UtilsQuiz.getSplitName(opponentUserName) + " is your team mate"
        val imageUrl = opponentUserImage?.replace("\n", EMPTY)
        binding.image2.setUserImageOrInitials(imageUrl, opponentUserName ?: EMPTY, 30, isRound = true)

        binding.userName2.text = UtilsQuiz.getSplitName(opponentUserName)
    }

    companion object {
        @JvmStatic
        fun newInstance(
            roomId: String?,
            opponentUserImage: String,
            opponentUserName: String?,
            currentUserTeamId: String,
            time: Long
        ) =
            RandomTeamMateFoundFragment().apply {
                arguments = Bundle().apply {
                    putString("roomId", roomId)
                    putString(OPPONENT_USER_IMAGE, opponentUserImage)
                    putString(OPPONENT_USER_NAME, opponentUserName)
                    putString(CURRENT_USER_TEAM_ID, currentUserTeamId)
                    putLong(TIME_DATA, time)
                }
            }
    }

    private fun moveFragment(time: Long) {
        val cdt = (time - (System.currentTimeMillis() / 1000 % 60)) % 60

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            val startTime = (SystemClock.elapsedRealtime() - binding.callTime.base).toString()
            val fm = activity?.supportFragmentManager
            fm?.beginTransaction()
                ?.replace(
                    R.id.container,
                    QuestionFragment.newInstance(roomId, startTime, RANDOM),
                    "SearchingOpponentTeam"
                )
                ?.commit()
        }, (cdt - 15) * 1000)
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

    private fun speakerOnOff() {
        if (flagSound == 0) {
            flagSound = 1
            engine?.setDefaultAudioRoutetoSpeakerphone(false)


            binding.imageSound.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.blue33)

            binding.imageSound.imageTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.white)

        } else {
            flagSound = 0
            engine?.setDefaultAudioRoutetoSpeakerphone(true)
            binding.imageSound.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.white)

            binding.imageSound.imageTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.grey_61)
        }
    }

    fun positiveBtnAction() {
        val startTime: String = (SystemClock.elapsedRealtime() - binding.callTime.base).toString()
        randomTeamMateFoundViewModel?.saveCallDuration(
            SaveCallDuration(
                currentUserTeamId ?: EMPTY,
                startTime.toInt().div(1000).toString(),
                currentUserId ?: EMPTY
            )
        )

        randomTeamMateFoundViewModel?.getClearRadius(
            SaveCallDurationRoomData(
                roomId ?: EMPTY,
                currentUserId ?: EMPTY,
                currentUserTeamId ?: EMPTY,
                startTime.toInt().div(1000).toString()
            )
        )

        activity?.let {
            randomTeamMateFoundViewModel?.clearRadius?.observe(it, {
                if (it.message == DATA_DELETED_SUCCESSFULLY_FROM_FIREBASE_AND_RADIUS) {
                    activity?.let {
                        randomTeamMateFoundViewModel?.saveCallDuration?.observe(it, {
                            if (it.message == CALL_DURATION_RESPONSE) {
                                val points = it.points
                                if (points.toInt() >= 1){
                                    lifecycleScope.launch(Dispatchers.Main) {
                                        UtilsQuiz.showSnackBar(
                                            binding.container,
                                            Snackbar.LENGTH_SHORT,
                                            "You earned +$points for speaking in English"
                                        )
                                    }
                                }
                                AudioManagerQuiz.audioRecording.stopPlaying()
                                engine?.leaveChannel()
                                binding.callTime.stop()
                                openChoiceScreen()
                            }
                        })
                    }
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
                    binding.callTime.stop()
                    PrefManager.put(USER_LEAVE_THE_GAME, true)
                    binding.userName2.alpha = 0.5f
                    binding.shadowImg2.visibility = View.VISIBLE
                }
            } catch (ex: Exception) {
                Timber.d(ex)
            }
        }

        override fun onSpeakerOff() {
            super.onSpeakerOff()
            AppObjectController.uiHandler.post {
                updateStatusLabel(binding.imageSound, enable = true)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PrefManager.put(USER_MUTE_OR_NOT, false)
    }

    override fun onTimeChangeMethod(time: Long) {
        moveFragment(time)
    }
}