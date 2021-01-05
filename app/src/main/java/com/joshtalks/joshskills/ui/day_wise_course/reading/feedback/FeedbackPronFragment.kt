package com.joshtalks.joshskills.ui.day_wise_course.reading.feedback

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.custom_ui.blurdialog.BlurDialogFragment
import com.joshtalks.joshskills.databinding.FragmentFeedbackPronBinding
import com.joshtalks.joshskills.ui.groupchat.uikit.ExoAudioPlayer2
import com.joshtalks.joshskills.ui.translation.ARG_WORD

class FeedbackPronFragment : BlurDialogFragment() {
    private var word: String? = null
    private lateinit var binding: FragmentFeedbackPronBinding
    private var exoAudioManager: ExoAudioPlayer2? = ExoAudioPlayer2.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            word = it.getString(ARG_WORD)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.apply {
            val width = AppObjectController.screenWidth * .85
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            window?.setLayout(width.toInt(), height)
            setCanceledOnTouchOutside(true)
            setCancelable(true)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_feedback_pron, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            attributes.windowAnimations = R.style.DialogAnimation
        }
        return binding.root
    }


    override fun getDownScaleFactor(): Float {
        return 4.0F
    }

    override fun getBlurRadius(): Int {
        return 20
    }

    override fun isRenderScriptEnable(): Boolean {
        return true
    }

    override fun isDebugEnable(): Boolean {
        return true
    }

    override fun isActionBarBlurred(): Boolean {
        return true
    }

    companion object {
        @JvmStatic
        private fun newInstance(word: String) =
            FeedbackPronFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_WORD, word)
                }
            }

        @JvmStatic
        fun showLanguageDialog(fragmentManager: FragmentManager, word: String) {
            val prev =
                fragmentManager.findFragmentByTag(FeedbackPronFragment::class.java.name)
            if (prev != null) {
                return
            }
            newInstance(word).show(fragmentManager, FeedbackPronFragment::class.java.name)
        }
    }
}
