package com.joshtalks.joshskills.ui.day_wise_course.grammar

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.service.DownloadUtils
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentGrammarLayoutBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.repository.local.entity.Question
import com.joshtalks.joshskills.repository.local.eventbus.MediaProgressEventBus
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.repository.server.assessment.QuestionStatus
import com.joshtalks.joshskills.ui.day_wise_course.CapsuleActivityCallback
import com.joshtalks.joshskills.ui.day_wise_course.CapsuleViewModel
import com.joshtalks.joshskills.ui.day_wise_course.practice.PRACTISE_OBJECT
import com.joshtalks.joshskills.ui.pdfviewer.PdfViewerActivity
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2core.DownloadBlock
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.util.ArrayList

class GrammarFragment : Fragment() {

    private var quizQuestion: Question? = null
    private var currentQuizQuestion: Int = 0
    private var desExpanded: Boolean = false
    private var appAnalytics: AppAnalytics? = null
    private var chatModelList: ArrayList<ChatModel>? = null
    lateinit var binding: FragmentGrammarLayoutBinding
    private val compositeDisposable = CompositeDisposable()
    private var correctAns = 0
    var activityCallback: CapsuleActivityCallback? = null
    var assessmentQuestions: ArrayList<AssessmentQuestionWithRelations> = ArrayList()

    private val viewModel: CapsuleViewModel by lazy {
        ViewModelProvider(this).get(CapsuleViewModel::class.java)
    }

    private var message: ChatModel? = null

    companion object {
        @JvmStatic
        fun instance(chatModelList: ArrayList<ChatModel>) = GrammarFragment().apply {
            arguments = Bundle().apply {
                putParcelableArrayList(PRACTISE_OBJECT, chatModelList)
            }
        }
    }

