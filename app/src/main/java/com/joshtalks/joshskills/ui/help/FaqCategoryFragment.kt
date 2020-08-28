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
import androidx.recyclerview.widget.GridLayoutManager
import com.esafirm.imagepicker.view.GridSpacingItemDecoration
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.databinding.FragmentFaqCategoryBinding
import com.joshtalks.joshskills.ui.help.viewholder.FaqCategoryViewHolder


class FaqCategoryFragment : Fragment() {

    private lateinit var faqCategoryBinding: FragmentFaqCategoryBinding
    private val viewModel by lazy { ViewModelProvider(requireActivity()).get(HelpViewModel::class.java) }

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
        initRV()
        if (viewModel.faqCategoryLiveData.value.isNullOrEmpty()) {
            viewModel.getAllHelpCategory()
        }
    }

    private fun addObservable() {
        viewModel.faqCategoryLiveData.observe(viewLifecycleOwner, Observer { list ->
            list.sortedBy { it.sortOrder }.forEach { typeOfHelpModel ->
                faqCategoryBinding.recyclerView.addView(
                    FaqCategoryViewHolder(
                        list,
                        typeOfHelpModel,
                        -1
                    )
                )
            }
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
    }
}
