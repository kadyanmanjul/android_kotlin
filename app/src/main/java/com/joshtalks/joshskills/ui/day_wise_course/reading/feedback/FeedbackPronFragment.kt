package com.joshtalks.joshskills.ui.day_wise_course.reading.feedback

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshSkillExecutors
import com.joshtalks.joshskills.core.custom_ui.blurdialog.BlurDialogFragment
import com.joshtalks.joshskills.databinding.FragmentFeedbackPronBinding
import com.joshtalks.joshskills.repository.local.entity.practise.WrongWord
import com.joshtalks.joshskills.ui.groupchat.uikit.ExoAudioPlayer2

class FeedbackPronFragment : BlurDialogFragment(), ExoAudioPlayer2.ProgressUpdateListener {
    private var word: WrongWord? = null
    private lateinit var binding: FragmentFeedbackPronBinding
    private var exoAudioManager: ExoAudioPlayer2? = ExoAudioPlayer2.getInstance()
    private var teacherAudioUrl: String? = null
    private var userAudioUrl: String? = null
    private var startTime: Long = 0L
    private var endTime: Long = 0L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            word = it.getParcelable(ARG_WORD_DETAILS)
            teacherAudioUrl = it.getString(ARG_AUDIO_TEACHER)
            userAudioUrl = it.getString(ARG_AUDIO_USER)
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


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        exoAudioManager?.setProgressUpdateListener(this)

        word?.let {
            binding.wordTv.text = it.word
            it.phones?.forEach { phonetic -> addTableRow(phonetic.phone, phonetic.quality) }
        }
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

    private fun addTableRow(char: String, quality: String) {
        val tableRow: TableRow =
            View.inflate(requireContext(), R.layout.table_row, null) as TableRow
        val charTv = (tableRow.getChildAt(0) as TextView)
        val qualityTv = (tableRow.getChildAt(1) as TextView)

        charTv.text = char
        qualityTv.text = quality

        if ("good".equals(quality, true)) {
            charTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
            qualityTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
        } else {
            charTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_f6))
            qualityTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_f6))
        }

        val layoutParams = ConstraintLayout.LayoutParams(
            binding.root.layoutParams
        )
        layoutParams.startToEnd = R.id.word_tv
        tableRow.layoutParams = layoutParams
        binding.tableLayout.addView(tableRow)
    }

    override fun onPause() {
        super.onPause()
        exoAudioManager?.release()
    }


    fun teacherSpeak() {
        teacherAudioUrl?.let {
            startTime = word!!.teacherStartTime * 1000
            endTime = word!!.teacherEndTime * 6000
            exoAudioManager?.play(it, seekDuration = 1000)
        }
    }

    fun userSpeak() {
        userAudioUrl?.let {
            startTime = word!!.studentStartTime * 1000
            endTime = word!!.studentEndTime * 1000
            exoAudioManager?.play(it, seekDuration = startTime)
        }
    }

    override fun onProgressUpdate(progress: Long) {
        super.onProgressUpdate(progress)
        Log.e("aaa", "aaaa$progress  " + "      " + startTime + "  " + endTime)
        JoshSkillExecutors.BOUNDED.execute {
            startTime += progress
            if (startTime >= endTime) {
                //   exoAudioManager?.onPause()
            }
        }
    }

    companion object {
        const val ARG_WORD_DETAILS = "word_detail"
        const val ARG_AUDIO_TEACHER = "audio_teacher"
        const val ARG_AUDIO_USER = "audio_user"


        @JvmStatic
        private fun newInstance(word: WrongWord, teacherAudioUrl: String?, userAudioUrl: String?) =
            FeedbackPronFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_WORD_DETAILS, word)
                    putString(ARG_AUDIO_TEACHER, teacherAudioUrl)
                    putString(ARG_AUDIO_USER, userAudioUrl)
                }
            }

        @JvmStatic
        fun showLanguageDialog(
            fragmentManager: FragmentManager,
            word: WrongWord,
            teacherAudioUrl: String?,
            userAudioUrl: String?
        ) {
            val prev =
                fragmentManager.findFragmentByTag(FeedbackPronFragment::class.java.name)
            if (prev != null) {
                return
            }
            newInstance(word, teacherAudioUrl, userAudioUrl).show(
                fragmentManager,
                FeedbackPronFragment::class.java.name
            )
        }
    }

}
