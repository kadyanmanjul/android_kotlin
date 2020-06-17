package com.joshtalks.joshskills.ui.help

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FragmentHelpListBinding
import com.joshtalks.joshskills.repository.server.help.HelpCenterOptions
import com.joshtalks.joshskills.ui.view_holders.HelpViewHolder


class HelpListFragment : Fragment() {
    private lateinit var viewModel: HelpViewModel
    private lateinit var helpListBinding: FragmentHelpListBinding

    companion object {
        @JvmStatic
        fun newInstance() = HelpListFragment().apply {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = activity?.run {
            ViewModelProvider(this).get(HelpViewModel::class.java)
        }!!

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        helpListBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_help_list, container, false)
        helpListBinding.lifecycleOwner = this
        helpListBinding.handler = this
        return helpListBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRV()
        HelpCenterOptions.getHelpOptionsModelObject()?.let { helpCenterOptionsModel ->
            val titleView =
                requireActivity().findViewById<AppCompatTextView>(R.id.text_message_title)
            titleView?.text = helpCenterOptionsModel.title
            helpCenterOptionsModel.options.forEach {
                helpListBinding.recyclerView.addView(HelpViewHolder(it))
            }
            helpCenterOptionsModel.supportMessage?.run {
                helpListBinding.infoSupport.visibility = View.VISIBLE
                helpListBinding.infoSupport.text = this
            }
        }
    }

    private fun initRV() {
        val linearLayoutManager = LinearLayoutManager(activity)
        linearLayoutManager.isSmoothScrollbarEnabled = true
        helpListBinding.recyclerView.builder.setHasFixedSize(true)
            .setLayoutManager(linearLayoutManager)
        val divider = DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
        divider.setDrawable(
            ColorDrawable(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.seek_bar_background
                )
            )
        )
        helpListBinding.recyclerView.addItemDecoration(divider)
    }
}
