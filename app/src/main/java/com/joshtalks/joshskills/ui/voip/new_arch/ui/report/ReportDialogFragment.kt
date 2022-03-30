package com.joshtalks.joshskills.ui.voip.new_arch.ui.report

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.base.BaseDialogFragment
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.databinding.FragmentReportDialogBinding
import com.joshtalks.joshskills.ui.voip.new_arch.ui.report.model.ReportModel

class ReportDialogFragment(val function: () -> Unit) : BaseDialogFragment() {

    private val FEEDBACK_OPTIONS = "feedback_option"
    private val REPORTED_BY_ID = "reported_by_id"
    private val REPORTED_AGAINST_ID = "reported_against_id"
    lateinit var binding: FragmentReportDialogBinding
    var type1 = "REPORT"
    var channelName = EMPTY
    var optionId = 0
    val CHANNEL_NAME = "channel_name"
    var callerId1 = 1
    var currentId1 = 1
    val ARG_CALLER_ID = "caller_id"
    val ARG_CURRENT_ID = "current_id"
    val ARG_TYPE = "type"
    val reportModel: ReportModel? = null
    var submitReport: (() -> Unit)? = null

    val vm by lazy {
        ViewModelProvider(this)[ReportViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReportDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        addObserver()
    }

    private fun addObserver() {
        vm.reportModel.observe(viewLifecycleOwner) {
            reportModel?.options = it.options
            reportModel?.message = it.message
        }
    }

    private fun initView() {
        val mArgs = arguments
        binding.vm = vm
        binding.fragment = this@ReportDialogFragment
        type1 = mArgs?.getString(ARG_TYPE).toString()
        callerId1 = mArgs?.getInt(ARG_CALLER_ID)!!
        currentId1 = mArgs.getInt(ARG_CURRENT_ID)
        channelName = mArgs.getString(CHANNEL_NAME).toString()
        vm.getReportOptionsListFromSharedPref(type1)
        binding.crossBtn.setOnClickListener {
            closeDialog()
        }
    }

    private fun closeDialog() {
        super.dismiss()
    }

    companion object {
        @JvmStatic
        fun newInstance(
            callerID: Int,
            currentID: Int,
            type: String,
            channelName: String,
            function: () -> Unit
        ) =
            ReportDialogFragment(function).apply {
                arguments = Bundle().apply {
                    putString(ARG_TYPE, type)
                    putInt(ARG_CALLER_ID, callerID)
                    putInt(ARG_CURRENT_ID, currentID)
                    putString(CHANNEL_NAME, channelName)
                }
            }
    }

    fun submitReport(v: View) {
        val map: HashMap<String, Any> = HashMap()
        map[CHANNEL_NAME] = channelName
        map[FEEDBACK_OPTIONS] = vm.optionId
        map[REPORTED_BY_ID] = currentId1
        map[REPORTED_AGAINST_ID] = callerId1
        vm.submitReportOption(map)
        closeDialog()
        function.invoke()
    }

    override fun show(manager: FragmentManager, tag: String?) {
        if (!manager.isDestroyed && !manager.isStateSaved) {
            super.show(manager, tag)
        }
    }
}