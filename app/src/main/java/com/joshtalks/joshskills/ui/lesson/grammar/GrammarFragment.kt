package com.joshtalks.joshskills.ui.lesson.grammar

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.children
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.service.DownloadUtils
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentGrammarLayoutBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.CHAT_TYPE
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.entity.LessonMaterialType
import com.joshtalks.joshskills.repository.local.entity.LessonQuestion
import com.joshtalks.joshskills.repository.local.entity.LessonQuestionType
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.MediaProgressEventBus
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.repository.server.assessment.QuestionStatus
import com.joshtalks.joshskills.ui.lesson.LessonActivityListener
import com.joshtalks.joshskills.ui.lesson.LessonViewModel
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

class GrammarFragment : Fragment(), ViewTreeObserver.OnScrollChangedListener {

    lateinit var binding: FragmentGrammarLayoutBinding
    private var lessonActivityListener: LessonActivityListener? = null
    private val viewModel: LessonViewModel by lazy {
        ViewModelProvider(requireActivity()).get(LessonViewModel::class.java)
    }
    private var appAnalytics: AppAnalytics? = null

    private val compositeDisposable = CompositeDisposable()
    private var pdfQuestion: LessonQuestion? = null
    private var quizQuestion: LessonQuestion? = null
    private var currentQuizQuestion: Int = 0
    private var correctAns = 0
    private var assessmentQuestions: ArrayList<AssessmentQuestionWithRelations> = ArrayList()

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
            DataBindingUtil.inflate(inflater, R.layout.fragment_grammar_layout, container, false)
        binding.handler = this
        binding.grammarScrollView.layoutTransition?.setAnimateParentHierarchy(false)

