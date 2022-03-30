package com.joshtalks.joshskills.ui.voip.share_call

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.text.HtmlCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.databinding.FragmentSharePreviewBinding

const val BUNDLE_ARG_MINUTES_TALKED = "BUNDLE_ARG_MINUTES_TALKED"
const val BUNDLE_ARG_RECEIVER_IMAGE = "BUNDLE_ARG_RECEIVER_IMAGE"
const val BUNDLE_ARG_CALLER_IMAGE = "BUNDLE_ARG_CALLER_IMAGE"
const val BUNDLE_ARG_CALLER_DETAILS = "BUNDLE_ARG_CALLER_DETAILS"
const val BUNDLE_ARG_RECEIVER_DETAILS = "BUNDLE_ARG_RECEIVER_DETAILS"
const val P2P_IMAGE_SHARE_TEXT = "P2P_IMAGE_SHARE_TEXT_"

class ShareScreenFragment: Fragment() {

    private lateinit var binding: FragmentSharePreviewBinding
    private val courseId = PrefManager.getStringValue(CURRENT_COURSE_ID, false, DEFAULT_COURSE_ID)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_share_preview, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {

            tvEnglishMinutes.text = HtmlCompat.fromHtml(getString(R.string.share_screen_heading,
                arguments?.getString(BUNDLE_ARG_MINUTES_TALKED)),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

            setImageForCallers(callerImage, arguments?.getString(BUNDLE_ARG_CALLER_IMAGE))
            setImageForCallers(receiverImage, arguments?.getString(BUNDLE_ARG_RECEIVER_IMAGE))

            tvCallerDetails.text = arguments?.getString(BUNDLE_ARG_CALLER_DETAILS)
            tvReceiverDetails.text = arguments?.getString(BUNDLE_ARG_RECEIVER_DETAILS)
            tvNewPerson.text = AppObjectController.getFirebaseRemoteConfig().getString(P2P_IMAGE_SHARE_TEXT + courseId)
        }
    }

    private fun setImageForCallers(image: ImageView, imageUrl: String?) {
        if (!imageUrl.isNullOrEmpty()) {
            image.setRoundImage(imageUrl)
        }
    }

    override fun onStart() {
        super.onStart()
    }

    fun getShareScreen() = binding.root


    companion object {
        fun setArguments(
            fragment: Fragment,
            minutesTalked: String,
            callerImage: String?,
            receiverImage: String?,
            callerDetails: String,
            receiverDetails: String
        ) {
            val bundle = Bundle().apply {
                putString(BUNDLE_ARG_MINUTES_TALKED, minutesTalked)
                putString(BUNDLE_ARG_CALLER_IMAGE, callerImage)
                putString(BUNDLE_ARG_RECEIVER_IMAGE, receiverImage)
                putString(BUNDLE_ARG_CALLER_DETAILS, callerDetails)
                putString(BUNDLE_ARG_RECEIVER_DETAILS, receiverDetails)
            }
            fragment.arguments = bundle
        }
    }
}