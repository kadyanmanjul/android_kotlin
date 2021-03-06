package com.joshtalks.joshskills.ui.voip.voip_rating

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.core.IS_PROFILE_FEATURE_ACTIVE
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.custom_ui.FullScreenProgressDialog
import com.joshtalks.joshskills.core.custom_ui.PointSnackbar
import com.joshtalks.joshskills.core.playSnackbarSound
import com.joshtalks.joshskills.core.setRoundImage
import com.joshtalks.joshskills.core.textDrawableBitmap
import com.joshtalks.joshskills.databinding.VoipCallFeedbackViewBinding
import com.joshtalks.joshskills.repository.server.Award
import com.joshtalks.joshskills.ui.practise.PracticeViewModel
import com.joshtalks.joshskills.ui.userprofile.ShowAwardFragment
import java.util.HashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val ARG_CALLER_IMAGE = "caller_image_url"
const val ARG_CALLER_NAME = "caller_name"
const val ARG_CHANNEL_NAME = "channel_name"
const val ARG_CALL_TIME = "call_time"
const val ARG_YOUR_NAME = "your_name"
const val ARG_YOUR_AGORA_ID = "your_agora_id"


class VoipCallFeedbackView : DialogFragment() {

    private lateinit var binding: VoipCallFeedbackViewBinding
    private var channelName: String = EMPTY
    private var yourAgoraId: Int = -1
    private var pointsString: String = EMPTY

    private val practiceViewModel: PracticeViewModel by lazy {
        ViewModelProvider(this).get(PracticeViewModel::class.java)
    }

    override fun onStart() {
        super.onStart()
        dialog?.apply {
            val width = AppObjectController.screenWidth * .85
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            window?.setLayout(width.toInt(), height)
            setCanceledOnTouchOutside(false)
            setCancelable(false)
            val lp: WindowManager.LayoutParams? = window?.attributes
            lp?.dimAmount = 0.85f
            window?.attributes = lp
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireActivity(), R.style.full_dialog) {
            override fun onBackPressed() {
                submitFeedback("BACK")
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.voip_call_feedback_view, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            //attributes.windowAnimations = R.style.DialogAnimation
        }
        return binding.root
    }

    private fun addObserver() {
        practiceViewModel.pointsSnackBarText.observe(
            this.viewLifecycleOwner,
            {
                if (it.pointsList.isNullOrEmpty().not()) {
                    /*RxBus2.publish(
                        SnackBarEvent(
                            it.pointsList!!.get(0),null
                        )
                    )*/
                    showSnackBar(binding.rootView, Snackbar.LENGTH_LONG, it.pointsList!!.get(0))
                }
            })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val msz = AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.VOIP_FEEDBACK_MESSAGE)

        arguments?.let {
            channelName = it.getString(ARG_CHANNEL_NAME) ?: EMPTY
            yourAgoraId = it.getInt(ARG_YOUR_AGORA_ID)
            val callerName = it.getString(ARG_CALLER_NAME) ?: EMPTY
            val yourName = it.getString(ARG_YOUR_NAME) ?: EMPTY
            binding.txtMessage.text = msz.replaceFirst("#", yourName).replace("##", callerName)

            binding.cImage.setImageResource(R.drawable.ic_call_placeholder)
            val image = it.getString(ARG_CALLER_IMAGE)
            if (image.isNullOrEmpty()) {
                binding.cImage.setImageBitmap(
                    callerName.textDrawableBitmap(
                        width = 96,
                        height = 96
                    )
                )
            } else {
                binding.cImage.setRoundImage(image)
            }

            val mTime = StringBuilder()
            val callTime = it.getLong(ARG_CALL_TIME)
            val second: Int = (callTime / 1000 % 60).toInt()
            val minute: Int = (callTime / (1000 * 60) % 60).toInt()
            if (minute > 0) {
                mTime.append(minute).append(getMinuteString(minute)).append("  ")
            }
            if (second > 0) {
                mTime.append(second).append(getSecondString(second))
            }
            binding.txtSpoke.text = getString(R.string.spoke_for_minute, mTime.toString())
            binding.txtBottom.text = getString(R.string.block_user_hint, callerName)

            addObserver()
            practiceViewModel.getPointsForVocabAndReading(null, channelName = channelName)
        }
    }
    private fun getMinuteString(min :Int): String {
        if (min>1){
            return " minutes "
        }
        return " minute "
    }
    private fun getSecondString(sec :Int): String {
        if (sec>1){
            return " seconds "
        }
        return " second "
    }

    fun submitFeedback(response: String) {
        FullScreenProgressDialog.showProgressBar(requireActivity())
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val requestParams: HashMap<String, String> = HashMap()
                requestParams["channel_name"] = channelName
                requestParams["agora_mentor_id"] = yourAgoraId.toString()
                requestParams["response"] = response
                val res = AppObjectController.p2pNetworkService.p2pCallFeedbackV2(requestParams)
                /*
                   if (res.pointsList.isNullOrEmpty().not()) {
                    PrefManager.put(SPEAKING_POINTS, res.pointsList?.get(0).toString())
                }
                if (res.awardMentorList.isNullOrEmpty().not()) {
                    showAward(res.awardMentorList!!)
                } else {
                    exitDialog()
                }*/
                delay(750)
                exitDialog()
            } catch (ex: Throwable) {
                delay(750)
                exitDialog()
            }
        }
    }

    private fun exitDialog() {
        FullScreenProgressDialog.hideProgressBar(requireActivity())
        //dismissAllowingStateLoss()
        val intent = Intent()
        intent.putExtra("points_list", pointsString)
        requireActivity().setResult(Activity.RESULT_OK, intent)
        requireActivity().finishAndRemoveTask()
    }

    fun showSnackBar(view: View, duration: Int, action_lable: String?) {
        if (PrefManager.getBoolValue(IS_PROFILE_FEATURE_ACTIVE)) {
            //SoundPoolManager.getInstance(AppObjectController.joshApplication).playSnackBarSound()
            PointSnackbar.make(view, duration, action_lable)?.show()
            playSnackbarSound(requireActivity())
        }
    }

    private fun showAward(awardList: List<Award>, isFromUserProfile: Boolean = false) {
        if (false) {
            ShowAwardFragment.showDialog(childFragmentManager, awardList, isFromUserProfile)
        }
    }

    companion object {
        @JvmStatic
        private fun newInstance(
            channelName: String?,
            callTime: Long,
            callerName: String?,
            callerImage: String?,
            yourName: String?,
            yourAgoraId: Int?
        ) =
            VoipCallFeedbackView().apply {
                arguments = Bundle().apply {
                    putString(ARG_CHANNEL_NAME, channelName)
                    putLong(ARG_CALL_TIME, callTime)
                    putString(ARG_CALLER_NAME, callerName)
                    putString(ARG_CALLER_IMAGE, callerImage)
                    putString(ARG_YOUR_NAME, yourName)
                    putInt(ARG_YOUR_AGORA_ID, yourAgoraId ?: -1)
                }
            }

        @JvmStatic
        fun showCallRatingDialog(
            fragmentManager: FragmentManager,
            channelName: String?,
            callTime: Long,
            callerName: String?,
            callerImage: String?,
            yourName: String?,
            yourAgoraId: Int?
        ) {
            val prev =
                fragmentManager.findFragmentByTag(VoipCallFeedbackView::class.java.name)
            if (prev != null) {
                return
            }
            newInstance(channelName, callTime, callerName, callerImage, yourName, yourAgoraId).show(
                fragmentManager,
                VoipCallFeedbackView::class.java.name
            )
        }
    }

}