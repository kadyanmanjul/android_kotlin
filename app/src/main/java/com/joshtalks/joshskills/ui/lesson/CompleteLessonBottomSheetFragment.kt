package com.joshtalks.joshskills.ui.lesson

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.databinding.CompleteLessonDialogBinding

class CompleteLessonBottomSheetFragment(val viewModel: LessonViewModel) : BottomSheetDialogFragment() {

    private lateinit var binding: CompleteLessonDialogBinding
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
            activity?.finish()
        }
        binding.txtVBodyGrammar.text = AppObjectController.getFirebaseRemoteConfig().getString(
            FirebaseRemoteConfigKey.LESSON_COMPLETE_GRAMMAR_.plus(
                PrefManager.getStringValue(CURRENT_COURSE_ID).ifEmpty { DEFAULT_COURSE_ID })
        )
        binding.txtVBodyVocab.text = AppObjectController.getFirebaseRemoteConfig().getString(
            FirebaseRemoteConfigKey.LESSON_COMPLETE_VOCAB_.plus(
                PrefManager.getStringValue(CURRENT_COURSE_ID).ifEmpty { DEFAULT_COURSE_ID })
        )
        binding.txtVBodySpeaking.text = AppObjectController.getFirebaseRemoteConfig().getString(
            FirebaseRemoteConfigKey.LESSON_COMPLETE_SPEAKING_.plus(
                PrefManager.getStringValue(CURRENT_COURSE_ID).ifEmpty { DEFAULT_COURSE_ID })
        )
        binding.txtVBodyReading.text = AppObjectController.getFirebaseRemoteConfig().getString(
            FirebaseRemoteConfigKey.LESSON_COMPLETE_READING_.plus(
                PrefManager.getStringValue(CURRENT_COURSE_ID).ifEmpty { DEFAULT_COURSE_ID })
        )
        binding.txtVLessonCompleteHeading.text = AppObjectController.getFirebaseRemoteConfig().getString(
            FirebaseRemoteConfigKey.LESSON_COMPLETE_HEADING_.plus(
                PrefManager.getStringValue(CURRENT_COURSE_ID).ifEmpty { DEFAULT_COURSE_ID })
        )
        binding.crdViewReading.setOnClickListener {
            viewModel.saveImpression(Lesson_pop_up_reading_clicked)
            viewModel.lessonCompletePopUpClick.postValue(3)
            dismissAllowingStateLoss()
        }
        binding.crdViewGrammar.setOnClickListener {
            viewModel.saveImpression(Lesson_pop_up_grammar_clicked)
            viewModel.lessonCompletePopUpClick.postValue(1)
            dismissAllowingStateLoss()
        }
        binding.crdViewVocab.setOnClickListener {
            viewModel.saveImpression(Lesson_pop_up_vocab_clicked)
            viewModel.lessonCompletePopUpClick.postValue(2)
            dismissAllowingStateLoss()
        }
        binding.crdViewSpeaking.setOnClickListener {
            viewModel.saveImpression(Lesson_pop_up_speaking_clicked)
            viewModel.lessonCompletePopUpClick.postValue(0)
            dismissAllowingStateLoss()
        }
    }

    companion object{
        @JvmStatic
        fun newInstance(viewModel: LessonViewModel):CompleteLessonBottomSheetFragment{
            val fragment = CompleteLessonBottomSheetFragment(viewModel)
            fragment.isCancelable = false
            return fragment
        }
    }
}