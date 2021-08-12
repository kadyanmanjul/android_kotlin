package com.joshtalks.joshskills.ui.online_test

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshFragment
import com.joshtalks.joshskills.core.FREE_TRIAL_TEST_SCORE
import com.joshtalks.joshskills.core.HAS_SEEN_GRAMMAR_TOOLTIP
import com.joshtalks.joshskills.core.IS_FREE_TRIAL
import com.joshtalks.joshskills.core.ONLINE_TEST_LAST_LESSON_ATTEMPTED
import com.joshtalks.joshskills.core.ONLINE_TEST_LAST_LESSON_COMPLETED
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.playSnackbarSound
import com.joshtalks.joshskills.databinding.FragmentGrammarOnlineTestBinding
import com.joshtalks.joshskills.ui.chat.DEFAULT_TOOLTIP_DELAY_IN_MS
import com.joshtalks.joshskills.ui.lesson.GRAMMAR_POSITION
import com.joshtalks.joshskills.ui.lesson.LessonActivityListener
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GrammarOnlineTestFragment : CoreJoshFragment(), OnlineTestFragment.OnlineTestInterface {
    private lateinit var binding: FragmentGrammarOnlineTestBinding
    private var lessonActivityListener: LessonActivityListener? = null
    private var lessonNumber: Int = -1
    private var scoreText: Int = -1
    private var pointsList: String? = null

    private var currentTooltipIndex = 0
    private val lessonTooltipList by lazy {
        listOf(
            "हर पाठ में 4 भाग होते हैं \nGrammar, Vocabulary, Reading\nऔर Speaking",
            "आज, इस भाग में हम अपने वर्तमान व्याकरण स्तर का पता लगाएंगे",
            "हमारे स्तर के आधार पर अगले पाठ से हम यहाँ व्याकरण की अवधारणाएँ सीखेंगे"
        )
    }

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
        showTooltip()
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
                        binding.score.text = getString(R.string.test_score, scoreText)

                    } else {
                        binding.score.text = getString(
                            R.string.test_score, PrefManager.getIntValue(
                                FREE_TRIAL_TEST_SCORE, false, 0
                            )
                        )
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

        binding.btnNextStep.setOnClickListener {
            showNextTooltip()
        }
    }

    private fun showTooltip() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (PrefManager.getBoolValue(HAS_SEEN_GRAMMAR_TOOLTIP, defValue = false)) {
                withContext(Dispatchers.Main) {
                    binding.lessonTooltipLayout.visibility = View.GONE
                }
            } else {
                delay(DEFAULT_TOOLTIP_DELAY_IN_MS)
                if (lessonNumber == 1) {
                    withContext(Dispatchers.Main) {
                        binding.joshTextView.text = lessonTooltipList[currentTooltipIndex]
                        binding.txtTooltipIndex.text =
                            "${currentTooltipIndex + 1} of ${lessonTooltipList.size}"
                        binding.lessonTooltipLayout.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun showNextTooltip() {
        if (currentTooltipIndex < lessonTooltipList.size - 1) {
            currentTooltipIndex++
            binding.joshTextView.text = lessonTooltipList[currentTooltipIndex]
            binding.txtTooltipIndex.text =
                "${currentTooltipIndex + 1} of ${lessonTooltipList.size}"
        } else {
            binding.lessonTooltipLayout.visibility = View.GONE
            PrefManager.put(HAS_SEEN_GRAMMAR_TOOLTIP, true)
        }
    }

    fun hideTooltip() {
        binding.lessonTooltipLayout.visibility = View.GONE
        PrefManager.put(HAS_SEEN_GRAMMAR_TOOLTIP, true)
    }

    private fun completeGrammarCardLogic() {
        /*lessonActivityListener?.onQuestionStatusUpdate(
            QUESTION_STATUS.AT,
            questionId
        )*/
        lessonActivityListener?.onSectionStatusUpdate(GRAMMAR_POSITION, true)
    }

    fun startOnlineExamTest() {

        if (PermissionUtils.isStoragePermissionEnabled(AppObjectController.joshApplication).not()) {
            askStoragePermission()
            return
        }
        moveToOnlineTestFragment()

    }

    fun moveToOnlineTestFragment() {
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

    private fun askStoragePermission() {
        PermissionUtils.storageReadAndWritePermission(
            requireContext(),
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            moveToOnlineTestFragment()
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.permissionPermanentlyDeniedDialog(requireActivity())
                            //errorDismiss()
                            return
                        }
                        return
                    }
                    report?.isAnyPermissionPermanentlyDenied?.let {
                        PermissionUtils.permissionPermanentlyDeniedDialog(requireActivity())
                        //errorDismiss()
                        return
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            }
        )
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
                binding.score.text = getString(R.string.test_score, scoreText)

            } else {
                binding.score.text = getString(
                    R.string.test_score, PrefManager.getIntValue(
                        FREE_TRIAL_TEST_SCORE, false, 0
                    )
                )

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
        lessonActivityListener?.onNextTabCall(GRAMMAR_POSITION)
    }

    companion object {
        const val TAG = "GrammarOnlineTestFragment"
        const val CURRENT_LESSON_NUMBER = "current_lesson_number"
        const val POINTS_LIST = "points_list"
        const val SCORE_TEXT = "score_text"

        @JvmStatic
        fun getInstance(
            lessonNumber: Int,
            scoreText: Int? = null,
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
