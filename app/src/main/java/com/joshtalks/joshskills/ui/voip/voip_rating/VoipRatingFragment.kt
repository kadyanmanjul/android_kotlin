package com.joshtalks.joshskills.ui.voip.voip_rating


import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.custom_ui.FullScreenProgressDialog
import com.joshtalks.joshskills.databinding.VoipRatingFragmentBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.voip.RequestVoipRating
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val LAST_VOIP_CALL_ID = "last_call_id"
const val LAST_VOIP_CALL_TIME = "last_call_time"

class VoipRatingFragment : DialogFragment() {
    private lateinit var binding: VoipRatingFragmentBinding
    private var plivoId: String = EMPTY
    private var lastCallTime: Long = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.full_dialog)
        arguments?.getString(LAST_VOIP_CALL_ID)?.run {
            plivoId = this
        }
        lastCallTime = arguments?.getLong(LAST_VOIP_CALL_TIME) ?: 0
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireActivity(), R.style.full_dialog) {
            override fun onBackPressed() {
                exitDialog()
            }
        }
    }


    override fun onStart() {
        super.onStart()
        dialog?.run {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            window?.setLayout(width, height)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.voip_rating_fragment,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvSpeaking.text = getString(R.string.speaking_time_msz, getTimeHhMm())
    }

    private fun getTimeHhMm(): String {
        val second: Int = (lastCallTime / 1000 % 60).toInt()
        val minute: Int = (lastCallTime / (1000 * 60) % 60).toInt()
        return String.format(
            "%02d:%02d", minute, second
        )
    }

    fun submitFeedback() {
        FullScreenProgressDialog.showProgressBar(requireActivity())
        val request = RequestVoipRating(
            Mentor.getInstance().getId(),
            plivoId,
            binding.prConfidence.getRatingPoint(),
            binding.prGrammar.getRatingPoint(),
            binding.prPronunciation.getRatingPoint(),
            binding.prEagernessToLearn.getRatingPoint(),
            binding.prWasTheRespectFullness.getRatingPoint()
        )
        requestForFeedback(request)
    }

    private fun requestForFeedback(request: RequestVoipRating) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppObjectController.commonNetworkService.feedbackVoipCall(request)
                FullScreenProgressDialog.hideProgressBar(requireActivity())
                exitDialog()
            } catch (ex: Throwable) {
                FullScreenProgressDialog.hideProgressBar(requireActivity())
                exitDialog()
                ex.showAppropriateMsg()
            }
        }
    }

    fun exitDialog() {
        val intent = Intent()
        requireActivity().setResult(Activity.RESULT_OK, intent)
        requireActivity().finish()
    }

    companion object {
        @JvmStatic
        fun newInstance(callId: String?, time: Int) = VoipRatingFragment()
            .apply {
                arguments = Bundle().apply {
                    putString(LAST_VOIP_CALL_ID, callId)
                    putLong(LAST_VOIP_CALL_TIME, time.toLong())
                }
            }
    }
}