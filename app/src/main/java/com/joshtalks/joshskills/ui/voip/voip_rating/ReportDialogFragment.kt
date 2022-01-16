package com.joshtalks.joshskills.ui.voip.voip_rating

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseDialogFragment
import com.joshtalks.joshskills.databinding.LayoutReportDialogFragmentBinding
import com.joshtalks.joshskills.ui.voip.voip_rating.adapter.ReportAdapter




class ReportDialogFragment(function: () -> Unit) : BaseDialogFragment() {

    lateinit var binding: LayoutReportDialogFragmentBinding
    private lateinit var manager: FlexboxLayoutManager
    var type = "REPORT"
    var channelName = "460dfa4a-88e1-48e9-a7f0-d2fcaa95c377"
    var optionId = 0
    val CHANNEL_NAME="channel_name"
    val FEEDBACK_OPTIONS="feedback_option"
    val REPORTED_BY_ID="reported_by_id"
    val REPORTED_AGAINST_ID="reported_against_id"
    var callerId=0
    var currentId=0
    val ARG_CALLER_ID = "caller_id"
    val ARG_CURRENT_ID= "current_id"



    val vm by lazy {
        ViewModelProvider(this)[ReportViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LayoutReportDialogFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (arguments != null) {
            val mArgs = arguments
            type = mArgs?.getString("type").toString()
            callerId= mArgs?.getInt(ARG_CALLER_ID)!!
            currentId= mArgs?.getInt(ARG_CURRENT_ID)!!


        }

        initView()
        addObserver()
    }

    private fun addObserver() {
        vm.reportModel.observe(viewLifecycleOwner, {
            val optionList = it.options
            binding.headTv.text = it.message.toString()
            manager = FlexboxLayoutManager(context)
            manager.flexDirection = FlexDirection.ROW;
            manager.justifyContent = JustifyContent.FLEX_START;
            binding.issueRv.apply {
                adapter = ReportAdapter(optionList, context) {

                    if (optionId == 0) {
                        binding.submitBtn.setBackgroundResource(R.drawable.rounded_state_button_bg)
                        binding.submitBtn.isEnabled = true
                    }
                    optionId = it

                }
                layoutManager = manager
            }
        })
    }

    private fun initView() {

        vm.getReportOptionsListFromSharedPref(type)

        binding.crossBtn.setOnClickListener {
            closeDialog()

        }
        binding.handler = myHandlersListener

    }
    private val myHandlersListener: ClickListenerHandler = object : ClickListenerHandler {
        override fun submitReport() {
            val map: HashMap<String, Any> = HashMap<String, Any>()
            map[CHANNEL_NAME] = channelName
            map[FEEDBACK_OPTIONS] = optionId
            map[REPORTED_BY_ID] = currentId
            map[REPORTED_AGAINST_ID] = callerId
            vm.submitReportOption(map)
            closeDialog()
            function.invoke()
        }
    }



    private fun closeDialog() {
            super.dismiss()
    }


}