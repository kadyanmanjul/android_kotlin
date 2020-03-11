package com.joshtalks.joshskills.ui.help


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.databinding.FragmentHelpListBinding
import com.joshtalks.joshskills.ui.view_holders.HelpViewHolder
import com.vanniktech.emoji.Utils


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
        initRV()
        return helpListBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val titleView = activity?.findViewById<AppCompatTextView>(R.id.text_message_title)
        titleView?.text = getString(R.string.help_header)
        if (viewModel.typeOfHelpModelLiveData.value.isNullOrEmpty()) {
            viewModel.getAllHelpCategory()
        }
        viewModel.typeOfHelpModelLiveData.observe(viewLifecycleOwner, Observer {
            it.forEach { obj ->
                helpListBinding.recyclerView.addView(HelpViewHolder(obj))
            }
            helpListBinding.progressBar.visibility = View.GONE
        })
    }

    private fun initRV() {
        val linearLayoutManager = LinearLayoutManager(activity)
        linearLayoutManager.isSmoothScrollbarEnabled = true
        helpListBinding.recyclerView.builder.setHasFixedSize(true)
            .setLayoutManager(linearLayoutManager)
        helpListBinding.recyclerView.addItemDecoration(
            LayoutMarginDecoration(
                Utils.dpToPx(
                    AppObjectController.joshApplication,
                    2f
                )
            )
        )
    }
}
