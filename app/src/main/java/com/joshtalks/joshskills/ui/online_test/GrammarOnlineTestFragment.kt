package com.joshtalks.joshskills.ui.online_test

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshFragment
import com.joshtalks.joshskills.core.ONLINE_TEST_LAST_LESSON_ATTEMPTED
import com.joshtalks.joshskills.core.ONLINE_TEST_LAST_LESSON_COMPLETED
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.databinding.FragmentGrammarOnlineTestBinding
import com.joshtalks.joshskills.ui.lesson.LessonActivityListener

class GrammarOnlineTestFragment : CoreJoshFragment(), OnlineTestFragment.OnlineTestInterface {
    private lateinit var binding: FragmentGrammarOnlineTestBinding
    private var lessonActivityListener: LessonActivityListener? = null
    private var lessonNumber: Int = -1

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is LessonActivityListener)
            lessonActivityListener = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        arguments?.let {
            lessonNumber = it.getInt(CURRENT_LESSON_NUMBER, -1)
        }
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_grammar_online_test,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        when {
            (PrefManager.getIntValue(ONLINE_TEST_LAST_LESSON_COMPLETED, defValue = 1)
                .plus(1) == lessonNumber) -> {
                binding.startTestContainer.visibility = View.VISIBLE
                binding.testCompletedContainer.visibility = View.GONE
                if (PrefManager.getIntValue(
                        ONLINE_TEST_LAST_LESSON_ATTEMPTED,
                        defValue = 1
                    ) == lessonNumber
                ) {
                    binding.description.text = getString(R.string.grammar_continue_test_text)
                    binding.startBtn.text = getString(R.string.grammar_btn_text_continue)
                }
            }
            (PrefManager.getIntValue(
                ONLINE_TEST_LAST_LESSON_COMPLETED,
                defValue = 1
            ) >= lessonNumber) -> {
                binding.startTestContainer.visibility = View.GONE
                binding.testCompletedContainer.visibility = View.VISIBLE
                completeGrammarCardLogic()
            }
            else -> {
                binding.startTestContainer.visibility = View.VISIBLE
                binding.testCompletedContainer.visibility = View.GONE
                binding.startBtn.isEnabled = false
                binding.startBtn.isClickable = false
                binding.startBtn.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        AppObjectController.joshApplication,
                        R.color.light_shade_of_gray
                    )
                )
                binding.description.text = getString(
                    R.string.grammar_lock_text, PrefManager.getIntValue(
                        ONLINE_TEST_LAST_LESSON_COMPLETED, defValue = 1
                    ).plus(1)
                )
            }
        }
    }

    private fun completeGrammarCardLogic() {
        /*lessonActivityListener?.onQuestionStatusUpdate(
            QUESTION_STATUS.AT,
            questionId
        )*/
        lessonActivityListener?.onSectionStatusUpdate(0, true)
    }

    fun startOnlineExamTest() {
        activity?.supportFragmentManager?.let { fragmentManager ->
            binding.parentContainer.visibility = View.VISIBLE
            binding.startTestContainer.visibility = View.GONE
            binding.testCompletedContainer.visibility = View.GONE
            fragmentManager
                .beginTransaction()
                .replace(
                    R.id.parent_Container,
                    OnlineTestFragment.getInstance(lessonNumber),
                    OnlineTestFragment.TAG
                )
                .addToBackStack(TAG)
                .commitAllowingStateLoss()
        }
    }

    private fun showGrammarCompleteLayout() {
        binding.parentContainer.visibility = View.GONE
        binding.startTestContainer.visibility = View.GONE
        binding.testCompletedContainer.visibility = View.VISIBLE
    }

    fun onGrammarContinueClick() {
        lessonActivityListener?.onNextTabCall(0)
    }

    companion object {
        const val TAG = "GrammarOnlineTestFragment"
        const val CURRENT_LESSON_NUMBER = "current_lesson_number"

        @JvmStatic
        fun getInstance(lessonNumber: Int): GrammarOnlineTestFragment {
            val args = Bundle()
            args.putInt(CURRENT_LESSON_NUMBER, lessonNumber)
            val fragment = GrammarOnlineTestFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun testCompleted() {
        showGrammarCompleteLayout()
    }

}
