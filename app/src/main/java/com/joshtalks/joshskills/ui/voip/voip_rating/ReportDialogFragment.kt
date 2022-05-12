package com.joshtalks.joshskills.ui.voip.voip_rating

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseDialogFragment
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.analytics.ParamKeys
import com.joshtalks.joshskills.databinding.LayoutReportDialogFragmentBinding
import com.joshtalks.joshskills.ui.voip.SHOW_FPP_DIALOG
import com.joshtalks.joshskills.ui.voip.voip_rating.adapter.ReportAdapter


class ReportDialogFragment : BaseDialogFragment() {

    lateinit var binding: LayoutReportDialogFragmentBinding
    private lateinit var manager: FlexboxLayoutManager
    var type1 = "REPORT"
    var channelName = EMPTY
    var fppDialogFlag:String?=null
    var optionId = 0
    private var function: (() -> Unit)? = null
    val CHANNEL_NAME="channel_name"
    val FEEDBACK_OPTIONS="feedback_option"
    val REPORTED_BY_ID="reported_by_id"
    val REPORTED_AGAINST_ID="reported_against_id"
    var callerId1=1
    var currentId1=1
    val ARG_CALLER_ID = "caller_id"
    val ARG_CURRENT_ID= "current_id"
    val ARG_TYPE= "type"


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

        val mArgs = arguments
        type1 = mArgs?.getString(ARG_TYPE).toString()
        callerId1= mArgs?.getInt(ARG_CALLER_ID)!!
        currentId1= mArgs?.getInt(ARG_CURRENT_ID)
        channelName= mArgs?.getString(CHANNEL_NAME).toString()
        fppDialogFlag = mArgs.getString(SHOW_FPP_DIALOG)

        vm.getReportOptionsListFromSharedPref(type1)

        binding.crossBtn.setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.CANCEL).push()
            closeDialog()
            //function.invoke()
        }
        binding.handler = myHandlersListener

    }
    private val myHandlersListener: ClickListenerHandler = object : ClickListenerHandler {
        override fun submitReport() {
            val map: HashMap<String, Any> = HashMap<String, Any>()
            map[CHANNEL_NAME] = channelName
            map[FEEDBACK_OPTIONS] = optionId
            map[REPORTED_BY_ID] = currentId1
            map[REPORTED_AGAINST_ID] = callerId1

            MixPanelTracker.publishEvent(MixPanelEvent.CALL_END_REASON)
                .addParam(ParamKeys.FEEDBACK_OPTION,optionId)
                .addParam(ParamKeys.REPORTED_BY_ID,currentId1)
                .addParam(ParamKeys.REPORTED_AGAINST_ID,callerId1)
                .addParam(ParamKeys.CHANNEL_NAME,channelName)
                .push()

            vm.submitReportOption(map)
            closeDialog()
        }
    }

    private fun closeDialog() {
        if(fppDialogFlag=="false"){
            function?.invoke()
        }else{
            super.dismiss()
        }
    }

   companion object {
        @JvmStatic
        fun newInstance(
            callerID: Int,
            currentID: Int,
            typ: String,
            channelName: String,
            mFunction: () -> Unit,
            fppDialogFlag: String?,
        ) =
            ReportDialogFragment().apply {
                arguments = Bundle().apply {
                    function = mFunction
                    putString(ARG_TYPE, typ)
                    putInt(ARG_CALLER_ID,callerID)
                    putInt(ARG_CURRENT_ID,currentID)
                    putString(CHANNEL_NAME,channelName)
                    putString(SHOW_FPP_DIALOG,fppDialogFlag)
                }
            }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        if (!manager?.isDestroyed && !manager.isStateSaved) {
            super.show(manager, tag)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        activity?.finish()
    }
}