package com.joshtalks.joshskills.ui.voip.new_arch.ui.feedback

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseDialogFragment
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.databinding.FragmentFeedbackDialogBinding
import com.joshtalks.joshskills.quizgame.util.UtilsQuiz.showSnackBar
import com.joshtalks.joshskills.repository.local.model.KFactor
import com.joshtalks.joshskills.ui.practise.PracticeViewModel
import com.joshtalks.joshskills.ui.voip.new_arch.ui.callbar.VoipPref
import com.joshtalks.joshskills.ui.voip.share_call.ShareWithFriendsActivity
import retrofit2.Response

class FeedbackDialogFragment(val function: () -> Unit) : BaseDialogFragment() {

    lateinit var binding: FragmentFeedbackDialogBinding
    val YES = "YES"
    val NO = "NO"
    val MAYBE = "MAYBE"
    val CLOSED = "CLOSED"

    private val practiceViewModel: PracticeViewModel by lazy {
        ViewModelProvider(this).get(PracticeViewModel::class.java)
    }

    val vm by lazy {
        ViewModelProvider(this)[FeedbackViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFeedbackDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        addObserver()
    }

    private fun addObserver() {
        vm.responseLiveData.observe(viewLifecycleOwner) {
            startShareActivity(it)
        }
        practiceViewModel.pointsSnackBarText.observe(
            this
        ) {
            if (it.pointsList.isNullOrEmpty().not()) {
                showSnackBar(
                    binding.container,
                    Snackbar.LENGTH_LONG,
                    it.pointsList?.get(0)
                )
                PrefManager.put(
                    LESSON_COMPLETE_SNACKBAR_TEXT_STRING,
                    it.pointsList!!.last(),
                    false
                )
            }
        }
    }

    private fun initView() {
        binding.vm = vm
        val callerName = vm.getCallerName()
        val duration = vm.getCallDurationString()
        val msz = AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.VOIP_FEEDBACK_MESSAGE_NEW)
        binding.txtSpoke.text = getString(R.string.spoke_for_minute, duration)
        binding.txtBottom.text = getString(R.string.block_user_hint, callerName, callerName)
        binding.txtMessage.text = msz.replaceFirst("#", callerName)
        binding.cImage.setImageResource(R.drawable.ic_call_placeholder)
        practiceViewModel.getPointsForVocabAndReading(
            null,
            channelName = VoipPref.getLastCallChannelName()
        )
    }

    fun closeDialog() {
        super.dismiss()
    }

    companion object {
        @JvmStatic
        fun newInstance(
            function: () -> Unit
        ) = FeedbackDialogFragment(function)
    }

    override fun show(manager: FragmentManager, tag: String?) {
        if (!manager.isDestroyed && !manager.isStateSaved) {
            super.show(manager, tag)
        }
    }

    fun startShareActivity(apiResponse: Response<KFactor>) {
        Log.d(TAG, "startShareActivity: ${vm.getDurationInMin()}")
        vm.getDurationInMin()
        if (apiResponse.isSuccessful &&
            apiResponse.code() in 201..203 && apiResponse.body()?.duration_filter!!
        ) {
            val body = apiResponse.body()!!

            val cState: String?
            val cCity: String?
            val rState: String?
            val rCity: String?

            if (VoipPref.getCurrentUserAgoraId() == body.caller.agora_mentor_id) {
                cState = body.caller.state
                cCity = body.caller.city
                rState = body.receiver.state
                rCity = body.receiver.city
            } else {
                cState = body.receiver.state
                cCity = body.receiver.city
                rState = body.caller.state
                rCity = body.caller.city
            }
            ShareWithFriendsActivity.startShareWithFriendsActivity(
                activity = activity as Activity,
                receiverName = vm.getCallerName(),
                receiverImage = vm.getProfileImage(),
                minutesTalked = vm.getDurationInMin(),
                callerState = cState,
                callerCity = cCity,
                receiverState = rState,
                receiverCity = rCity,
            )
        }
        closeDialog()
    }
}