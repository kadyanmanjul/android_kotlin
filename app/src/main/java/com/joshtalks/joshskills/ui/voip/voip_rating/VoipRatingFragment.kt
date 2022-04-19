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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.custom_ui.FullScreenProgressDialog
import com.joshtalks.joshskills.core.custom_ui.PointSnackbar
import com.joshtalks.joshskills.databinding.VoipRatingFragmentBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.userprofile.models.Award
import com.joshtalks.joshskills.repository.server.voip.RequestVoipRating
import com.joshtalks.joshskills.ui.practise.PracticeViewModel
import com.joshtalks.joshskills.ui.userprofile.fragments.ShowAwardFragment
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val LAST_VOIP_CALL_TIME = "last_call_time"
const val LAST_VOIP_CALL_CHANNEL_NAME = "last_call_channel_name"

class VoipRatingFragment : DialogFragment() {
    private lateinit var binding: VoipRatingFragmentBinding
    private var channelName: String = EMPTY
    private var pointsString: String = EMPTY
    private var lastCallTime: Long = 0

    private val practiceViewModel: PracticeViewModel by lazy {
        ViewModelProvider(this).get(PracticeViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.full_dialog)
        arguments?.getString(LAST_VOIP_CALL_CHANNEL_NAME)?.run {
            channelName = this
        }
        lastCallTime = arguments?.getLong(LAST_VOIP_CALL_TIME) ?: 0
    }

    private fun addObserver() {
        practiceViewModel.pointsSnackBarText.observe(
            this.viewLifecycleOwner,
            {
                if (it.pointsList.isNullOrEmpty().not()) {
                    showSnackBar(binding.rootView, Snackbar.LENGTH_LONG, it.pointsList!!.get(0))
                }
            }
        )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireActivity(), R.style.full_dialog) {
            override fun onBackPressed() {
                MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
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
        inflater: LayoutInflater,
        container: ViewGroup?,
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
        addObserver()
        practiceViewModel.getPointsForVocabAndReading(null, channelName = channelName)
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

    @Synchronized
    fun submitFeedback() {
        FullScreenProgressDialog.showProgressBar(requireActivity())
        val request = RequestVoipRating(
            Mentor.getInstance().getId(),
            channelName,
            binding.prConfidence.getRatingPoint(),
            binding.prGrammar.getRatingPoint(),
            binding.prPronunciation.getRatingPoint(),
            binding.prEagernessToLearn.getRatingPoint(),
            binding.prWasTheRespectFullness.getRatingPoint()
        )
        requestForFeedback(request)
    }

    private fun requestForFeedback(request: RequestVoipRating) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val res = AppObjectController.commonNetworkService.feedbackVoipCallAsync(request)
                if (res.isSuccessful && res.body() != null) {
                    if (res.body()?.awardMentorList.isNullOrEmpty().not()) {
                        showAward(
                            res.body()!!.awardMentorList!!
                        )
                    }
                    if (res.body()!!.pointsList.isNullOrEmpty().not()) {
                        // PrefManager.put(SPEAKING_POINTS, res.body()!!.pointsList?.get(0).toString())
                    }
                }
                FullScreenProgressDialog.hideProgressBar(requireActivity())
            } catch (ex: Throwable) {
                FullScreenProgressDialog.hideProgressBar(requireActivity())
                ex.showAppropriateMsg()
            }
            exitDialog()
        }
    }

    fun showSnackBar(view: View, duration: Int, action_lable: String?) {
        if (PrefManager.getBoolValue(IS_PROFILE_FEATURE_ACTIVE)) {
            // SoundPoolManager.getInstance(AppObjectController.joshApplication).playSnackBarSound()
            PointSnackbar.make(view, duration, action_lable)?.show()
            playSnackbarSound(requireActivity())
        }
    }

    fun showAward(awarList: List<Award>, isFromUserProfile: Boolean = false) {
        if (false) {
            // TODO add when awards functionality is over
            // if (PrefManager.getBoolValue(IS_PROFILE_FEATURE_ACTIVE)) {
            ShowAwardFragment.showDialog(
                childFragmentManager,
                awarList,
                isFromUserProfile
            )
        }
    }

    fun exitDialog() {
        val intent = Intent()
        // intent.putExtra("points_list",pointsString)
        intent.putExtra("points_list", pointsString)
        requireActivity().setResult(Activity.RESULT_OK, intent)
        requireActivity().finishAndRemoveTask()
    }

    companion object {
        @JvmStatic
        fun newInstance(channelName: String?, time: Long) = VoipRatingFragment()
            .apply {
                arguments = Bundle().apply {
                    putString(LAST_VOIP_CALL_CHANNEL_NAME, channelName)
                    putLong(LAST_VOIP_CALL_TIME, time)
                }
            }
    }
}
