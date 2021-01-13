package com.joshtalks.joshskills.ui.day_wise_course.vocabulary

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshFragment
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentVocabularyBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.EXPECTED_ENGAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.PendingTask
import com.joshtalks.joshskills.repository.local.entity.PendingTaskModel
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.SnackBarEvent
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentWithRelations
import com.joshtalks.joshskills.repository.server.RequestEngage
import com.joshtalks.joshskills.ui.day_wise_course.CapsuleActivityCallback
import com.joshtalks.joshskills.ui.practise.PracticeViewModel
import com.joshtalks.joshskills.util.FileUploadService
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val PRACTISE_OBJECT = "practise_object"

class VocabularyFragment : CoreJoshFragment(), VocabularyPracticeAdapter.PracticeClickListeners {
    private lateinit var adapter: VocabularyPracticeAdapter

    private var compositeDisposable = CompositeDisposable()

    private lateinit var binding: FragmentVocabularyBinding
    private var chatModelList: ArrayList<ChatModel>? = null
    private var isVideoRecordDone = false
    private var isDocumentAttachDone = false
    private var startTime: Long = 0
    private var totalTimeSpend: Long = 0
    private var filePath: String? = null
    private var currentChatModel: ChatModel? = null

    private var activityCallback: CapsuleActivityCallback? = null

    private val practiceViewModel: PracticeViewModel by lazy {
        ViewModelProvider(this).get(PracticeViewModel::class.java)
    }

