package com.joshtalks.joshskills.lesson.vocabulary

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.common.constants.VOCAB_POSITION
import com.joshtalks.joshskills.common.core.*
import com.joshtalks.joshskills.common.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.common.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.common.core.analytics.ParamKeys
import com.joshtalks.joshskills.common.core.custom_ui.recorder.OnAudioRecordListener
import com.joshtalks.joshskills.common.core.custom_ui.recorder.RecordingItem
import com.joshtalks.joshskills.common.core.io.AppDirectory
import com.joshtalks.joshskills.common.core.videotranscoder.enforceSingleScrollDirection
import com.joshtalks.joshskills.common.repository.local.entity.*
import com.joshtalks.joshskills.common.repository.local.eventbus.SnackBarEvent
import com.joshtalks.joshskills.common.repository.local.model.Mentor
import com.joshtalks.joshskills.common.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.common.repository.local.model.assessment.AssessmentWithRelations
import com.joshtalks.joshskills.common.repository.server.PurchasePopupType
import com.joshtalks.joshskills.common.repository.server.RequestEngage
import com.joshtalks.joshskills.common.ui.chat.DEFAULT_TOOLTIP_DELAY_IN_MS
import com.joshtalks.joshskills.lesson.LessonActivity
import com.joshtalks.joshskills.lesson.R
import com.joshtalks.joshskills.lesson.databinding.FragmentVocabularyBinding
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import timber.log.Timber

class VocabularyFragment : CoreJoshFragment(), VocabularyPracticeAdapter.PracticeClickListeners {
    private lateinit var adapter: VocabularyPracticeAdapter

    private var compositeDisposable = CompositeDisposable()

    private lateinit var binding: FragmentVocabularyBinding
    private var isVideoRecordDone = false
    private var isDocumentAttachDone = false
    private var startTime: Long = 0
    private var totalTimeSpend: Long = 0
    private var filePath: String? = null
    private var currentQuestion: LessonQuestion? = null

    private var lessonActivityListener: com.joshtalks.joshskills.lesson.LessonActivityListener? = null
    private var aPosition: Int = -1

    private val viewModel: com.joshtalks.joshskills.lesson.LessonViewModel by lazy {
        ViewModelProvider(requireActivity()).get(com.joshtalks.joshskills.lesson.LessonViewModel::class.java)
    }

    private var currentTooltipIndex = 0
    private val lessonTooltipList by lazy {
        listOf(
            "हम यहां हर रोज़ 3 शब्द सीखेंगे",
            "जैसे-जैसे कोर्स आगे बढ़ेगा हम यहां वाक्यांश और मुहावरे भी सीखेंगे"
        )
    }

