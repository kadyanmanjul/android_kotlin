package com.joshtalks.joshskills.ui.day_wise_course.practice

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.databinding.FragmentPraticeBinding
import com.joshtalks.joshskills.repository.local.entity.*
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.RequestEngage
import com.joshtalks.joshskills.ui.day_wise_course.CapsuleActivityCallback
import com.joshtalks.joshskills.ui.practise.PracticeViewModel
import com.joshtalks.joshskills.util.FileUploadService
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

const val PRACTISE_OBJECT = "practise_object"

class NewPracticeFragment : CoreJoshFragment(), PracticeAdapter.PracticeClickListeners {
    private var currentPlayingPosition: Int = -1
    private lateinit var adapter: PracticeAdapter

    private lateinit var binding: FragmentPraticeBinding
    private var chatModelList: ArrayList<ChatModel>? = null
    private var isAudioRecordDone = false
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
        fun instance(chatModelList: ArrayList<ChatModel>) = NewPracticeFragment().apply {
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
                DataBindingUtil.inflate(inflater, R.layout.fragment_pratice, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        adapter = PracticeAdapter(requireContext(), practiceViewModel, chatModelList!!, this)
        binding.practiceRv.layoutManager = LinearLayoutManager(requireContext())
        binding.practiceRv.adapter = adapter

        addObserver()
        binding.progressLayout.setOnClickListener {

        }

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
    }

    override fun submitQuiz(chatModel: ChatModel) {
        onQuestionSubmitted(chatModel)
    }

    private fun onQuestionSubmitted(chatModel: ChatModel) {

        CoroutineScope(Dispatchers.IO).launch {
            chatModel.question?.interval?.run {
                WorkManagerAdmin.determineNPAEvent(NPSEvent.PRACTICE_COMPLETED, this)
            }
        }
        activityCallback?.onQuestionStatusUpdate(
                QUESTION_STATUS.AT,
                chatModel.question?.questionId?.toIntOrNull() ?: 0
        )

        chatModel.question?.status = QUESTION_STATUS.AT

        currentChatModel = null

        adapter.notifyDataSetChanged()
    }

    override fun openNextScreen() {
        var openNextScreen = true
        chatModelList?.forEach { item ->
            if (item.question?.status == QUESTION_STATUS.NA) {
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
                onQuestionSubmitted(chatModel)
                openNextScreen()
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
                filePath = AppDirectory.getAudioSentFile(null).absolutePath
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
}
