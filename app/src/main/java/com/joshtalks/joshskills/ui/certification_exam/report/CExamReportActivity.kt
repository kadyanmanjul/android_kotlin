package com.joshtalks.joshskills.ui.certification_exam.report

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.Utils
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
import com.joshtalks.joshskills.ui.certification_exam.examview.CExamMainActivity
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class CExamReportActivity : BaseActivity() {

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

    private val viewModel: CertificationExamViewModel by lazy {
        ViewModelProvider(this).get(CertificationExamViewModel::class.java)
    }
    private var certificateExamId: Int = -1
    private var certificationQuestionModel: CertificationQuestionModel? = null
    private lateinit var binding: ActivityCexamReportBinding
    private var compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)
        binding =
            DataBindingUtil.setContentView(this, R.layout.activity_cexam_report)
        binding.lifecycleOwner = this
        binding.handler = this
        certificateExamId = intent.getIntExtra(CERTIFICATION_EXAM_ID, -1)
        if (certificateExamId == -1) {
            this.finish()
            return
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
        viewModel.apiStatus.observe(
            this,
            {
                binding.progressBar.visibility = View.GONE
            }
        )
        viewModel.examReportLiveData.observe(
            this,
            { certificateList ->
                certificateList?.run {
                    setUpExamViewPager(this)
                    certificateList.last().awardMentor?.let {
                        // showAward(mutableListOf(it))
                    }

                    if (certificateList.last().points.isNullOrBlank().not()) {
                        showSnackBar(
                            binding.rootView,
                            Snackbar.LENGTH_LONG,
                            certificateList.last().points
                        )
                    }
                }
            }
        )
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
                .subscribe(
                    {
                        downloadFile(it.url, message = "Certificate download complete")
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )
    }

    override fun onBackPressed() {
        if (viewModel.isSAnswerUiShow) {
            viewModel.isSAnswerUiShow = false
            RxBus2.publish(OpenReportQTypeEventBus(QuestionReportType.UNKNOWN))
            return
        }
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(onDownloadComplete)
        } catch (ex: Exception) {
        }
    }
}