    private var downloadListener = object : FetchListener {
        override fun onAdded(download: Download) {

        }

        override fun onCancelled(download: Download) {
            DownloadUtils.removeCallbackListener(download.tag)
            message?.downloadStatus = DOWNLOAD_STATUS.FAILED
            fileNotDownloadView()

        }

        override fun onCompleted(download: Download) {
            DownloadUtils.removeCallbackListener(download.tag)
            message?.downloadStatus = DOWNLOAD_STATUS.DOWNLOADED
            fileDownloadSuccess()
        }

        override fun onDeleted(download: Download) {

        }

        override fun onDownloadBlockUpdated(
            download: Download,
            downloadBlock: DownloadBlock,
            totalBlocks: Int
        ) {

        }

        override fun onError(download: Download, error: Error, throwable: Throwable?) {
            DownloadUtils.removeCallbackListener(download.tag)
            message?.downloadStatus = DOWNLOAD_STATUS.FAILED
            fileNotDownloadView()

        }

        override fun onPaused(download: Download) {

        }

        override fun onProgress(
            download: Download,
            etaInMilliSeconds: Long,
            downloadedBytesPerSecond: Long
        ) {

        }

        override fun onQueued(download: Download, waitingOnNetwork: Boolean) {

        }

        override fun onRemoved(download: Download) {

        }

        override fun onResumed(download: Download) {

        }

        override fun onStarted(
            download: Download,
            downloadBlocks: List<DownloadBlock>,
            totalBlocks: Int
        ) {
            message?.downloadStatus = DOWNLOAD_STATUS.DOWNLOADING
            fileDownloadingInProgressView()

        }

        override fun onWaitingNetwork(download: Download) {

        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            chatModelList = arguments?.getParcelableArrayList(PRACTISE_OBJECT)
        }
        if (chatModelList == null) {
            requireActivity().finish()
        }

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is CapsuleActivityCallback)
            activityCallback = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_grammar_layout, container, false)
        binding.handler = this

        binding.expandIv.setOnClickListener {
            if (desExpanded) {
                binding.grammarDescTv.maxLines = 2
                binding.expandIv.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.arrow_down
                    )
                )
            } else {
                binding.grammarDescTv.maxLines = 100
                binding.expandIv.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.arrow_up
                    )
                )
            }
            desExpanded = desExpanded.not()
        }

        addObserValbles()
        chatModelList?.forEach { setupUi(it) }

        return binding.root
    }


    override fun onPause() {
        binding.videoPlayer.onPause()
        super.onPause()
    }

    private fun addObserValbles() {
//        viewModel.getQuestions()
        assessmentQuestions.clear()
        viewModel.assessmentLiveData.observe(viewLifecycleOwner) { assessmentRelations ->
            assessmentRelations.questionList.sortedBy { it.question.sortOrder }
                .forEach { item -> assessmentQuestions.add(item) }

            if (assessmentQuestions.size > 0) {
                binding.quizRadioGroup.setOnCheckedChangeListener { radioGroup: RadioGroup, checkedId: Int ->
                    showQuizUi()
                    requestFocus(binding.submitAnswerBtn)
                }

                updateQuiz(assessmentQuestions.get(0))
                if (quizQuestion?.status == QUESTION_STATUS.AT) {
                    showQuizCompleteLayout()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appAnalytics = AppAnalytics.create(AnalyticsEvent.PDF_VH.NAME)
            .addBasicParam()
            .addUserDetails()
    }


    fun setupUi(chatModel: ChatModel) {
        chatModel.question?.run {
            if (this.type == BASE_MESSAGE_TYPE.QUIZ) {
                binding.quizTv.text = AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.TODAYS_QUIZ_TITLE)
                this.assessmentId?.let {
                    quizQuestion = this
                    viewModel.fetchAssessmentDetails(it)
                }
            } else
                when (this.material_type) {
                    BASE_MESSAGE_TYPE.VI -> {
                        binding.videoPlayer.visibility = View.VISIBLE
                        this.videoList?.getOrNull(0)?.video_url?.let {
                            binding.videoPlayer.setUrl(it)
                            binding.videoPlayer.fitToScreen()
                            binding.videoPlayer.setPlayListener {
                                val videoId = this.videoList?.getOrNull(0)?.id
                                val videoUrl = this.videoList?.getOrNull(0)?.video_url
                                VideoPlayerActivity.startVideoActivity(
                                    requireContext(),
                                    "",
                                    videoId,
                                    videoUrl
                                )

                                updateVideoQuestionStatus(this)

                            }
                            binding.videoPlayer.downloadStreamButNotPlay()
                        }

                        if (this.status == QUESTION_STATUS.NA) {

                            binding.quizShader.visibility = View.VISIBLE
                            compositeDisposable.add(
                                RxBus2.listen(MediaProgressEventBus::class.java)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe({ eventBus ->
                                        if (eventBus.progress >= 3000) {
                                            updateVideoQuestionStatus(this)
                                            binding.startQuizBtn.alpha = 1f
                                        }
                                    }, {
                                        it.printStackTrace()
                                    })
                            )

                        } else {
                            binding.quizShader.visibility = View.GONE
                        }

                        this.qText?.let {
                            binding.grammarDescTv.visibility = View.VISIBLE
                            binding.grammarDescTv.text =
                                HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY)
                        }
                    }
                    BASE_MESSAGE_TYPE.PD -> {
                        message = chatModel
                        binding.additionalMaterialTv.visibility = View.VISIBLE
                        binding.additionalMaterialTv.text = this.title
                        this.pdfList?.getOrNull(0)?.let { pdfType ->
                            binding.additionalMaterialTv.setOnClickListener {
                                PdfViewerActivity.startPdfActivity(
                                    requireContext(),
                                    pdfType.id,
                                    EMPTY
                                )

                            }
                        }
                        setUpPdfView(chatModel)
                    }

                    BASE_MESSAGE_TYPE.QUIZ -> {
                        this.assessmentId?.let {
                            viewModel.fetchAssessmentDetails(it)
                        }
                    }
                    else -> {

                    }
                }
        }
    }

    private fun updateQuiz(question: AssessmentQuestionWithRelations) {
        binding.quizQuestionTv.text = getString(
            R.string.ques_short_form,
            currentQuizQuestion + 1,
            assessmentQuestions.size, question.question.text
        )

        if (currentQuizQuestion == 0)
            binding.previousQuestionIv.visibility = View.GONE
        else
            binding.previousQuestionIv.visibility = View.VISIBLE

        if (assessmentQuestions.size - 1 == currentQuizQuestion)
            binding.nextQuestionIv.visibility = View.GONE
        else
            binding.nextQuestionIv.visibility = View.VISIBLE

        hideExplanation()
        binding.explanationTv.text = question.reviseConcept?.description
        binding.quizRadioGroup.check(-1)
        question.choiceList.forEachIndexed { index, choice ->
            when (index) {
                0 -> {
                    setupOption(binding.option1, choice, question)
                }
                1 -> {
                    setupOption(binding.option2, choice, question)
                }
                2 -> {
                    setupOption(binding.option3, choice, question)
                }
                3 -> {
                    setupOption(binding.option4, choice, question)
                }
            }
        }

        binding.submitAnswerBtn.isEnabled = false
        binding.continueBtn.visibility = View.GONE
        if (question.question.isAttempted) {
            binding.submitAnswerBtn.visibility = View.GONE
            binding.showExplanationBtn.visibility = View.VISIBLE
        } else {
            binding.showExplanationBtn.visibility = View.GONE
            binding.submitAnswerBtn.visibility = View.VISIBLE
        }

    }

    private fun setupOption(
        radioButton: RadioButton,
        choice: Choice,
        question: AssessmentQuestionWithRelations
    ) {
        radioButton.text = choice.text
        if (question.question.isAttempted) {
            radioButton.isClickable = false
            if (choice.userSelectedOrder == 1) {
                radioButton.isChecked = true
                if (choice.isCorrect) {
                    radioButton.setBackgroundResource(R.drawable.rb_correct_rect_bg)
                    radioButton.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        0,
                        R.drawable.ic_green_tick,
                        0
                    )
                    radioButton.elevation = 8F
                } else {
                    radioButton.setBackgroundColor(
                        ContextCompat.getColor(requireContext(), R.color.received_bg_BC)
                    )
                    radioButton.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        0,
                        R.drawable.ic_green_tick,
                        0
                    )
                }
            } else if (choice.isCorrect) {
                radioButton.setBackgroundResource(R.drawable.rb_correct_rect_bg)
                radioButton.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ic_green_tick,
                    0
                )
                radioButton.elevation = 8F
            } else {
                radioButton.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.white)
                )
                radioButton.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    0,
                    0
                )
                radioButton.elevation = 0F
            }
        } else {
            radioButton.isClickable = true
            radioButton.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.white)
            )
            radioButton.elevation = 0F
            radioButton.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                0,
                0
            )
        }
        if (choice.isCorrect)
            binding.quizRadioGroup.tag = radioButton.id
    }

    private fun showQuizUi() {
        binding.questionNavigateRl.visibility = View.VISIBLE
        binding.quizTv.visibility = View.VISIBLE
        binding.quizQuestionTv.visibility = View.VISIBLE
        binding.quizRadioGroup.visibility = View.VISIBLE
        binding.submitAnswerBtn.isEnabled = true
        binding.submitAnswerBtn.visibility = View.VISIBLE
    }

    fun onQuestionSubmit() {
        if (binding.quizRadioGroup.tag is Int) {

            val question = assessmentQuestions.get(currentQuizQuestion)
            question.question.isAttempted = true
            question.question.status =
                evaluateQuestionStatus((binding.quizRadioGroup.tag as Int) == binding.quizRadioGroup.checkedRadioButtonId)

            val selectedChoice = question.choiceList[binding.quizRadioGroup.indexOfChild(
                binding.root.findViewById(binding.quizRadioGroup.checkedRadioButtonId)
            )]
            selectedChoice.isSelectedByUser = true
            selectedChoice.userSelectedOrder = 1

            viewModel.saveAssessmentQuestion(question)
            if (currentQuizQuestion == assessmentQuestions.size - 1)
                activityCallback?.onQuestionStatusUpdate(
                    QUESTION_STATUS.AT.name,
                    quizQuestion?.questionId?.toIntOrNull() ?: 0
                )

            binding.quizRadioGroup.findViewById<RadioButton>(binding.quizRadioGroup.tag as Int)
                .setBackgroundResource(R.drawable.rb_correct_rect_bg)

            if (binding.quizRadioGroup.tag as Int == binding.quizRadioGroup.checkedRadioButtonId) {
                correctAns++
            }

            updateQuiz(question)
            binding.continueBtn.visibility = View.VISIBLE
            binding.showExplanationBtn.visibility = View.VISIBLE
            requestFocus(binding.showExplanationBtn)
        }
    }

    fun onStartQuizClick() {
        binding.quizShader.visibility = View.GONE
        showQuizUi()
    }

    private fun evaluateQuestionStatus(status: Boolean): QuestionStatus {
        return if (status) QuestionStatus.CORRECT
        else QuestionStatus.WRONG

    }

    fun onContinueClick() {
        if (assessmentQuestions.size - 1 > currentQuizQuestion) {
            updateQuiz(assessmentQuestions.get(++currentQuizQuestion))
        } else {
            showQuizCompleteLayout()
        }
    }

    fun onGrammarContinueClick() {
        activityCallback?.onNextTabCall(binding.continueBtn)
    }

    fun onRedoQuizClick() {
        assessmentQuestions.forEach { question ->
            question.question.isAttempted = false
            question.question.status = QuestionStatus.NONE
            viewModel.saveAssessmentQuestion(question)
        }
        currentQuizQuestion = 0
        updateQuiz(assessmentQuestions[0])
        binding.grammarCompleteLayout.visibility = View.GONE

        quizQuestion?.let {

            it.status = QUESTION_STATUS.NA
            viewModel.updateQuestionInLocal(it)

            viewModel.updateQuestionStatus(
                QUESTION_STATUS.NA.name,
                it.questionId.toInt(),
                it.course_id,
                it.lesson_id
            )
        }


    }

    private fun showQuizCompleteLayout() {
        binding.grammarCompleteLayout.visibility = View.VISIBLE
        binding.submitAnswerBtn.isEnabled = false
        binding.continueBtn.visibility = View.GONE
        binding.showExplanationBtn.visibility = View.GONE
        hideExplanation()
        binding.marksTv.text = getString(R.string.marks_text, correctAns, assessmentQuestions.size)
    }

    private fun hideExplanation() {
        binding.explanationLbl.visibility = View.GONE
        binding.explanationTv.visibility = View.GONE
        binding.showExplanationBtn.text = getString(R.string.show_explanation)
    }

    fun showExplanation() {
        if (binding.explanationLbl.visibility == View.VISIBLE) {
            binding.showExplanationBtn.text = getString(R.string.show_explanation)
            binding.explanationLbl.visibility = View.GONE
            binding.explanationTv.visibility = View.GONE
        } else {
            binding.showExplanationBtn.text = getString(R.string.hide_explanation)
            binding.explanationLbl.visibility = View.VISIBLE
            binding.explanationTv.visibility = View.VISIBLE
            binding.explanationTv.requestFocus()
            requestFocus(binding.explanationTv)
        }
    }

    private fun setUpPdfView(message: ChatModel) {
        message.question?.let {
            it.pdfList?.getOrNull(0)?.let { pdfObj ->
                try {
                    if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADING) {
                        fileDownloadingInProgressView()
                        download(pdfObj.url)
                    } else if (PermissionUtils.isStoragePermissionEnabled(requireContext()) && AppDirectory.getFileSize(
                            File(
                                AppDirectory.docsReceivedFile(pdfObj.url).absolutePath
                            )
                        ) > 0
                    ) {
                        fileDownloadSuccess()
                    } else {
                        fileNotDownloadView()
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }

    private fun fileDownloadSuccess() {
        binding.ivStartDownload.visibility = android.view.View.GONE
        binding.progressDialog.visibility = android.view.View.GONE
        binding.ivCancelDownload.visibility = android.view.View.GONE
    }

    private fun fileNotDownloadView() {
        appAnalytics?.addParam(AnalyticsEvent.PDF_VIEW_STATUS.NAME, "Not downloaded")
        binding.ivStartDownload.visibility = android.view.View.VISIBLE
        binding.progressDialog.visibility = android.view.View.GONE
        binding.ivCancelDownload.visibility = android.view.View.GONE
    }

    private fun fileDownloadingInProgressView() {
        binding.ivStartDownload.visibility = android.view.View.GONE
        binding.progressDialog.visibility = android.view.View.VISIBLE
        binding.ivCancelDownload.visibility = android.view.View.VISIBLE
    }


    private fun requestFocus(view: View) {
        view.parent.requestChildFocus(
            view,
            view
        )
    }

    fun showNextQuestion() {
        updateQuiz(assessmentQuestions.get(++currentQuizQuestion))
    }

    fun showPreviousQuestion() {
        updateQuiz(assessmentQuestions.get(--currentQuizQuestion))
    }

    fun onClickPdfContainer() {
        if (PermissionUtils.isStoragePermissionEnabled(requireActivity())) {
            PermissionUtils.storageReadAndWritePermission(
                requireActivity(),
                object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.areAllPermissionsGranted()?.let { flag ->
                            if (flag) {
                                appAnalytics?.addParam(
                                    AnalyticsEvent.PDF_VIEW_STATUS.NAME,
                                    "pdf Opened"
                                )?.push()
                                openPdf()
                                return

                            }
                            if (report.isAnyPermissionPermanentlyDenied) {
                                PermissionUtils.permissionPermanentlyDeniedDialog(
                                    requireActivity()
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
            return
        }
        openPdf()
    }

    private fun openPdf() {
        if (PermissionUtils.isStoragePermissionEnabled(requireContext()).not()) {
            askStoragePermission()
            return
        }
        message?.question?.pdfList?.getOrNull(0)?.let { pdfType ->
            PdfViewerActivity.startPdfActivity(
                context = requireContext(),
                pdfId = pdfType.id,
                courseName = message!!.question!!.title!!,
                pdfPath = AppDirectory.docsReceivedFile(pdfType.url).absolutePath
            )
        }

    }

    fun downloadCancel() {
        fileNotDownloadView()
        message?.downloadStatus = DOWNLOAD_STATUS.NOT_START

    }

    fun downloadStart() {
        if (message?.downloadStatus == DOWNLOAD_STATUS.DOWNLOADING) {
            return
        }
        download(message?.url)
    }

    fun askStoragePermission() {

        PermissionUtils.storageReadAndWritePermission(
            requireActivity(),
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
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

    private fun download(url: String?) {

        if (PermissionUtils.isStoragePermissionEnabled(requireContext()).not()) {
            askStoragePermission()
            return
        }
        message?.question?.pdfList?.let {
            if (it.size > 0) {
                DownloadUtils.downloadFile(
                    it.get(0).url,
                    AppDirectory.docsReceivedFile(it.get(0).url).absolutePath,
                    message!!.chatId,
                    message!!,
                    downloadListener
                )
            } else if (BuildConfig.DEBUG) {
                showToast("Pdf size is 0")
            }
        }
    }

    private fun updateVideoQuestionStatus(question: Question) {
        activityCallback?.onQuestionStatusUpdate(
            QUESTION_STATUS.AT.name,
            question.questionId.toIntOrNull() ?: 0
        )
        question.status = QUESTION_STATUS.AT
        viewModel.updateQuestionInLocal(question)
    }

    override fun onDestroy() {
        compositeDisposable.dispose()
        super.onDestroy()
    }

}