package com.joshtalks.joshskills.ui.help

import android.os.Bundle
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.decorator.GridSpacingItemDecoration
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentFaqCategoryBinding
import com.joshtalks.joshskills.repository.server.help.Action
import com.joshtalks.joshskills.ui.help.adapter.FaqCategoryViewAdapter
import com.joshtalks.joshskills.ui.special_practice.utils.OPEN_CALL_SCREEN
import com.joshtalks.joshskills.ui.special_practice.utils.OPEN_CATEGORY_SCREEN
import com.joshtalks.joshskills.ui.special_practice.utils.OPEN_FAQ_SCREEN
import com.joshtalks.joshskills.ui.special_practice.utils.OPEN_HELP_CHAT_SCREEN


class FaqCategoryFragment : Fragment() {

    private lateinit var faqCategoryBinding: FragmentFaqCategoryBinding
    private val viewModel by lazy { ViewModelProvider(requireActivity()).get(HelpViewModel::class.java) }
    private val faqListAdapter = FaqCategoryViewAdapter(cardType = -1)
    private var message = Message()

    private var singleLiveEvent = EventLiveData
    companion object {
        @JvmStatic
        fun newInstance() = FaqCategoryFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        faqCategoryBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_faq_category, container, false)
        faqCategoryBinding.lifecycleOwner = this
        return faqCategoryBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val titleView = requireActivity().findViewById<AppCompatTextView>(R.id.text_message_title)
        titleView?.text = getString(R.string.faq_title)
        addObservable()
        if (viewModel.faqCategoryLiveData.value.isNullOrEmpty()) {
            viewModel.getAllHelpCategory()
        }

        faqListAdapter.setOnClickListener { map ->
            message.what = OPEN_CATEGORY_SCREEN
            message.obj = map
            singleLiveEvent.value = message
        }
    }

    private fun addObservable() {
        viewModel.faqCategoryLiveData.observe(viewLifecycleOwner, Observer { list ->
            faqListAdapter.addListOfFAQ(list)
            initRV()
        })
        viewModel.apiCallStatusLiveData.observe(viewLifecycleOwner, Observer {
            faqCategoryBinding.progressBar.visibility = View.GONE
        })
    }

    private fun initRV() {
        val layoutManager = GridLayoutManager(requireContext(), 2)
        faqCategoryBinding.recyclerView.builder.setHasFixedSize(true)
            .setLayoutManager(layoutManager)
        faqCategoryBinding.recyclerView.addItemDecoration(
            GridSpacingItemDecoration(2, Utils.dpToPx(requireContext(), 12f), true)
        )
        faqCategoryBinding.recyclerView.adapter = faqListAdapter
    }
}
