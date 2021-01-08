package com.joshtalks.joshskills.ui.day_wise_course.reading.feedback

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.custom_ui.blurdialog.BlurDialogFragment
import com.joshtalks.joshskills.databinding.FragmentFeedbackPronBinding
import com.joshtalks.joshskills.repository.local.entity.practise.WrongWord
import com.joshtalks.joshskills.ui.groupchat.uikit.ExoAudioPlayer2
import timber.log.Timber


class ReadingResultFragment : BlurDialogFragment(), ExoAudioPlayer2.ProgressUpdateListener {
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
        val tableRow =
            View.inflate(requireContext(), R.layout.table_row, null) as TableRow
        val charTv = (tableRow.getChildAt(0) as TextView)
        val qualityTv = (tableRow.getChildAt(1) as TextView)

        var lp = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        charTv.layoutParams = lp

        val lp1 = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        qualityTv.layoutParams = lp1

        charTv.text = char
        qualityTv.text = quality

        if ("good".equals(quality, true)) {
            charTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
            qualityTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
        } else {
            charTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_f6))
            qualityTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_f6))
        }

        binding.tableLayout.addView(tableRow)
    }

    override fun onResume() {
        super.onResume()
        exoAudioManager?.setProgressUpdateListener(this)
    }

    override fun onPause() {
        super.onPause()
        exoAudioManager?.setProgressUpdateListener(null)
        exoAudioManager?.release()
    }


    fun teacherSpeak() {
        binding.audio1.playAnimation()
        teacherAudioUrl?.let {
            playAudio(it, word!!.teacherStartTime * 10, word!!.teacherEndTime * 10)
        }
    }

    fun userSpeak() {
        if (word != null && word!!.studentStartTime >= word!!.studentEndTime) {
            return
        }
        binding.audio2.playAnimation()
        userAudioUrl?.let {
            playAudio(it, word!!.studentStartTime * 10, word!!.studentEndTime * 10)
        }
    }

    private fun playAudio(url: String, startPos: Long, endPos: Long) {
            startTime = startPos
            endTime = endPos
        exoAudioManager?.play(url, seekDuration = startPos, delayProgress = 5)
    }

    override fun onProgressUpdate(progress: Long) {
        super.onProgressUpdate(progress)
        Timber.tag("Audio")
            .e("Start " + startTime + "  end" + endTime + "    progress  " + progress)
        if (progress >= endTime) {
            exoAudioManager?.onPause()
            exoAudioManager?.setProgressUpdateListener(null)
            binding.audio1.pauseAnimation()
            binding.audio1.progress = 0.0F
            binding.audio2.pauseAnimation()
            binding.audio2.progress = 0.0F
            return
        }
    }


    companion object {
        const val ARG_WORD_DETAILS = "word_detail"
        const val ARG_AUDIO_TEACHER = "audio_teacher"
        const val ARG_AUDIO_USER = "audio_user"


        @JvmStatic
        private fun newInstance(word: WrongWord, teacherAudioUrl: String?, userAudioUrl: String?) =
            ReadingResultFragment().apply {
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
                fragmentManager.findFragmentByTag(ReadingResultFragment::class.java.name)
            if (prev != null) {
                return
            }
            newInstance(word, teacherAudioUrl, userAudioUrl).show(
                fragmentManager,
                ReadingResultFragment::class.java.name
            )
        }
    }

}
