package com.joshtalks.joshskills.ui.lesson.vocabulary

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshFragment
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.recorder.OnAudioRecordListener
import com.joshtalks.joshskills.core.custom_ui.recorder.RecordingItem
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentVocabularyBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.CHAT_TYPE
import com.joshtalks.joshskills.repository.local.entity.EXPECTED_ENGAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.LessonQuestion
import com.joshtalks.joshskills.repository.local.entity.LessonQuestionType
import com.joshtalks.joshskills.repository.local.entity.PendingTask
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.SnackBarEvent
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentWithRelations
import com.joshtalks.joshskills.repository.server.RequestEngage
import com.joshtalks.joshskills.ui.lesson.LessonActivityListener
import com.joshtalks.joshskills.ui.lesson.LessonViewModel
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    private var lessonActivityListener: LessonActivityListener? = null
    private var aPosition: Int = -1


    private val viewModel: LessonViewModel by lazy {
        ViewModelProvider(requireActivity()).get(LessonViewModel::class.java)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Timber.d("Sahil : onAttach() Started")
        if (context is LessonActivityListener)
            lessonActivityListener = context
        Timber.d("Sahil : onAttach() Completed")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("Sahil : onCreate() Started")
        super.onCreate(savedInstanceState)
        totalTimeSpend = System.currentTimeMillis()
        Timber.d("Sahil : onCreate() Completed")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.d("Sahil : onCreateView() Started")
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_vocabulary, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        //binding.rootView.layoutTransition.setAnimateParentHierarchy(false)
        binding.vocabularyCompletedTv.text = AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.VOCABULARY_COMPLETED)

        addObserver()

        if (isAllQuestionsAttempted()) {
            binding.vocabularyCompleteLayout.visibility = View.VISIBLE
        }

        Timber.d("Sahil : onCreateView() Completed")
        return binding.root
    }

    private fun addObserver() {

        viewModel.lessonQuestionsLiveData.observe(viewLifecycleOwner, {
            Timber.d("Sahil : lessonQuestionsLiveData.observe Started")
            initAdapter(ArrayList())
            viewModel.getAssessmentData(it.filter { it.chatType == CHAT_TYPE.VP })
        })

        viewModel.vocabAssessmentData.observe(viewLifecycleOwner, {
            Timber.d("Sahil : vocabAssessmentData.observe Started")
            initAdapter(it)
            Timber.d("Sahil : vocabAssessmentData.observe Completed")
        })

        /*viewModel.pointsSnackBarText.observe(viewLifecycleOwner) {
            if (it.pointsList.isNullOrEmpty().not()) {
                showSnackBar(binding.rootView, Snackbar.LENGTH_LONG, it.pointsList!!.get(0))
            }
        }*/
    }

    private fun initAdapter(assessmentList: ArrayList<AssessmentWithRelations>) {
        viewModel.lessonQuestionsLiveData.value?.filter { it.chatType == CHAT_TYPE.VP }
            ?.let { lessonQuestions ->
                adapter = VocabularyPracticeAdapter(
                    requireContext(),
                    lessonQuestions.sortedBy { it.vpSortOrder },
                    assessmentList,
                    this,
                    this
                )
            }

        adapter.setHasStableIds(true)
        binding.practiceRv.layoutManager = LinearLayoutManager(requireContext())
        binding.practiceRv.adapter = adapter
        binding.practiceRv.setHasFixedSize(true)
        binding.practiceRv.setItemViewCacheSize(5)
    }

    override fun submitQuiz(lessonQuestion: LessonQuestion, isCorrect: Boolean, questionId: Int) {
        onQuestionSubmitted(lessonQuestion, isCorrect, questionId)
        openNextScreen()
    }

    override fun quizOptionSelected(
        lessonQuestion: LessonQuestion,
        assessmentQuestion: AssessmentQuestionWithRelations
    ) {
        viewModel.saveAssessmentQuestion(assessmentQuestion)
        lessonActivityListener?.onQuestionStatusUpdate(
            QUESTION_STATUS.IP,
            lessonQuestion.id
        )
        viewModel.saveQuizToServer(assessmentQuestion.question.assessmentId)
        lessonQuestion.status = QUESTION_STATUS.IP
        currentQuestion = null
        adapter.notifyDataSetChanged()
    }

    override fun playAudio(position: Int) {
        aPosition = position
    }

    override fun cancelAudio() {
        filePath = null
    }

    private fun onQuestionSubmitted(
        lessonQuestion: LessonQuestion,
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
        adapter.notifyDataSetChanged()
    }

    override fun openNextScreen() {
        if (isAllQuestionsAttempted() && isVisible) {
            binding.vocabularyCompleteLayout.visibility = View.VISIBLE
            lessonActivityListener?.onSectionStatusUpdate(1, true)
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
        lessonActivityListener?.onNextTabCall(1)
    }

    fun onCloseDialog() {
        binding.vocabularyCompleteLayout.visibility = View.GONE
    }

    override fun submitPractice(lessonQuestion: LessonQuestion): Boolean {
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
                viewModel.getPointsForVocabAndReading(lessonQuestion.id)
                onQuestionSubmitted(lessonQuestion)
                openNextScreen()

                CoroutineScope(Dispatchers.Main).launch {
                    val requestEngage = RequestEngage()
//                requestEngage.text = binding.etPractise.text.toString()
                    requestEngage.localPath = lessonQuestion.filePath
                    requestEngage.duration =
                        Utils.getDurationOfMedia(requireActivity(), lessonQuestion.filePath)
                            ?.toInt()
                    //requestEngage.feedbackRequire = lessonQuestion.feedback_require
                    requestEngage.questionId = lessonQuestion.id
                    requestEngage.mentor = Mentor.getInstance().getId()
                    if (it == EXPECTED_ENGAGE_TYPE.AU || it == EXPECTED_ENGAGE_TYPE.VI || it == EXPECTED_ENGAGE_TYPE.DX) {
                        requestEngage.answerUrl = lessonQuestion.filePath
                    }
                    delay(1000)
                    viewModel.addTaskToService(requestEngage, PendingTask.VOCABULARY_PRACTICE)

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


    override fun onPause() {
        super.onPause()
        Timber.d("Sahil : onPause() Started")
//        adapter.itemList.forEachIndexed { index, lessonQuestion ->
//            if (lessonQuestion.type != LessonQuestionType.QUIZ)
//                binding.practiceRv.findViewHolderForAdapterPosition(index)?.let {
//                    (it as VocabularyPracticeAdapter.VocabularyViewHolder?)?.pauseAudio()
//                }
//        }
//        aPosition = -1
        Timber.d("Sahil : onPause() Completed")
    }

    override fun askRecordPermission() {

        PermissionUtils.audioRecordStorageReadAndWritePermission(
            requireActivity(),
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let {
                        if (report.isAnyPermissionPermanentlyDenied) {
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
            })
    }

    override fun focusChild(position: Int) {
        binding.practiceRv.smoothScrollToPosition(position)
    }

    private fun subscribeRXBus() {
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(SnackBarEvent::class.java)
                .subscribeOn(Schedulers.computation())
                .subscribe({
                    //if (it.questionId in chatModelList.) check for question Id later
                    showSnackBar(
                        binding.rootView,
                        Snackbar.LENGTH_LONG,
                        it.pointsSnackBarText.toString()
                    )
                }, {
                    it.printStackTrace()
                })
        )
    }

    override fun onResume() {
        super.onResume()
        Timber.d("Sahil : onResume() Started")
        subscribeRXBus()
        if (isVisible.not()) {
            adapter.itemList.forEachIndexed { index, lessonQuestion ->
                if (lessonQuestion.type != LessonQuestionType.QUIZ)
                    (binding.practiceRv.findViewHolderForAdapterPosition(index)
                            as VocabularyPracticeAdapter.VocabularyViewHolder).pauseAudio()
            }
        }
        Timber.d("Sahil : onResume() Completed")
    }

    override fun onStop() {
        super.onStop()
        Timber.d("Sahil : onStop() Started")
        compositeDisposable.clear()
        Timber.d("Sahil : onStop() Completed")
    }

    companion object {
        @JvmStatic
        fun getInstance() = VocabularyFragment()
    }

}
