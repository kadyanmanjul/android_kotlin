package com.joshtalks.joshskills.ui.voip.extra


import android.app.Dialog
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

class VoipRatingFragment : DialogFragment() {
    private lateinit var binding: VoipRatingFragmentBinding
    private var plivoId: String = EMPTY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.full_dialog)
        arguments?.getString(LAST_VOIP_CALL_ID)?.run {
            plivoId = this
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireActivity(), R.style.full_dialog) {
            override fun onBackPressed() {
                requireActivity().finish()
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
    ): View? {

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

    fun close() {
        requireActivity().finishAndRemoveTask()
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
                val response = AppObjectController.commonNetworkService.feedbackVoipCall(request)
                FullScreenProgressDialog.hideProgressBar(requireActivity())
                requireActivity().finishAndRemoveTask()
            } catch (ex: Throwable) {
                FullScreenProgressDialog.hideProgressBar(requireActivity())
                ex.showAppropriateMsg()
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(callId: String?) = VoipRatingFragment()
            .apply {
                arguments = Bundle().apply {
                    putString(LAST_VOIP_CALL_ID, callId)
                }
            }
    }
}