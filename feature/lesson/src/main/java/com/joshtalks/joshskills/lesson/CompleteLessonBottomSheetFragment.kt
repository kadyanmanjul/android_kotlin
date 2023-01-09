package com.joshtalks.joshskills.lesson

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.common.core.*
import com.joshtalks.joshskills.common.ui.chat.CHAT_ROOM_ID
import com.joshtalks.joshskills.common.ui.video_player.LAST_LESSON_INTERVAL
import com.joshtalks.joshskills.lesson.databinding.CompleteLessonDialogBinding

class CompleteLessonBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: CompleteLessonDialogBinding
    private val viewModel by lazy {
        ViewModelProvider(requireActivity())[LessonViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = CompleteLessonDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.saveImpression(Lesson_pop_up_shown)
        binding.cross.setOnClickListener {
            dismissAllowingStateLoss()
            viewModel.saveImpression(Lesson_pop_up_cancelled)
            val resultIntent = Intent()
            viewModel.lessonLiveData.value?.let {
                resultIntent.putExtra(CHAT_ROOM_ID, it.chatId)
                resultIntent.putExtra(LAST_LESSON_INTERVAL, it.interval)
                resultIntent.putExtra(LessonActivity.LAST_LESSON_STATUS, it.status?.name)
                resultIntent.putExtra(LESSON_NUMBER, it.lessonNo)
            }
            activity?.setResult(AppCompatActivity.RESULT_OK, resultIntent)
            activity?.finish()
        }

        binding.txtVLessonCompleteHeading.text = AppObjectController.getFirebaseRemoteConfig().getString(
            FirebaseRemoteConfigKey.LESSON_COMPLETE_HEADING_.plus(
                PrefManager.getStringValue(CURRENT_COURSE_ID).ifEmpty { DEFAULT_COURSE_ID })
        )

        val txtVBodySpeaking = AppObjectController.getFirebaseRemoteConfig().getString(
            FirebaseRemoteConfigKey.LESSON_COMPLETE_SPEAKING_.plus(
                PrefManager.getStringValue(CURRENT_COURSE_ID).ifEmpty { DEFAULT_COURSE_ID })
        )
        val txtVBodyGrammar = AppObjectController.getFirebaseRemoteConfig().getString(
            FirebaseRemoteConfigKey.LESSON_COMPLETE_GRAMMAR_.plus(
                PrefManager.getStringValue(CURRENT_COURSE_ID).ifEmpty { DEFAULT_COURSE_ID })
        )
        val txtVBodyVocab  = AppObjectController.getFirebaseRemoteConfig().getString(
            FirebaseRemoteConfigKey.LESSON_COMPLETE_VOCAB_.plus(
                PrefManager.getStringValue(CURRENT_COURSE_ID).ifEmpty { DEFAULT_COURSE_ID })
        )

        val txtVBodyReading = AppObjectController.getFirebaseRemoteConfig().getString(
            FirebaseRemoteConfigKey.LESSON_COMPLETE_READING_.plus(
                PrefManager.getStringValue(CURRENT_COURSE_ID).ifEmpty { DEFAULT_COURSE_ID })
        )

        //  creating models and adding into a list
        val speakingModel = CompleteLessonPopupModel(
            0,
            getString(R.string.lesson_popup_heading_speaking),
            txtVBodySpeaking,
            R.drawable.ic_vectorspeaking,
            R.color.decorative_five,
            R.color.decorative_four
        )
        val readingModel = CompleteLessonPopupModel(
            3,
            getString(R.string.lesson_popup_heading_reading),
            txtVBodyReading,
            R.drawable.ic_vector_practicereading,
            R.color.surface_success,
            R.color.success
        )
        val vocabModel = CompleteLessonPopupModel(
            2,
            getString(R.string.lesson_popup_heading_vocab),
            txtVBodyVocab,
            R.drawable.ic_vectorvocab,
            R.color.surface_critical,
            R.color.decorative_one
        )
        val grammarModel = CompleteLessonPopupModel(
            1,
            getString(R.string.lesson_popup_heading_grammar),
            txtVBodyGrammar,
            R.drawable.ic_vector_grammar,
            R.color.surface_information,
            R.color.primary_500
        )

        //  ordered according to the design
        val list : List<CompleteLessonPopupModel> = listOf(grammarModel,vocabModel,readingModel,speakingModel)

        binding.rvLessonPopup.adapter = LessonPopUpAdapter(list) {
            when (it) {
                0 -> {
                    viewModel.saveImpression(Lesson_pop_up_speaking_clicked)
                    viewModel.lessonCompletePopUpClick.postValue(0)
                }
                1 -> {
                    viewModel.saveImpression(Lesson_pop_up_grammar_clicked)
                    viewModel.lessonCompletePopUpClick.postValue(1)
                }
                2 -> {
                    viewModel.saveImpression(Lesson_pop_up_vocab_clicked)
                    viewModel.lessonCompletePopUpClick.postValue(2)

                }
                3 -> {
                    viewModel.saveImpression(Lesson_pop_up_reading_clicked)
                    viewModel.lessonCompletePopUpClick.postValue(3)
                }
            }
            dismissAllowingStateLoss()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): CompleteLessonBottomSheetFragment {
            val fragment = CompleteLessonBottomSheetFragment()
            fragment.isCancelable = false
            return fragment
        }
    }
}