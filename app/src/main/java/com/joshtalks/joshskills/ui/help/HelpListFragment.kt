package com.joshtalks.joshskills.ui.help

import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentHelpListBinding
import com.joshtalks.joshskills.repository.server.help.Action
import com.joshtalks.joshskills.repository.server.help.HelpCenterOptions
import com.joshtalks.joshskills.repository.server.help.HelpCenterOptionsModel
import com.joshtalks.joshskills.ui.help.adapter.HelpListAdapter
import com.joshtalks.joshskills.ui.special_practice.utils.OPEN_CALL_SCREEN
import com.joshtalks.joshskills.ui.special_practice.utils.OPEN_FAQ_SCREEN
import com.joshtalks.joshskills.ui.special_practice.utils.OPEN_HELP_CHAT_SCREEN


class HelpListFragment : Fragment() {
    private lateinit var viewModel: HelpViewModel
    private lateinit var helpListBinding: FragmentHelpListBinding
    private val helpListAdapter = HelpListAdapter()
    private var message = Message()

    private var singleLiveEvent = EventLiveData
    companion object {
        @JvmStatic
        fun newInstance() = HelpListFragment().apply {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = requireActivity().run {
            ViewModelProvider(this).get(HelpViewModel::class.java)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        helpListBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_help_list, container, false)
        helpListBinding.lifecycleOwner = this
        helpListBinding.handler = this
        return helpListBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        HelpCenterOptions.getHelpOptionsModelObject()?.let { helpCenterOptionsModel ->
            val titleView = requireActivity().findViewById<AppCompatTextView>(R.id.text_message_title)
            titleView?.text = helpCenterOptionsModel.title
            helpListAdapter.addListOfHelp(helpCenterOptionsModel.options)
            initRV(helpCenterOptionsModel)
        }

        helpListAdapter.setOnClickListener { option, position ->
            when {
                Action.CALL == option.action -> {
                    message.what = OPEN_CALL_SCREEN
                    message.obj = option
                    singleLiveEvent.value = message
                }
                Action.HELPCHAT == option.action -> {
                    message.what = OPEN_HELP_CHAT_SCREEN
                    message.obj = option
                    singleLiveEvent.value = message
                }
                Action.FAQ == option.action -> {
                    message.what = OPEN_FAQ_SCREEN
                    message.obj = option
                    singleLiveEvent.value = message
                }
                else -> {
                    showToast(getString(R.string.something_went_wrong))
                }
            }
        }
    }

    private fun initRV(helpCenterOptionsModel: HelpCenterOptionsModel) {
        val linearLayoutManager = LinearLayoutManager(activity)
        linearLayoutManager.isSmoothScrollbarEnabled = true
        helpListBinding.recyclerView.adapter = helpListAdapter
        helpListBinding.recyclerView.builder.setHasFixedSize(true)
            .setLayoutManager(linearLayoutManager)
        helpCenterOptionsModel.supportMessage?.run {
            helpListBinding.infoSupport.visibility = View.VISIBLE
            helpListBinding.infoSupport.text = this
        }
    }
}