    private var lessonID = -1
    private var timerPopText = EMPTY

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is com.joshtalks.joshskills.lesson.LessonActivityListener)
            lessonActivityListener = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        totalTimeSpend = System.currentTimeMillis()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_vocabulary, container, false)
        binding.rootView.layoutTransition?.setAnimateParentHierarchy(false)
        binding.lifecycleOwner = this
        binding.handler = this
        binding.vocabularyCompletedTv.text = AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.VOCABULARY_COMPLETED)

        addObserver()

        if (isAllQuestionsAttempted()) {
            binding.vocabularyCompleteLayout.visibility = View.VISIBLE
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBottomMargin()
        // showTooltip()
    }

    private fun showTooltip() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (PrefManager.getBoolValue(HAS_SEEN_VOCAB_TOOLTIP, defValue = false)) {
                withContext(Dispatchers.Main) {
                    binding.lessonTooltipLayout.visibility = View.GONE
                }
            } else {
                delay(DEFAULT_TOOLTIP_DELAY_IN_MS)
                if (viewModel.lessonLiveData.value?.lessonNo == 1) {
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
            PrefManager.put(HAS_SEEN_VOCAB_TOOLTIP, true)
        }
    }

    fun hideTooltip() {
        binding.lessonTooltipLayout.visibility = View.GONE
        PrefManager.put(HAS_SEEN_VOCAB_TOOLTIP, true)
    }

    private fun addObserver() {
        viewModel.lessonId.observe(
            viewLifecycleOwner
        ){
            lessonID = it
        }

//        viewModel.updatedLessonResponseLiveData.observe(
//            viewLifecycleOwner
//        ) {
//            timerPopText = it.popUpText?.body?: EMPTY
//            if (timerPopText!= EMPTY){
//                PurchaseDialog.newInstance(timerPopText,it.popUpText?.title?: EMPTY, it.popUpText?.price?: EMPTY).show(requireActivity().supportFragmentManager,"PurchaseDialog")
//            }
//        }

        viewModel.lessonQuestionsLiveData.observe(
            viewLifecycleOwner,
            {
                initAdapter(ArrayList())
                viewModel.getAssessmentData(it.filter { it.chatType == CHAT_TYPE.VP })
            }
        )

        viewModel.vocabAssessmentData.observe(
            viewLifecycleOwner,
            {
                adapter.updateAssessmentQuizList(it)
            }
        )

        /*viewModel.pointsSnackBarText.observe(viewLifecycleOwner) {
            if (it.pointsList.isNullOrEmpty().not()) {
                showSnackBar(binding.rootView, Snackbar.LENGTH_LONG, it.pointsList!!.get(0))
            }
        }*/
        binding.btnNextStep.setOnClickListener {
            showNextTooltip()
        }
    }

    private fun initAdapter(assessmentList: ArrayList<AssessmentWithRelations>) {
        viewModel.lessonQuestionsLiveData.value?.filter { it.chatType == CHAT_TYPE.VP }
            ?.let { lessonQuestions ->
                adapter = VocabularyPracticeAdapter(
                    requireContext(),
                    lessonQuestions.sortedBy { it.vpSortOrder },
                    assessmentList,
                    this,
                    this,
                    getConversationId()
                )
            }

        adapter.setHasStableIds(true)
        binding.practiceRv.layoutManager = LinearLayoutManager(requireContext())
        binding.practiceRv.adapter = adapter
        binding.practiceRv.setHasFixedSize(true)
        binding.practiceRv.setItemViewCacheSize(5)
        binding.practiceRv.enforceSingleScrollDirection()

        if (isAdded && activity!=null) {
            if (requireActivity().intent?.getBooleanExtra("reopen", false) == true) {
                val pos = adapter.getItemPosition(requireActivity().intent?.getStringExtra("practice_word"))
                if (pos >= 0) {
                    adapter.fromNotification = true
                    adapter.expandCardPosition = pos
                    binding.practiceRv.scrollToPosition(pos);
                }
            }
        }else{
            showToast(getString(R.string.something_went_wrong))
        }
    }

    override fun submitQuiz(
        lessonQuestion: LessonQuestion,
        isCorrect: Boolean,
        questionId: Int,
        positionInList: Int,
        hasNextItem: Boolean,
        canShowSectionCompletedCard: Boolean
    ) {
        onQuestionSubmitted(
            lessonQuestion,
            positionInList,
            hasNextItem,
            isCorrect,
            questionId
        )
        openNextScreen(canShowSectionCompletedCard)
    }

    override fun quizOptionSelected(
        lessonQuestion: LessonQuestion,
        assessmentQuestion: AssessmentQuestionWithRelations,
        positionInList: Int,
        hasNextItem: Boolean
    ) {
        viewModel.saveAssessmentQuestion(assessmentQuestion)
        lessonActivityListener?.onQuestionStatusUpdate(
            QUESTION_STATUS.IP,
            lessonQuestion.id
        )
        viewModel.saveQuizToServer(assessmentQuestion.question.assessmentId)
        lessonQuestion.status = QUESTION_STATUS.IP
        currentQuestion = null
        if (hasNextItem) {
            adapter.notifyItemRangeChanged(positionInList, 2)
        } else {
            adapter.notifyItemChanged(positionInList)
        }
    }

    override fun playAudio(position: Int) {
        aPosition = position
    }

    override fun cancelAudio() {
        filePath = null
    }

    private fun onQuestionSubmitted(
        lessonQuestion: LessonQuestion,
        positionInList: Int,
        hasNextItem: Boolean,
        isCorrect: Boolean = false,
        questionId: Int = -1
    ) {

        if (isCorrect && questionId != -1) {
            val quizQuestion = arrayListOf<Int>()
            quizQuestion.add(questionId)
            lessonActivityListener?.onQuestionStatusUpdate(
                QUESTION_STATUS.AT,
                lessonQuestion.id,
                quizCorrectQuestionIds = quizQuestion
            )
        } else {
            lessonActivityListener?.onQuestionStatusUpdate(
                QUESTION_STATUS.AT,
                lessonQuestion.id
            )
        }

        lessonQuestion.status = QUESTION_STATUS.AT

        currentQuestion = null
        AppObjectController.uiHandler.post {
            if (hasNextItem) {
                adapter.notifyItemRangeChanged(positionInList, 2)
            } else {
                adapter.notifyItemChanged(positionInList)
            }
            // adapter.notifyDataSetChanged()
        }
    }

    override fun openNextScreen(canShowSectionCompletedCard: Boolean) {
        if (isAllQuestionsAttempted() && isVisible) {
            if (canShowSectionCompletedCard) {
                AppObjectController.uiHandler.post {
                    binding.vocabularyCompleteLayout.visibility = View.VISIBLE
                }
                MixPanelTracker.publishEvent(MixPanelEvent.VOCABULARY_COMPLETED)
                    .addParam(ParamKeys.LESSON_ID,lessonID)
                    .push()
                if (PrefManager.getBoolValue(IS_FREE_TRIAL))
                    viewModel.getCoursePopupData(PurchasePopupType.VOCAB_COMPLETED)
            }
            lessonActivityListener?.onSectionStatusUpdate(VOCAB_POSITION, true)
        }
    }

    private fun isAllQuestionsAttempted(): Boolean {
        viewModel.lessonQuestionsLiveData.value?.filter { it.chatType == CHAT_TYPE.VP }
            ?.forEach { item ->
                if (item.status != QUESTION_STATUS.AT) {
                    return false
                }
            }
        return true
    }

    fun onContinueClick() {
        MixPanelTracker.publishEvent(MixPanelEvent.VOCAB_CONTINUE)
            .addParam(ParamKeys.LESSON_ID, lessonID)
            .push()
        lessonActivityListener?.onNextTabCall(
            VOCAB_POSITION.minus(
                if (PrefManager.getBoolValue(
                        IS_A2_C1_RETENTION_ENABLED
                    )
                ) 0
                else 1
            )
        )
    }

    fun onCloseDialog() {
        MixPanelTracker.publishEvent(MixPanelEvent.VOCAB_CLOSE)
            .addParam(ParamKeys.LESSON_ID,lessonID)
            .push()
        binding.vocabularyCompleteLayout.visibility = View.GONE
    }

    override fun submitPractice(
        lessonQuestion: LessonQuestion,
        positionInList: Int,
        hasNextItem: Boolean
    ): Boolean {
        if (lessonQuestion.expectedEngageType != null) {
            lessonQuestion.expectedEngageType?.let {
                if (EXPECTED_ENGAGE_TYPE.TX == it) {
                    showToast(getString(R.string.submit_practise_msz))
                    return false
                } else if (EXPECTED_ENGAGE_TYPE.AU == it && lessonQuestion.filePath == null) {
                    showToast(getString(R.string.submit_practise_msz))
                    return false
                } else if (EXPECTED_ENGAGE_TYPE.VI == it && isVideoRecordDone.not()) {
                    showToast(getString(R.string.submit_practise_msz))
                    return false
                } else if (EXPECTED_ENGAGE_TYPE.DX == it && isDocumentAttachDone.not()) {
                    showToast(getString(R.string.submit_practise_msz))
                    return false
                }
                currentQuestion = lessonQuestion
                lessonQuestion.status = QUESTION_STATUS.IP

                CoroutineScope(Dispatchers.IO).launch {
                    viewModel.getPointsForVocabAndReading(lessonQuestion.id)
                    onQuestionSubmitted(lessonQuestion, positionInList, hasNextItem)
                    openNextScreen(true)

                    val requestEngage = RequestEngage()
//                requestEngage.text = binding.etPractise.text.toString()
                    requestEngage.localPath = lessonQuestion.filePath
                    if (isAdded && activity != null) {
                        requestEngage.duration = Utils.getDurationOfMedia(requireActivity(), lessonQuestion.filePath)?.toInt()
                        // requestEngage.feedbackRequire = lessonQuestion.feedback_require
                        requestEngage.questionId = lessonQuestion.id
                        requestEngage.mentor = Mentor.getInstance().getId()
                        if (it == EXPECTED_ENGAGE_TYPE.AU || it == EXPECTED_ENGAGE_TYPE.VI || it == EXPECTED_ENGAGE_TYPE.DX) {
                            requestEngage.answerUrl = lessonQuestion.filePath
                        }
                        delay(1000)
                        viewModel.addTaskToService(requestEngage, PendingTask.VOCABULARY_PRACTICE)
                    }else{
                        showToast(getString(R.string.something_went_wrong))
                    }
                }
                return true
            }
        }
        return false
    }

    override fun startRecording(
        lessonQuestion: LessonQuestion,
        position: Int,
        startTimeUnit: Long
    ) {
//        if (isCallOngoing()) {
//            return
//        }
        this.startTime = startTimeUnit
        if (isAdded && activity != null)
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        viewModel.startRecordAudio(object : OnAudioRecordListener {
            override fun onRecordFinished(recordingItem: RecordingItem?) {
                recordingItem?.filePath?.let {
                    filePath = AppDirectory.getAudioSentFile(
                        null,
                        audioExtension = ".m4a"
                    ).absolutePath
                    AppDirectory.copy(it, filePath!!)
                    lessonQuestion.filePath = filePath
                }
            }
        })
    }

    override fun stopRecording(lessonQuestion: LessonQuestion, position: Int, stopTime: Long) {
        viewModel.stopRecordingAudio(false)
        if (isAdded && activity != null)
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun askRecordPermission() {
        if (isAdded && activity != null) {
            PermissionUtils.audioRecordStorageReadAndWritePermission(
                requireActivity(),
                object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.areAllPermissionsGranted()?.let {
                            if (report.isAnyPermissionPermanentlyDenied) {
                                if (isAdded && activity != null)
                                    PermissionUtils.permissionPermanentlyDeniedDialog(
                                        requireActivity(),
                                        R.string.record_permission_message
                                    )
                                return
                            }
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
        }else{
            showToast(getString(R.string.something_went_wrong))
        }
    }

    override fun focusChild(position: Int) {
        try {
            binding.practiceRv.smoothScrollToPosition(position)
        } catch (ex: java.lang.Exception) {
            Timber.d(ex)
        }
    }

    private fun initBottomMargin() {
        if (isAdded && activity is LessonActivity && (requireActivity() as LessonActivity).getBottomBannerHeight() > 0) {
            binding.bannerMargin.layoutParams.height = (requireActivity() as LessonActivity).getBottomBannerHeight()
        }
    }

    private fun subscribeRXBus() {
        compositeDisposable.add(
            com.joshtalks.joshskills.common.messaging.RxBus2.listenWithoutDelay(SnackBarEvent::class.java)
                .subscribeOn(Schedulers.computation())
                .subscribe(
                    {
                        // if (it.questionId in chatModelList.) check for question Id later
                        showSnackBar(
                            binding.rootView,
                            Snackbar.LENGTH_LONG,
                            it.pointsSnackBarText.toString()
                        )
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )
    }

    override fun onResume() {
        super.onResume()
        subscribeRXBus()
        if (PrefManager.hasKey(HAS_SEEN_VOCAB_SCREEN).not()) {
            PrefManager.put(HAS_SEEN_VOCAB_SCREEN, true)
        }
        try {
            if (isVisible.not()) {
                adapter.audioManager?.onPause()
                adapter.itemList.forEachIndexed { index, lessonQuestion ->
                    if (lessonQuestion.type != LessonQuestionType.QUIZ)
                        (
                                binding.practiceRv.findViewHolderForAdapterPosition(index)
                                        as VocabularyPracticeAdapter.VocabularyViewHolder
                                ).pauseAudio()
                }
            }
        } catch (ex: Throwable) {
            Timber.d(ex)
        }
    }

    override fun onPause() {
        super.onPause()
        if (::adapter.isInitialized) {
            adapter.audioManager?.onPause()
        }
//        adapter.itemList.forEachIndexed { index, lessonQuestion ->
//            if (lessonQuestion.type != LessonQuestionType.QUIZ)
//                binding.practiceRv.findViewHolderForAdapterPosition(index)?.let {
//                    (it as VocabularyPracticeAdapter.VocabularyViewHolder?)?.pauseAudio()
//                }
//        }
//        aPosition = -1
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
        try {
            adapter.audioManager?.onPause()
        } catch (ex: Exception) {
        }
    }

    override fun onDestroy() {
        try {
            super.onDestroy()
            try {
                adapter.audioManager?.release()
            } catch (ex: Exception) {
            }
        } catch (ex: Exception) {
        }
    }

    companion object {
        @JvmStatic
        fun getInstance() = VocabularyFragment()
    }
}
