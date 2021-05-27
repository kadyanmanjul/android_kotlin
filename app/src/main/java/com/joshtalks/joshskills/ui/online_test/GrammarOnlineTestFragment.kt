package com.joshtalks.joshskills.ui.online_test

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshFragment
import com.joshtalks.joshskills.databinding.FragmentGrammarOnlineTestBinding
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.lesson.LessonActivityListener

class GrammarOnlineTestFragment : CoreJoshFragment() {
    private lateinit var binding: FragmentGrammarOnlineTestBinding
    private var lessonActivityListener: LessonActivityListener? = null

    private var openOnlineTestActivity: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            showGrammarCompleteLayout()
        }
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

        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_grammar_online_test, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // TODO if this is completed
        binding.startTestContainer.visibility = View.VISIBLE
        binding.testCompletedContainer.visibility = View.GONE
    }

    fun startOnlineExamTest() {
        openOnlineTestActivity.launch(
            Intent(requireActivity(), OnlineTestActivity::class.java).apply {
                putExtra(CONVERSATION_ID, requireActivity().intent.getStringExtra(CONVERSATION_ID))
            }
        )
    }

    fun showTestCompletedScreen(messageText: String) {
        binding.startTestContainer.visibility = View.GONE
        binding.testCompletedContainer.visibility = View.VISIBLE
        if (messageText.isNullOrBlank().not()) {
            binding.title2.text = messageText
        }
    }

    private fun showGrammarCompleteLayout() {
        binding.startTestContainer.visibility = View.GONE
        binding.testCompletedContainer.visibility = View.VISIBLE
    }

    fun onGrammarContinueClick() {
        lessonActivityListener?.onNextTabCall(0)
    }

    companion object {
        const val TAG = "GrammarOnlineTestFragment"

        @JvmStatic
        fun getInstance() = GrammarOnlineTestFragment()
    }

}