        binding.grammarScrollView.viewTreeObserver.addOnScrollChangedListener(this)
        binding.expandIv.setOnClickListener {
            if (binding.grammarDescTv.maxLines == COLLAPSED_DESCRIPTION_MAX_LINES) {
                binding.grammarDescTv.maxLines = EXPANDED_DESCRIPTION_MAX_LINES
                binding.expandIv.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.arrow_up
                    )
                )
            } else {
                binding.grammarDescTv.maxLines = COLLAPSED_DESCRIPTION_MAX_LINES
                binding.expandIv.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.arrow_down
                    )
                )
            }
        }
        setObservers()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appAnalytics = AppAnalytics.create(AnalyticsEvent.PDF_VH.NAME)
            .addBasicParam()
            .addUserDetails()
    }

    override fun onPause() {
        binding.videoPlayer.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        compositeDisposable.dispose()
        super.onDestroy()
    }

    private fun setObservers() {
        viewModel.lessonQuestionsLiveData.observe(viewLifecycleOwner, Observer {
            binding.practiceTitleTv.text =
                getString(
                    R.string.today_lesson,
                    viewModel.lessonLiveData.value?.lessonName
                )

            val grammarQuestions = it.filter { it.chatType == CHAT_TYPE.GR }
//            if (grammarQuestions.isNullOrEmpty()) {
//                requireActivity().finish()
//            }
            grammarQuestions.forEach {
                setupUi(it)
            }

        })

        viewModel.grammarAssessmentLiveData.observe(owner = viewLifecycleOwner) { assessmentRelations ->
            assessmentQuestions.clear()
            assessmentRelations.questionList.sortedBy { it.question.sortOrder }.let {
                assessmentQuestions.addAll(it)
            }

            if (assessmentQuestions.size > 0) {
                binding.quizRadioGroup.setOnCheckedChangeListener(quizCheckedChangeListener)
                showQuizUi()
                updateQuiz(assessmentQuestions[0])

                if (quizQuestion?.status == QUESTION_STATUS.AT) {
                    setQuizScore(assessmentQuestions)
                    showQuizCompleteLayout()
                }
            }
        }
    }

    private fun setQuizScore(assessmentQuestions: ArrayList<AssessmentQuestionWithRelations>) {
        assessmentQuestions.forEach {
            it.choiceList.filter { choice -> choice.isCorrect && choice.isSelectedByUser }
                .forEach {
                    correctAns = correctAns.plus(1)
                }
        }
    }

    private fun setupUi(lessonQuestion: LessonQuestion) {

        if (lessonQuestion.type == LessonQuestionType.QUIZ) {
            // LessonQuestionType is QUIZ
            binding.quizTv.text = AppObjectController.getFirebaseRemoteConfig()
                .getString(FirebaseRemoteConfigKey.TODAYS_QUIZ_TITLE)
            lessonQuestion.assessmentId?.let {
                quizQuestion = lessonQuestion
                viewModel.fetchAssessmentDetails(it)
            }
        } else {
            // LessonQuestionType is Q
            when (lessonQuestion.materialType) {

                LessonMaterialType.VI -> {
                    binding.videoPlayer.visibility = View.VISIBLE
                    lessonQuestion.videoList?.getOrNull(0)?.video_url?.let {
                        val videoId = lessonQuestion.videoList?.getOrNull(0)?.id

                        binding.videoPlayer.setUrl(it)
                        binding.videoPlayer.setVideoId(videoId)
                        //binding.videoPlayer.setCourseId(course_id)
                        binding.videoPlayer.fitToScreen()
                        binding.videoPlayer.setPlayListener {

                            val videoUrl = it
                            VideoPlayerActivity.startVideoActivity(
                                requireContext(),
                                "",
                                videoId,
                                videoUrl
                            )
                        }
                        binding.videoPlayer.downloadStreamButNotPlay()

                        videoId?.toIntOrNull()?.let { id ->
                            viewModel.getMaxIntervalForVideo(id)
                        }
                        viewModel.grammarVideoInterval.observe(owner = this@GrammarFragment.viewLifecycleOwner) { graph ->
                            binding.videoPlayer.setProgress(graph?.endTime ?: 0)
                        }

                    }

                    if (lessonQuestion.status == QUESTION_STATUS.NA) {
                        binding.quizShader.visibility = View.VISIBLE
                    } else {
                        binding.quizShader.visibility = View.GONE
                    }

                    setUpVideoProgressListener(lessonQuestion)

                    lessonQuestion.qText?.let {
                        binding.grammarDescTv.visibility = View.VISIBLE
                        binding.grammarDescTv.text =
                            HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY)
                    }
                }

                LessonMaterialType.PD -> {
                    pdfQuestion = lessonQuestion
                    binding.additionalMaterialTv.visibility = View.VISIBLE
                    binding.additionalMaterialTv.text = lessonQuestion.title
                    setUpPdfView(lessonQuestion)
                }
            }
        }
    }

    private fun setUpVideoProgressListener(question: LessonQuestion) {
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(MediaProgressEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ mediaProgressEvent ->
                    if (mediaProgressEvent.progress > 3000 && question.status != QUESTION_STATUS.AT) {
                        question.status = QUESTION_STATUS.AT
                        question.isVideoWatchTimeSend = true
                        updateVideoQuestionStatus(question)
                    }
                    val videoPercent =
                        binding.videoPlayer.player?.duration?.let {
                            mediaProgressEvent.progress.div(
                                it
                            ).times(100).toInt()
                        } ?: -1
                    val percentVideoWatched =
                        mediaProgressEvent.watchTime.times(100).div(
                            binding.videoPlayer.player?.duration!!
                        ).toInt()

                    if (percentVideoWatched != 0 && percentVideoWatched >= 70 && videoPercent != -1 && videoPercent >= 70 && question.isVideoWatchTimeSend) {
                        updateVideoQuestionStatus(question, true)
                        question.isVideoWatchTimeSend = false
                    }

                    if (mediaProgressEvent.progress + 1000 >= question.videoList?.get(0)?.duration ?: 0) {
                        binding.quizShader.visibility = View.GONE
                        compositeDisposable.clear()
                        showScrollToBottomView()
                    }


                }, {
                    it.printStackTrace()
                })
        )

    }

    private fun showScrollToBottomView() {
        val view: View =
            binding.grammarScrollView.getChildAt(binding.grammarScrollView.childCount - 1)
        val bottomDetector: Int =
            view.bottom - (binding.grammarScrollView.height + binding.grammarScrollView.scrollY)
        if (bottomDetector == 0) {
            binding.scrollToBottomIv.visibility = View.GONE
        } else if (binding.grammarDescTv.maxLines > 2) {
            binding.scrollToBottomIv.visibility = View.VISIBLE
        }
    }

    fun scrollToBottom() {
        binding.grammarScrollView.scrollTo(0, binding.grammarScrollView.bottom)
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

    private fun resetRadioBackground(radioButton: RadioButton) {
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

    private val quizCheckedChangeListener =
        RadioGroup.OnCheckedChangeListener { radioGroup: RadioGroup, checkedId: Int ->

            resetRadioButtonsBg()
            binding.submitAnswerBtn.isEnabled = true
            radioGroup.findViewById<RadioButton>(checkedId)?.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.received_bg_BC)
            )
        }

    private fun setupOption(
        radioButton: RadioButton,
        choice: Choice,
        question: AssessmentQuestionWithRelations
    ) {
        radioButton.text = choice.text
        if (question.question.isAttempted) {
            radioButton.isClickable = false
            if (choice.userSelectedOrder == choice.sortOrder) {
                binding.quizRadioGroup.setOnCheckedChangeListener(null)
                radioButton.isChecked = true

                binding.quizRadioGroup.setOnCheckedChangeListener(quizCheckedChangeListener)

                if (choice.isCorrect) {
                    radioButton.setBackgroundResource(R.drawable.rb_correct_rect_bg)
                    radioButton.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        0,
                        R.drawable.ic_green_tick,
                        0
                    )
                    radioButton.elevation = 8F
                    radioButton.alpha = 1f
                } else {
                    resetRadioBackground(radioButton)
                    radioButton.alpha = 0.5f
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
                radioButton.alpha = 1f
            } else {
                resetRadioBackground(radioButton)
                radioButton.alpha = 0.5f
            }
        } else {
            resetRadioBackground(radioButton)
            radioButton.isClickable = true
            radioButton.alpha = 1f
        }
        if (choice.isCorrect)
            binding.quizRadioGroup.tag = radioButton.id
    }

    private fun showQuizUi() {
        binding.questionNavigateRl.visibility = View.VISIBLE
        binding.quizTv.visibility = View.VISIBLE
        binding.quizQuestionTv.visibility = View.VISIBLE
        binding.quizRadioGroup.visibility = View.VISIBLE
        binding.submitAnswerBtn.visibility = View.VISIBLE
    }

    private fun resetRadioButtonsBg() {
        binding.quizRadioGroup.children.iterator().forEach {
            if (it is RadioButton)
                resetRadioBackground(it)
        }
    }

    fun onQuestionSubmit() {
        if (binding.quizRadioGroup.tag is Int) {
            val question = assessmentQuestions[currentQuizQuestion]
            question.question.isAttempted = true
            question.question.status =
                evaluateQuestionStatus((binding.quizRadioGroup.tag as Int) == binding.quizRadioGroup.checkedRadioButtonId)

            val selectedChoice = question.choiceList[binding.quizRadioGroup.indexOfChild(
                binding.root.findViewById(binding.quizRadioGroup.checkedRadioButtonId)
            )]
            selectedChoice.isSelectedByUser = true
            selectedChoice.userSelectedOrder = selectedChoice.sortOrder

            viewModel.saveAssessmentQuestion(question)
            val correctQuestionList = ArrayList<Int>()
            assessmentQuestions.forEach { questionWithRelation ->
                if (questionWithRelation.question.isAttempted && questionWithRelation.question.status == QuestionStatus.CORRECT) {
                    correctQuestionList.add(questionWithRelation.question.remoteId)
                }
            }
            if (currentQuizQuestion == assessmentQuestions.size - 1)
                lessonActivityListener?.onQuestionStatusUpdate(
                    QUESTION_STATUS.AT,
                    quizQuestion?.id,
                    quizCorrectQuestionIds = correctQuestionList
                )
            viewModel.saveQuizToServer(assessmentQuestions[currentQuizQuestion].question.assessmentId)
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
            updateQuiz(assessmentQuestions[++currentQuizQuestion])
        } else {
            showQuizCompleteLayout()
            lessonActivityListener?.onSectionStatusUpdate(0, true)
        }
    }

    fun onGrammarContinueClick() {
        lessonActivityListener?.onNextTabCall(0)
    }

    fun onRedoQuizClick() {
        correctAns = 0
        assessmentQuestions.forEach { question ->
            question.question.isAttempted = false
            question.question.status = QuestionStatus.NONE
            question.choiceList.forEach { choice ->
                choice.isSelectedByUser = false
                choice.userSelectedOrder = 0
            }
            viewModel.saveAssessmentQuestion(question)
        }
        currentQuizQuestion = 0
        updateQuiz(assessmentQuestions[0])
        binding.grammarCompleteLayout.visibility = View.GONE
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

    private fun setUpPdfView(pdfQuestion: LessonQuestion) {
        pdfQuestion.let {
            it.pdfList?.getOrNull(0)?.let { pdfObj ->
                try {
                    if (pdfQuestion.downloadStatus == DOWNLOAD_STATUS.DOWNLOADING) {
                        download()
                    } else if (PermissionUtils.isStoragePermissionEnabled(requireContext()) && AppDirectory.getFileSize(
                            File(
                                AppDirectory.docsReceivedFile(pdfObj.url).absolutePath
                            )
                        ) > 0
                    ) {
                        pdfQuestion.downloadStatus = DOWNLOAD_STATUS.DOWNLOADED
                        fileDownloadSuccess()
                    } else {
                        pdfQuestion.downloadStatus = DOWNLOAD_STATUS.NOT_START
                        fileNotDownloadView()
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }

    private fun fileDownloadSuccess() {
        binding.ivStartDownload.visibility = View.GONE
        binding.progressDialog.visibility = View.GONE
        binding.ivCancelDownload.visibility = View.GONE
        binding.ivDownloadCompleted.visibility = View.VISIBLE
    }

    private fun fileNotDownloadView() {
        appAnalytics?.addParam(AnalyticsEvent.PDF_VIEW_STATUS.NAME, "Not downloaded")
        binding.ivStartDownload.visibility = View.VISIBLE
        binding.progressDialog.visibility = View.GONE
        binding.ivCancelDownload.visibility = View.GONE
        binding.ivDownloadCompleted.visibility = View.GONE
    }

    private fun fileDownloadingInProgressView() {
        binding.ivStartDownload.visibility = View.GONE
        binding.progressDialog.visibility = View.VISIBLE
        binding.ivCancelDownload.visibility = View.VISIBLE
        binding.ivDownloadCompleted.visibility = View.GONE
    }


    private fun requestFocus(view: View) {
        view.parent.requestChildFocus(
            view,
            view
        )
    }

    fun showNextQuestion() {
        updateQuiz(assessmentQuestions[++currentQuizQuestion])
    }

    fun showPreviousQuestion() {
        updateQuiz(assessmentQuestions[--currentQuizQuestion])
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
            askStoragePermission(OPEN_PDF_REQUEST_CODE)
            return
        }
        if (pdfQuestion?.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED) {
            pdfQuestion?.pdfList?.getOrNull(0)?.let { pdfType ->
                PdfViewerActivity.startPdfActivity(
                    context = requireContext(),
                    pdfId = pdfType.id,
                    courseName = pdfQuestion?.title ?: "Josh Skills",
                    pdfPath = AppDirectory.docsReceivedFile(pdfType.url).absolutePath
                )
            }
        } else {
            download()
        }
    }

    fun downloadCancel() {
        fileNotDownloadView()
        pdfQuestion?.downloadStatus = DOWNLOAD_STATUS.NOT_START

    }

    fun downloadStart() {
        if (pdfQuestion?.downloadStatus == DOWNLOAD_STATUS.DOWNLOADING) {
            return
        }
        download()
    }

    private fun askStoragePermission(requestCode: Int) {

        PermissionUtils.storageReadAndWritePermission(
            requireActivity(),
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let {
                        if (report.areAllPermissionsGranted()) {
                            if (requestCode == DOWNLOAD_PDF_REQUEST_CODE) {
                                download()
                            } else if (requestCode == OPEN_PDF_REQUEST_CODE) {
                                openPdf()
                            }
                        } else if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.permissionPermanentlyDeniedDialog(
                                requireActivity(),
                                R.string.grant_storage_permission
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

    private fun download() {
        if (PermissionUtils.isStoragePermissionEnabled(requireContext()).not()) {
            askStoragePermission(DOWNLOAD_PDF_REQUEST_CODE)
            return
        }
        pdfQuestion?.pdfList?.let {
            if (it.isNotEmpty()) {
                fileDownloadingInProgressView()
                DownloadUtils.downloadFile(
                    it[0].url,
                    AppDirectory.docsReceivedFile(it[0].url).absolutePath,
                    pdfQuestion!!.id,
                    null,
                    downloadListener,
                    true,
                    pdfQuestion
                )
            } else if (BuildConfig.DEBUG) {
                showToast("Pdf size is 0")
            }
        }
    }

    private fun updateVideoQuestionStatus(
        question: LessonQuestion,
        isVideoPercentComplete: Boolean = false
    ) {
        lessonActivityListener?.onQuestionStatusUpdate(
            QUESTION_STATUS.AT,
            question.id,
            isVideoPercentComplete
        )

        pdfQuestion?.let {
            it.status = QUESTION_STATUS.AT
            viewModel.updateQuestionInLocal(it)
        }
    }

    override fun onScrollChanged() {
        val view: View =
            binding.grammarScrollView.getChildAt(binding.grammarScrollView.childCount - 1)
        val bottomDetector: Int =
            view.bottom - (binding.grammarScrollView.height + binding.grammarScrollView.scrollY)
        if (bottomDetector == 0) {
            binding.scrollToBottomIv.visibility = View.GONE
        }
    }

    private var downloadListener = object : FetchListener {
        override fun onAdded(download: Download) {

        }

        override fun onCancelled(download: Download) {
            DownloadUtils.removeCallbackListener(download.tag)
            pdfQuestion?.downloadStatus = DOWNLOAD_STATUS.FAILED
            fileNotDownloadView()

        }

        override fun onCompleted(download: Download) {
            DownloadUtils.removeCallbackListener(download.tag)
            pdfQuestion?.downloadStatus = DOWNLOAD_STATUS.DOWNLOADED
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
            pdfQuestion?.downloadStatus = DOWNLOAD_STATUS.FAILED
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
            pdfQuestion?.downloadStatus = DOWNLOAD_STATUS.DOWNLOADING
            fileDownloadingInProgressView()

        }

        override fun onWaitingNetwork(download: Download) {

        }

    }

    companion object {
        private const val DOWNLOAD_PDF_REQUEST_CODE = 0
        private const val OPEN_PDF_REQUEST_CODE = 1
        private const val COLLAPSED_DESCRIPTION_MAX_LINES = 2
        private const val EXPANDED_DESCRIPTION_MAX_LINES = 100

        @JvmStatic
        fun getInstance() = GrammarFragment()
    }

}
