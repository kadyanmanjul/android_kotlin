package com.joshtalks.joshskills.ui.lesson.reading.feedback

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.custom_ui.custom_textview.AutoLinkMode
import com.joshtalks.joshskills.databinding.ReadingPracticeFragmentBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.CHAT_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.LessonQuestion
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.repository.local.entity.practise.PracticeEngagementV2
import com.joshtalks.joshskills.repository.local.entity.practise.PractiseType
import com.joshtalks.joshskills.repository.local.eventbus.EmptyEventBus
import com.joshtalks.joshskills.repository.local.eventbus.ViewPagerDisableEventBus
import com.joshtalks.joshskills.ui.lesson.LessonActivityListener
import com.joshtalks.joshskills.ui.lesson.LessonViewModel
import com.joshtalks.joshskills.ui.lesson.READING_POSITION
import com.joshtalks.joshskills.ui.translation.LanguageTranslationDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ReadingFragment : CoreJoshFragment(), ReadingPractiseCallback {

    companion object {
        const val PRACTISE_OBJECT = "practise_object"
        const val separatorRegex = "<a>([\\s\\S]*?)<\\/a>"

        @JvmStatic
        fun instance(chatModelList: ArrayList<ChatModel>) = ReadingFragment().apply {
            arguments = Bundle().apply {
                putParcelableArrayList(PRACTISE_OBJECT, chatModelList)
            }
        }
    }

    private lateinit var binding: ReadingPracticeFragmentBinding

    private var rpQuestion: LessonQuestion? = null
    var lessonActivityListener: LessonActivityListener? = null
    private var compositeDisposable = CompositeDisposable()


    private val viewModel: LessonViewModel by lazy {
        ViewModelProvider(requireActivity()).get(LessonViewModel::class.java)
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is LessonActivityListener) {
            lessonActivityListener = context
        }
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
        addObserver()
        return binding.root
    }

    private fun addObserver() {

        viewModel.lessonQuestionsLiveData.observe(viewLifecycleOwner, {
            rpQuestion = it.filter { it.chatType == CHAT_TYPE.RP }.getOrNull(0)
            if (rpQuestion == null) {
                requireActivity().finish()
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        binding.viewPager.measure(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        rpQuestion?.run {
            if (audioList.isNullOrEmpty()) {
                binding.cardViewDemoVoiceNote.visibility = View.GONE
                binding.imgQuotes.visibility = View.GONE
            }
            binding.txtReadingParagraph.addAutoLinkMode(AutoLinkMode.MODE_CUSTOM)
            binding.txtReadingParagraph.enableUnderLine()
            binding.txtReadingParagraph.setCustomRegex(separatorRegex)
            CoroutineScope(Dispatchers.Main).launch {
                binding.txtReadingParagraph.text = qText?.getSpannableString(
                    separatorRegex,
                    "<a>",
                    "</a>",
                    defaultSelectedColor = ContextCompat.getColor(requireContext(), R.color.black),
                    selectedColor = ContextCompat.getColor(requireContext(), R.color.black),
                    clickListener = object : OnWordClick {
                        override fun clickedWord(word: String) {
                            if (viewModel.isRecordingStarted()) {
                                return
                            }
                            LanguageTranslationDialog.showLanguageDialog(childFragmentManager, word)
                        }
                    })

                audioList?.getOrNull(0)?.let {
                    binding.readingAudioNote.initAudioPlayer(it.audio_url, it.duration)
                }
                imageList?.getOrNull(0)?.imageUrl?.let {
                    binding.imgDemoVoiceNote.setImage(it)
                }

                delay(500)
                initAdapter(practiseEngagementV2)
            }
        }
    }

    private fun initAdapter(practiseEngagementV2: List<PracticeEngagementV2>?) {
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        if (practiseEngagementV2.isNullOrEmpty()) {
            binding.viewPager.adapter = FeedbackListAdapter(
                this,
                arrayListOf(
                    PracticeEngagementV2(
                        questionForId = rpQuestion?.id,
                        practiseType = PractiseType.NOT_SUBMITTED
                    )
                )
            )
        } else {
            binding.viewPager.offscreenPageLimit = practiseEngagementV2.size
            binding.viewPager.setPageTransformer(MarginPageTransformer(Utils.dpToPx(40)))
            binding.viewPager.adapter =
                FeedbackListAdapter(this, practiseEngagementV2)
        }
        enableTab((binding.viewPager.adapter as FeedbackListAdapter).itemCount)
    }

    private fun enableTab(count: Int) {
        if (count < 2) {
            return
        }
        binding.tabLayout.visibility = View.VISIBLE
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            val pos = position + 1
            tab.text = "Attempt $pos"
        }.attach()
    }


    override fun onImproveAnswer() {
        CoroutineScope(Dispatchers.Main).launch {
            val anyUploadingPractice =
                (binding.viewPager.adapter as FeedbackListAdapter).isAnyPractiseUploading()
            if (anyUploadingPractice) {
                binding.viewPager.currentItem =
                    (binding.viewPager.adapter as FeedbackListAdapter).itemCount.minus(1)
                return@launch
            }

            rpQuestion?.run {
                val tempList = arrayListOf<PracticeEngagementV2>()
                if (practiseEngagementV2.isNullOrEmpty().not()) {
                    tempList.addAll(practiseEngagementV2!!.toMutableList())
                }
                tempList.add(
                    PracticeEngagementV2(
                        questionForId = rpQuestion?.id,
                        practiseType = PractiseType.NOT_SUBMITTED
                    )
                )
                initAdapter(tempList)
                delay(100)
                binding.viewPager.currentItem = tempList.size - 1
                delay(100)
                binding.rootView.scrollTo(0, binding.cardViewDemoVoiceNote.bottom)
                binding.readingAudioNote.pausePlayer()
            }
        }
    }

    override fun onContinue() {
        lessonActivityListener?.onNextTabCall(READING_POSITION)
    }

    override fun onPracticeSubmitted() {
        lessonActivityListener?.onQuestionStatusUpdate(
            QUESTION_STATUS.AT,
            rpQuestion?.id
        )
        lessonActivityListener?.onSectionStatusUpdate(READING_POSITION, true)
    }

    override fun onResume() {
        super.onResume()
        subscribeRxBusObserver()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    private fun subscribeRxBusObserver() {
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(EmptyEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    rpQuestion?.id?.let {
                        viewModel.getPracticeAfterUploaded(it, ::callback)
                    }
                }, {
                    it.printStackTrace()
                })
        )
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(ViewPagerDisableEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    //  binding.rootView.requestDisallowInterceptTouchEvent(it.flag.not())
                    binding.viewPager.isUserInputEnabled = it.flag
                    binding.readingAudioNote.pausePlayer()
                }, {
                    it.printStackTrace()
                })
        )

    }

    private fun callback(lessonQuestion: LessonQuestion?) {
        lessonQuestion?.let {
            this@ReadingFragment.rpQuestion = it
            CoroutineScope(Dispatchers.Main).launch {
                this@ReadingFragment.rpQuestion?.practiseEngagementV2?.run {
                    initAdapter(this)
                    binding.viewPager.currentItem = this.size - 1
                }
            }
        }
    }

}
