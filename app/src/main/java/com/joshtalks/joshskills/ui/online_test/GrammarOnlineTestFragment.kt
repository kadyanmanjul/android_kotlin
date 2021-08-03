package com.joshtalks.joshskills.ui.online_test

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshFragment
import com.joshtalks.joshskills.core.HAS_OPENED_GRAMMAR_FIRST_TIME
import com.joshtalks.joshskills.core.IS_FREE_TRIAL
import com.joshtalks.joshskills.core.ONLINE_TEST_LAST_LESSON_ATTEMPTED
import com.joshtalks.joshskills.core.ONLINE_TEST_LAST_LESSON_COMPLETED
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.playSnackbarSound
import com.joshtalks.joshskills.databinding.FragmentGrammarOnlineTestBinding
import com.joshtalks.joshskills.ui.lesson.LessonActivityListener

class GrammarOnlineTestFragment : CoreJoshFragment(), OnlineTestFragment.OnlineTestInterface {
    private lateinit var binding: FragmentGrammarOnlineTestBinding
    private var lessonActivityListener: LessonActivityListener? = null
    private var lessonNumber: Int = -1
    private var scoreText: Int = -1
    private var pointsList: String? = null

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
            scoreText = it.getInt(SCORE_TEXT, -1)
            pointsList = it.getString(POINTS_LIST)
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
            (PrefManager.getIntValue(ONLINE_TEST_LAST_LESSON_COMPLETED)
                .plus(1) == lessonNumber) -> {
                binding.startTestContainer.visibility = View.VISIBLE
                binding.testCompletedContainer.visibility = View.GONE
                binding.testScoreContainer.visibility = View.GONE
                if (PrefManager.getIntValue(
                        ONLINE_TEST_LAST_LESSON_ATTEMPTED
                    ) == lessonNumber
                ) {
                    binding.description.text = getString(R.string.grammar_continue_test_text)
                    binding.startBtn.text = getString(R.string.grammar_btn_text_continue)
                }
            }
            (PrefManager.getIntValue(
                ONLINE_TEST_LAST_LESSON_COMPLETED
            ) >= lessonNumber) -> {
                binding.startTestContainer.visibility = View.GONE
                if (PrefManager.hasKey(IS_FREE_TRIAL) && PrefManager.getBoolValue(
                        IS_FREE_TRIAL,
                        false,
                        false
                    )
                ) {
                    binding.testScoreContainer.visibility = View.VISIBLE
                    if (scoreText != -1) {
                        binding.score.text = getString(R.string.test_score, scoreText )

                    } else {
                        binding.score.text = getString(R.string.test_score, "0")

                    }
                    if (pointsList.isNullOrBlank().not()) {
                        showSnackBar(binding.rootView, Snackbar.LENGTH_LONG, pointsList)
                        playSnackbarSound(requireContext())
                    }
                } else {
                    binding.testCompletedContainer.visibility = View.VISIBLE
                }
                completeGrammarCardLogic()
            }
            else -> {
                binding.startTestContainer.visibility = View.VISIBLE
                binding.testCompletedContainer.visibility = View.GONE
                binding.testScoreContainer.visibility = View.GONE
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
                        ONLINE_TEST_LAST_LESSON_COMPLETED
                    ).plus(1)
                )
            }
        }

        if (PrefManager.getBoolValue(HAS_OPENED_GRAMMAR_FIRST_TIME, defValue = true)) {
            binding.lessonTooltipLayout.visibility = View.VISIBLE
        } else {
            binding.lessonTooltipLayout.visibility = View.GONE
        }
    }

    override fun onPause() {
        super.onPause()
//        binding.lessonTooltipLayout.visibility = View.GONE
//        PrefManager.put(HAS_OPENED_GRAMMAR_FIRST_TIME, false)
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
            binding.testScoreContainer.visibility = View.GONE
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
        if (PrefManager.hasKey(IS_FREE_TRIAL) && PrefManager.getBoolValue(
                IS_FREE_TRIAL,
                false,
                false
            )
        ) {
            binding.testScoreContainer.visibility = View.VISIBLE
            if (scoreText != -1) {
                binding.score.text = getString(R.string.test_score, scoreText )

            } else {
                binding.score.text = getString(R.string.test_score, "0")

            }
            if (pointsList.isNullOrBlank().not()) {
                showSnackBar(binding.rootView, Snackbar.LENGTH_LONG, pointsList)
                playSnackbarSound(requireContext())
            }
        } else {
            binding.testCompletedContainer.visibility = View.VISIBLE
        }
    }

    fun onGrammarContinueClick() {
        lessonActivityListener?.onNextTabCall(0)
    }

    companion object {
        const val TAG = "GrammarOnlineTestFragment"
        const val CURRENT_LESSON_NUMBER = "current_lesson_number"
        const val POINTS_LIST = "points_list"
        const val SCORE_TEXT = "score_text"

        @JvmStatic
        fun getInstance(
            lessonNumber: Int,
            scoreText: Int?=null,
            pointsList: String? = null
        ): GrammarOnlineTestFragment {
            val args = Bundle()
            args.putInt(CURRENT_LESSON_NUMBER, lessonNumber)
            args.putString(POINTS_LIST, pointsList)
            scoreText?.let {
                args.putInt(SCORE_TEXT, scoreText)
            }
            val fragment = GrammarOnlineTestFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun testCompleted() {
        showGrammarCompleteLayout()
    }

}
