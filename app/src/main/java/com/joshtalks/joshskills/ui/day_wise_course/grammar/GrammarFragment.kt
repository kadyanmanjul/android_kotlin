package com.joshtalks.joshskills.ui.day_wise_course.grammar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.service.DownloadUtils
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentGrammarLayoutBinding
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.server.assessment.QuestionStatus
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
import java.util.ArrayList

class GrammarFragment : Fragment() {

    private var currentQuizQuestion: Int = 0
    private var desExpanded: Boolean = false
    private var appAnalytics: AppAnalytics? = null
    private var chatModelList: ArrayList<ChatModel>? = null
    lateinit var binding: FragmentGrammarLayoutBinding

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
            chatModelList = arguments?.getParcelableArrayList<ChatModel>(PRACTISE_OBJECT)
        }
        if (chatModelList == null) {
            requireActivity().finish()
        }

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
                binding.grammarDescTv.maxLines = 1
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

    private fun addObserValbles() {
//        viewModel.getQuestions()
        assessmentQuestions.clear()
        viewModel.assessmentLiveData.observe(viewLifecycleOwner) { assessmentRelations ->
            assessmentRelations.questionList.sortedBy { it.question.sortOrder }
                .forEach { item -> assessmentQuestions.add(item) }

            if (assessmentQuestions.size > 0)
                updateQuiz(assessmentQuestions.get(0))
        }
    }

    private fun updateQuiz(question: AssessmentQuestionWithRelations) {
        binding.quizQuestionTv.text = question.question.text
        hideExplanation()
        binding.explanationTv.text = question.reviseConcept?.description
        binding.quizRadioGroup.check(-1)
        question.choiceList.forEachIndexed { index, choice ->
            when (index) {
                0 -> {
                    binding.option1.text = choice.text
                    binding.option1.setBackgroundColor(
                        ContextCompat.getColor(requireContext(), R.color.white)
                    )
                    if (choice.isCorrect)
                        binding.quizRadioGroup.tag = binding.option1.id
                }
                1 -> {
                    binding.option2.text = choice.text
                    binding.option2.setBackgroundColor(
                        ContextCompat.getColor(requireContext(), R.color.white)
                    )
                    if (choice.isCorrect)
                        binding.quizRadioGroup.tag = binding.option2.id
                }
                2 -> {
                    binding.option3.text = choice.text
                    binding.option3.setBackgroundColor(
                        ContextCompat.getColor(requireContext(), R.color.white)
                    )
                    if (choice.isCorrect)
                        binding.quizRadioGroup.tag = binding.option3.id
                }
                3 -> {
                    binding.option4.text = choice.text
                    binding.option4.setBackgroundColor(
                        ContextCompat.getColor(requireContext(), R.color.white)
                    )
                    if (choice.isCorrect)
                        binding.quizRadioGroup.tag = binding.option4.id
                }
            }
        }

        binding.submitAnswerBtn.visibility = View.VISIBLE
        binding.continueBtn.visibility = View.GONE

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appAnalytics = AppAnalytics.create(AnalyticsEvent.PDF_VH.NAME)
            .addBasicParam()
            .addUserDetails()
//            .addParam("ChatId", message.chatId)
    }


    fun setupUi(chatModel: ChatModel) {
        chatModel.question?.run {
            if (this.type == BASE_MESSAGE_TYPE.QUIZ) {
                this.assessmentId?.let {
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
                            }
                            binding.videoPlayer.downloadStreamButNotPlay()
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

                    BASE_MESSAGE_TYPE.TX -> {
                        this.qText?.let {
                            binding.grammarDescTv.visibility = View.VISIBLE
                            binding.grammarDescTv.text =
                                HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY)
                        }
                    }
                    BASE_MESSAGE_TYPE.QUIZ -> {
                        this.assessmentId?.let {
                            viewModel.fetchAssessmentDetails(it)
                        }
                    }
                    else -> {

                    }
                }

            /* if ((this.material_type == BASE_MESSAGE_TYPE.TX).not() && this.qText.isNullOrEmpty()
                     .not()
             ) {
                 binding.infoTv2.text =
                     HtmlCompat.fromHtml(this.qText!!, HtmlCompat.FROM_HTML_MODE_LEGACY)
                 binding.infoTv2.visibility = View.VISIBLE
             }

             if (this.practiceEngagement.isNullOrEmpty()) {
                 binding.submitAnswerBtn.visibility = View.VISIBLE
                 setViewAccordingExpectedAnswer(chatModel)
             } else {
                 hidePracticeInputLayout()
                 binding.submitAnswerBtn.visibility = View.GONE
                 setViewUserSubmitAnswer(chatModel)
             }*/

        }
    }

    fun onQuestionSubmit() {
        if (binding.quizRadioGroup.tag is Int) {

            val question = assessmentQuestions.get(currentQuizQuestion)
            question.question.isAttempted = true
            question.question.status =
                evaluateQuestionStatus((binding.quizRadioGroup.tag as Int) == binding.quizRadioGroup.checkedRadioButtonId)

            viewModel.saveAssessmentQuestion(question)
            viewModel.updateQuestionStatus(QUESTION_STATUS.AT.name, question.question.remoteId)

            binding.quizRadioGroup.findViewById<RadioButton>(binding.quizRadioGroup.tag as Int)
                .setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.bg_green_80)
                )

            binding.continueBtn.visibility = View.VISIBLE
            binding.showExplanationBtn.isEnabled = true
        }
    }

    private fun evaluateQuestionStatus(status: Boolean): QuestionStatus {
        return if (status) QuestionStatus.CORRECT
        else QuestionStatus.WRONG

    }

    fun onContinueClick() {
        if (assessmentQuestions.size - 1 > currentQuizQuestion) {
            updateQuiz(assessmentQuestions.get(++currentQuizQuestion))
        } else {

        }
    }


    fun hideExplanation() {
        binding.explanationLbl.visibility = View.GONE
        binding.explanationTv.visibility = View.GONE
    }

    fun showExplanation() {
        binding.showExplanationBtn.isEnabled = false
        binding.explanationLbl.visibility = View.VISIBLE
        binding.explanationTv.visibility = View.VISIBLE
        binding.explanationTv.requestFocus()
        binding.explanationTv.parent.requestChildFocus(binding.explanationTv, binding.explanationTv)
//        binding.grammarScrollView.scrollTo(0, binding.explanationTv.y.toInt())
    }

    private fun setUpPdfView(message: ChatModel) {
        message.question?.let {
            it.pdfList?.getOrNull(0)?.let { pdfObj ->
                try {
                    if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADING) {
                        fileDownloadingInProgressView()
                        download(pdfObj.url)
                    } else if (message.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED && AppDirectory.isFileExist(
                            pdfObj.downloadedLocalPath
                        )
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
        message?.question?.pdfList?.getOrNull(0)?.let { pdfType ->
            binding.additionalMaterialTv.setOnClickListener {
                PdfViewerActivity.startPdfActivity(
                    requireContext(),
                    pdfType.id,
                    EMPTY
                )

            }
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

}