    companion object {
        @JvmStatic
        fun instance(chatModelList: ArrayList<ChatModel>) = VocabularyFragment().apply {
            arguments = Bundle().apply {
                putParcelableArrayList(PRACTISE_OBJECT, chatModelList)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is CapsuleActivityCallback)
            activityCallback = context
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            chatModelList = arguments?.getParcelableArrayList(PRACTISE_OBJECT)
        }
        if (chatModelList == null) {
            requireActivity().finish()
        }
        totalTimeSpend = System.currentTimeMillis()

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_vocabulary, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        binding.progressLayout.setOnClickListener {

        }

        addObserver()
        practiceViewModel.getAssessmentData(chatModelList)
        binding.vocabularyCompletedTv.text = AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.VOCABULARY_COMPLETED)
        return binding.root
    }

    private fun addObserver() {
        practiceViewModel.requestStatusLiveData.observe(viewLifecycleOwner, {
            if (it) {
//                onPracticeSubmitted()
            }
            binding.progressLayout.visibility = View.GONE
        })

        practiceViewModel.assessmentData.observe(viewLifecycleOwner, {
            initAdapter(it)
        })

        practiceViewModel.practiceEngagementData.observe(viewLifecycleOwner, Observer {
            if (it.pointsList.isNullOrEmpty().not()) {
                showSnackBar(binding.rootView, Snackbar.LENGTH_LONG, it.pointsList?.get(0))
            }
        })
    }

    private fun initAdapter(arrayList: ArrayList<AssessmentWithRelations>) {
        val itemSize =
            chatModelList?.filter { it.question?.type == BASE_MESSAGE_TYPE.QUIZ }?.size ?: 0
        var quizSort = 1
        var wordSort = 1
        chatModelList?.forEach {
            it.question?.let {
                if (it.type == BASE_MESSAGE_TYPE.QUIZ) {
                    it.vocabOrder = quizSort
                    quizSort = quizSort.plus(1)
                } else {
                    it.vocabOrder = wordSort
                    wordSort = wordSort.plus(1)
                }
            }
        }
        adapter = VocabularyPracticeAdapter(
            requireContext(),
            practiceViewModel,
            chatModelList!!,
            this,
            quizsItemSize = itemSize,
            arrayList
        )
        chatModelList?.let {
            it.sortedBy { it.question?.vpSortOrder }
        }
        binding.practiceRv.layoutManager = LinearLayoutManager(requireContext())
        binding.practiceRv.adapter = adapter
    }

    override fun submitQuiz(chatModel: ChatModel, isCorrect: Boolean, questionId: Int) {
        onQuestionSubmitted(chatModel, isCorrect, questionId)
        openNextScreen()
    }

    override fun quizOptionSelected(chatModel: ChatModel) {
        onQuestionChoiceSelected(chatModel)
    }

    private fun onQuestionSubmitted(
        chatModel: ChatModel,
        isCorrect: Boolean = false,
        questionId: Int = -1
    ) {

        if (isCorrect && questionId != -1) {
            val quizQuestion = arrayListOf<Int>()
            quizQuestion.add(questionId)
            activityCallback?.onQuestionStatusUpdate(
                QUESTION_STATUS.AT,
                chatModel.question?.questionId?.toIntOrNull() ?: 0,
                quizCorrectQuestionIds = quizQuestion
            )
        } else {
            activityCallback?.onQuestionStatusUpdate(
                QUESTION_STATUS.AT,
                chatModel.question?.questionId?.toIntOrNull() ?: 0
            )
        }

        chatModel.question?.status = QUESTION_STATUS.AT

        currentChatModel = null

        adapter.notifyDataSetChanged()
    }

    private fun onQuestionChoiceSelected(chatModel: ChatModel) {
        activityCallback?.onQuestionStatusUpdate(
            QUESTION_STATUS.IP,
            chatModel.question?.questionId?.toIntOrNull() ?: 0
        )
        chatModel.question?.status = QUESTION_STATUS.IP
        currentChatModel = null
        adapter.notifyDataSetChanged()
    }

    override fun openNextScreen() {
        var openNextScreen = true
        chatModelList?.forEach { item ->
            if (item.question?.status != QUESTION_STATUS.AT) {
                openNextScreen = false
                return@forEach
            }
        }

        if (openNextScreen && isVisible) {
            binding.vocabularyCompleteLayout.visibility = View.VISIBLE
            activityCallback?.onSectionStatusUpdate(1, true)
        }
    }

    fun onContinueClick() {
        activityCallback?.onNextTabCall(2)
    }

    fun onCloseDialog() {
        binding.vocabularyCompleteLayout.visibility = View.GONE
    }

    override fun submitPractice(chatModel: ChatModel): Boolean {
        if (chatModel.question != null && chatModel.question!!.expectedEngageType != null) {
            chatModel.question?.expectedEngageType?.let {
                if (EXPECTED_ENGAGE_TYPE.TX == it) {
                    showToast(getString(R.string.submit_practise_msz))
                    return false
                } else if (EXPECTED_ENGAGE_TYPE.AU == it && chatModel.filePath == null) {
                    showToast(getString(R.string.submit_practise_msz))
                    return false
                } else if (EXPECTED_ENGAGE_TYPE.VI == it && isVideoRecordDone.not()) {
                    showToast(getString(R.string.submit_practise_msz))
                    return false
                } else if (EXPECTED_ENGAGE_TYPE.DX == it && isDocumentAttachDone.not()) {
                    showToast(getString(R.string.submit_practise_msz))
                    return false
                }
                onQuestionSubmitted(chatModel)
                openNextScreen()

                currentChatModel = chatModel
                val requestEngage = RequestEngage()
//                requestEngage.text = binding.etPractise.text.toString()
                requestEngage.localPath = chatModel.filePath
                requestEngage.duration =
                    Utils.getDurationOfMedia(requireActivity(), chatModel.filePath)?.toInt()
                requestEngage.feedbackRequire = chatModel.question?.feedback_require
                requestEngage.questionId = chatModel.question?.questionId!!
                requestEngage.mentor = Mentor.getInstance().getId()
                if (it == EXPECTED_ENGAGE_TYPE.AU || it == EXPECTED_ENGAGE_TYPE.VI || it == EXPECTED_ENGAGE_TYPE.DX) {
                    requestEngage.answerUrl = chatModel.filePath
                }

                chatModel.question!!.status = QUESTION_STATUS.IP

                CoroutineScope(Dispatchers.IO).launch {
                    val insertedId =
                        AppObjectController.appDatabase.pendingTaskDao().insertPendingTask(
                            PendingTaskModel(requestEngage, PendingTask.VOCABULARY_PRACTICE)
                        )
                    FileUploadService.uploadSinglePendingTasks(
                        AppObjectController.joshApplication,
                        insertedId
                    )

                }
                return true
            }
        }
        return false
    }


    override fun startRecording(chatModel: ChatModel, position: Int, startTimeUnit: Long) {
        this.startTime = startTimeUnit
        if (isAdded && activity != null)
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        practiceViewModel.startRecordAudio(null)
    }

    override fun stopRecording(chatModel: ChatModel, position: Int, stopTime: Long) {
        practiceViewModel.stopRecordingAudio(false)
        if (isAdded && activity != null)
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val timeDifference =
            TimeUnit.MILLISECONDS.toSeconds(stopTime) - TimeUnit.MILLISECONDS.toSeconds(
                startTime
            )
        if (timeDifference > 1) {
            practiceViewModel.recordFile?.let {
//                                isAudioRecordDone = true
                filePath = AppDirectory.getAudioSentFile(null, audioExtension = ".m4a").absolutePath
                chatModel.filePath = filePath
                AppDirectory.copy(it.absolutePath, filePath!!)
            }

        }
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
                    //Log.d("Manjul", "subscribeRXBus() called")
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
        subscribeRXBus()
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }
}
