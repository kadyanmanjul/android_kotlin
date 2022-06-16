package com.joshtalks.joshskills.ui.certification_exam.report

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.interfaces.FileDownloadCallback
import com.joshtalks.joshskills.core.service.CONVERSATION_ID
import com.joshtalks.joshskills.databinding.ActivityCexamReportBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.DownloadFileEventBus
import com.joshtalks.joshskills.repository.local.eventbus.GotoCEQuestionEventBus
import com.joshtalks.joshskills.repository.local.eventbus.OpenReportQTypeEventBus
import com.joshtalks.joshskills.repository.server.certification_exam.CertificateExamReportModel
import com.joshtalks.joshskills.repository.server.certification_exam.CertificationExamView
import com.joshtalks.joshskills.repository.server.certification_exam.CertificationQuestionModel
import com.joshtalks.joshskills.repository.server.certification_exam.QuestionReportType
import com.joshtalks.joshskills.ui.certification_exam.CERTIFICATION_EXAM_ID
import com.joshtalks.joshskills.ui.certification_exam.CERTIFICATION_EXAM_QUESTION
import com.joshtalks.joshskills.ui.certification_exam.CertificationExamViewModel
import com.joshtalks.joshskills.ui.certification_exam.constants.*
import com.joshtalks.joshskills.ui.certification_exam.examview.CExamMainActivity
import com.joshtalks.joshskills.ui.certification_exam.report.udetail.CertificateDetailActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class CExamReportActivity : BaseActivity(), FileDownloadCallback {
    private val viewModel: CertificationExamViewModel by lazy {
        ViewModelProvider(this).get(CertificationExamViewModel::class.java)
    }
    private var certificateExamId: Int = -1
    private var certificationQuestionModel: CertificationQuestionModel? = null
    private lateinit var binding: ActivityCexamReportBinding
    private var compositeDisposable = CompositeDisposable()

    private var userDetailsActivityResult: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.getStringExtra(CERTIFICATE_URL)?.let {
                    val localPath = result.data?.getStringExtra(LOCAL_DOWNLOAD_URL)
                    if (localPath.isNullOrEmpty()) {
                        //TODO: Have to understand what ill happed
                    } else {
                        downloadedFile(localPath)
                    }
                    val cPos = binding.examReportList.currentItem
                    // prevent API call to direct update filed value
                    viewModel.examReportLiveData.value?.getOrNull(cPos)?.certificateURL = it
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_cexam_report)
        window.setBackgroundDrawable(null)
        binding.lifecycleOwner = this
        binding.handler = this

        certificateExamId = intent.getIntExtra(CERTIFICATION_EXAM_ID, -1)
        if (certificateExamId == -1) {
            this.finish()
            return
        }else{
            viewModel.certificateExamId = certificateExamId
            viewModel.typeOfExam()
        }
        certificationQuestionModel =
            intent.getParcelableExtra(CERTIFICATION_EXAM_QUESTION) as CertificationQuestionModel?
        addObserver()
        viewModel.getUserAllExamReports(certificateExamId)
    }

    override fun getConversationId(): String? {
        return intent.getStringExtra(com.joshtalks.joshskills.track.CONVERSATION_ID)
    }

    private fun addObserver() {
        viewModel.apiStatus.observe(this) {
            binding.progressBar.visibility = View.GONE
        }

        viewModel.examReportLiveData.observe(this) { certificateList ->
            certificateList?.run {
                setUpExamViewPager(this)
                certificateList.lastOrNull()?.let {
                    if (!PrefManager.getBoolValue(IS_EXAM_POINTS_PROMPT, defValue = false)) {
                        if (it.points.isNullOrBlank().not()) {
                            showSnackBar(binding.rootView, Snackbar.LENGTH_LONG, it.points)
                            PrefManager.put(IS_EXAM_POINTS_PROMPT, true)
                        }
                    }
                }
            }
        }
    }

    private fun setUpExamViewPager(list: List<CertificateExamReportModel>) {
        binding.examReportList.adapter =
            CExamReportAdapter(this, list, certificationQuestionModel?.questions)
        binding.examReportList.setPageTransformer(
            MarginPageTransformer(
                Utils.dpToPx(
                    applicationContext,
                    16f
                )
            )
        )
        val tabStrip: LinearLayout = binding.tabLayout.getChildAt(0) as LinearLayout
        for (i in 0 until tabStrip.childCount) {
            tabStrip.getChildAt(i).setOnTouchListener { _, _ -> true }
        }
        binding.examReportList.registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {
            })
        TabLayoutMediator(binding.tabLayout, binding.examReportList) { tab, position ->
            tab.text = "Attempt " + (position + 1)
        }.attach()
        binding.examReportList.currentItem = list.size - 1
    }

    override fun onResume() {
        super.onResume()
        binding.tabLayout.visibility = View.VISIBLE
        addRxbusObserver()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    private fun addRxbusObserver() {
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(GotoCEQuestionEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        certificationQuestionModel?.run {
                            startActivity(
                                CExamMainActivity.startExamActivity(
                                    this@CExamReportActivity,
                                    this,
                                    examView = CertificationExamView.RESULT_VIEW,
                                    openQuestionId = it.questionId,
                                    attemptSequence = (binding.examReportList.currentItem + 1),
                                    conversationId = getConversationId()
                                )
                            )
                        }
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )

        compositeDisposable.add(
            RxBus2.listenWithoutDelay(DownloadFileEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        val examType = viewModel.certificationQuestionLiveData.value?.type
                        var impressionCheck = false
                        when (examType){
                            EXAM_TYPE_BEGINNER->{
                                impressionCheck = PrefManager.getBoolValue(IS_CERTIFICATE_GENERATED_BEGINNER, defValue = true)
                            }
                            EXAM_TYPE_INTERMEDIATE->{
                                impressionCheck = PrefManager.getBoolValue(IS_CERTIFICATE_GENERATED_INTERMEDIATE, defValue = true)
                            }
                            EXAM_TYPE_ADVANCED->{
                                impressionCheck = PrefManager.getBoolValue(IS_CERTIFICATE_GENERATED_ADVANCED, defValue = true)
                            }
                        }
                        if (impressionCheck) {
                            viewModel.saveImpression(GENERATE_CERTIFICATE)
                        }else{
                            viewModel.saveImpression(SHOW_CERTIFICATE)
                        }
                        val cPos = binding.examReportList.currentItem
                        val url =
                            viewModel.examReportLiveData.value?.getOrNull(cPos)?.certificateURL
                        userDetailsActivityResult.launch(
                            CertificateDetailActivity.startUserDetailsActivity(
                                this, rId = it.id,
                                conversationId = getConversationId(),
                                certificateUrl = url,
                                certificateExamId = certificateExamId
                            )
                        )
                        return@subscribe
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )
    }

    override fun onBackPressed() {
        MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
        if (viewModel.isSAnswerUiShow) {
            viewModel.isSAnswerUiShow = false
            RxBus2.publish(OpenReportQTypeEventBus(QuestionReportType.UNKNOWN))
            return
        }
        super.onBackPressed()
    }

    /*override fun downloadedFile(path: String) {
        showToast(getString(R.string.certificate_download_success))
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val file = File(path)
                val mime: MimeTypeMap = MimeTypeMap.getSingleton()
                val ext: String = file.name.substring(file.name.lastIndexOf(".") + 1)
                val type = mime.getMimeTypeFromExtension(ext)
                val target = Intent(Intent.ACTION_VIEW)
                target.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                target.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                target.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val contentUri: Uri = FileProvider.getUriForFile(
                        Objects.requireNonNull(applicationContext),
                        BuildConfig.APPLICATION_ID + ".provider", file
                    )
                    target.setDataAndType(contentUri, type)
                } else {
                    target.setDataAndType(Uri.fromFile(file), type)
                }
                target.flags =
                    (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                val intent = Intent.createChooser(target, "Open Certifificate ")
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }*/

    companion object {
        fun getExamResultActivityIntent(
            context: Context,
            certificateExamId: Int,
            certificationQuestionModel: CertificationQuestionModel,
            conversationId: String? = null,
        ): Intent {
            return Intent(context, CExamReportActivity::class.java).apply {
                putExtra(CERTIFICATION_EXAM_ID, certificateExamId)
                putExtra(CERTIFICATION_EXAM_QUESTION, certificationQuestionModel)
                putExtra(CONVERSATION_ID, conversationId)
            }
        }
    }
}
