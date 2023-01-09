package com.joshtalks.joshskills.certificate.report


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.certificate.CERTIFICATION_EXAM_QUESTION
import com.joshtalks.joshskills.certificate.CertificationExamViewModel
import com.joshtalks.joshskills.certificate.R
import com.joshtalks.joshskills.certificate.constants.CHECK_EXAM_DETAILS
import com.joshtalks.joshskills.certificate.databinding.FragmentCexamReportBinding
import com.joshtalks.joshskills.common.core.EMPTY
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.local.eventbus.EmptyEventBus
import com.joshtalks.joshskills.common.repository.local.eventbus.GotoCEQuestionEventBus
import com.joshtalks.joshskills.common.repository.local.eventbus.OpenReportQTypeEventBus
import com.joshtalks.joshskills.common.repository.server.certification_exam.CertificateExamReportModel
import com.joshtalks.joshskills.common.repository.server.certification_exam.CertificationQuestion
import com.joshtalks.joshskills.common.repository.server.certification_exam.QuestionReportType
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class CExamReportFragment : Fragment() {

    companion object {
        private const val ARG_EXAM_REPORT = "exam_report"

        @JvmStatic
        fun newInstance(
            obj: CertificateExamReportModel,
            questionList: List<CertificationQuestion>?
        ) =
            CExamReportFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_EXAM_REPORT, obj)
                    putParcelableArrayList(
                        CERTIFICATION_EXAM_QUESTION,
                        ArrayList(questionList ?: emptyList())
                    )
                }
            }
    }

    private lateinit var binding: FragmentCexamReportBinding
    private var certificateExamReport: CertificateExamReportModel? = null
    private var questionList: List<CertificationQuestion> = emptyList()
    private var compositeDisposable = CompositeDisposable()
    private var id:Int? =null
    private lateinit var url:String
    private val viewModel: CertificationExamViewModel by lazy {
        ViewModelProvider(requireActivity()).get(CertificationExamViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            certificateExamReport = it.getParcelable(ARG_EXAM_REPORT)
            questionList =
                it.getParcelableArrayList(CERTIFICATION_EXAM_QUESTION)
                    ?: emptyList()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_cexam_report, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        id = certificateExamReport?.reportId
        url = certificateExamReport?.certificateURL?:EMPTY
        certificateExamReport?.run {
            val adapter = ReportOverviewAdapter(this@CExamReportFragment, this,viewModel.examType.value,questionList)
            binding.chatRv.adapter = adapter
            updateRvScrolling(false)
        }
    }

    override fun onResume() {
        super.onResume()
        addObserver()
        /*if(PrefManager.getBoolValue(IS_FIRST_TIME_FLOW_CERTI, defValue = false)){
            PrefManager.put(IS_FIRST_TIME_FLOW_CERTI, true)
            certificateExamReport?.run {
                binding.chatRv.invalidate()
                binding.chatRv.addView(ReportOverviewView1(this, viewModel.examType.value))
                binding.chatRv.addView(ReportOverviewView2(this, questionList))
                binding.chatRv.refresh()
                updateRvScrolling(true)
            }
        }*/
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    private fun addObserver() {
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(EmptyEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    updateRvScrolling(true)
                    viewModel.saveImpression(CHECK_EXAM_DETAILS)
                    binding.chatRv.setCurrentItem(1,true)
                }, {
                    it.printStackTrace()
                })
        )
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(OpenReportQTypeEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (QuestionReportType.UNKNOWN == it.type) {
                        viewModel.isSAnswerUiShow = false
                        binding.tempFl.visibility = View.GONE
                        return@subscribe
                    }
                    binding.tempFl.visibility = View.VISIBLE
                    showViewOnHint(it.type)
                }, {
                    it.printStackTrace()
                })
        )
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(GotoCEQuestionEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    viewModel.isSAnswerUiShow = false
                    binding.tempFl.visibility = View.GONE
                }, {
                    it.printStackTrace()
                })
        )
    }

    private fun updateRvScrolling(flag: Boolean) {
        binding.chatRv.isUserInputEnabled = flag
    }

    private fun showViewOnHint(type: QuestionReportType) {
        certificateExamReport?.run {
            val adapter = ReportOverView3Adapter(this@CExamReportFragment,this,questionList,type)
            binding.tempRv.adapter = adapter
            viewModel.isSAnswerUiShow = true
        }
        binding.tempRv.setOnClickListener {
            binding.tempFl.visibility = View.GONE
            binding.tempRv.removeAllViews()
        }
    }
}