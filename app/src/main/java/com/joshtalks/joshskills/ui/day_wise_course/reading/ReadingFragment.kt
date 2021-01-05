package com.joshtalks.joshskills.ui.day_wise_course.reading

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshFragment
import com.joshtalks.joshskills.core.OnWordClick
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.custom_textview.AutoLinkMode
import com.joshtalks.joshskills.core.getSpannableString
import com.joshtalks.joshskills.databinding.ReadingPracticeFragmentBinding
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.practise.PracticeEngagementV2
import com.joshtalks.joshskills.repository.local.entity.practise.PractiseType
import com.joshtalks.joshskills.ui.day_wise_course.CapsuleActivityCallback
import com.joshtalks.joshskills.ui.day_wise_course.reading.feedback.FeedbackListAdapter
import com.joshtalks.joshskills.ui.practise.PracticeViewModel
import com.joshtalks.joshskills.ui.translation.LanguageTranslationDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class ReadingFragment : CoreJoshFragment() {

    companion object {
        const val PRACTISE_OBJECT = "practise_object"
        const val MAX_ATTEMPT = 4
        const val separatorRegex = "<a>([\\s\\S]*?)<\\/a>"

        @JvmStatic
        fun instance(chatModelList: ArrayList<ChatModel>) = ReadingFragment().apply {
            arguments = Bundle().apply {
                putParcelableArrayList(PRACTISE_OBJECT, chatModelList)
            }
        }
    }

    private lateinit var binding: ReadingPracticeFragmentBinding
    private var chatModel: ChatModel? = null
    private var chatList: ArrayList<ChatModel>? = null
    var activityCallback: CapsuleActivityCallback? = null


    private val practiceViewModel: PracticeViewModel by lazy {
        ViewModelProvider(this).get(PracticeViewModel::class.java)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            arguments?.let {
                chatList = it.getParcelableArrayList(PRACTISE_OBJECT)
            }
        }
        chatModel = chatList?.getOrNull(0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.reading_practice_fragment,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        addObserver()
        chatModel?.question?.run {
            coreJoshActivity?.feedbackEngagementStatus(this)
        }
    }

    private fun initView() {
        chatModel?.question?.run {
            binding.txtReadingParagraph.addAutoLinkMode(AutoLinkMode.MODE_CUSTOM)
            binding.txtReadingParagraph.enableUnderLine()
            binding.txtReadingParagraph.setCustomRegex(separatorRegex)
            CoroutineScope(Dispatchers.Main).launch {
                binding.txtReadingParagraph.text = qText?.getSpannableString(
                    separatorRegex,
                    "<a>",
                    "</a>",
                    defaultSelectedColor = ContextCompat.getColor(requireContext(), R.color.e1_red),
                    selectedColor = ContextCompat.getColor(requireContext(), R.color.e1_red),
                    clickListener = object : OnWordClick {
                        override fun clickedWord(word: String) {
                            LanguageTranslationDialog.showLanguageDialog(childFragmentManager, word)
                        }
                    })
            }

            audioList?.getOrNull(0)?.let {
                binding.readingAudioNote.initAudioPlayer(it.audio_url, it.duration)
            }

            initAdapter(practiseEngagementV2)

        }
    }

    private fun initAdapter(practiseEngagementV2: List<PracticeEngagementV2>?) {
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.viewPager.offscreenPageLimit = practiseEngagementV2?.size ?: 1
        binding.viewPager.setPageTransformer(MarginPageTransformer(Utils.dpToPx(40)))

        if (practiseEngagementV2.isNullOrEmpty()) {
            binding.viewPager.adapter = FeedbackListAdapter(
                this,
                arrayListOf(PracticeEngagementV2(practiseType = PractiseType.NOT_SUBMITTED))
            )
        } else {
            binding.viewPager.adapter =
                FeedbackListAdapter(this, practiseEngagementV2)
        }
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            val pos = position + 1
            tab.text = "Attempt $pos"
        }.attach()
        binding.viewPager.measure(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }


    private fun addObserver() {
        practiceViewModel.requestStatusLiveData.observe(viewLifecycleOwner, Observer {
        })

        practiceViewModel.practiceFeedback2LiveData.observe(viewLifecycleOwner, Observer {
        })

        practiceViewModel.practiceEngagementData.observe(viewLifecycleOwner, Observer {
        })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is CapsuleActivityCallback)
            activityCallback = context
    }

}
