package com.joshtalks.joshskills.ui.help

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.repository.server.FAQCategory
import kotlinx.android.synthetic.main.fragment_faq.*

class FaqFragment : Fragment() {

    private lateinit var categoryList: List<FAQCategory>
    private var selectedCategory: FAQCategory? = null
    private val viewModel by lazy { ViewModelProvider(this).get(HelpViewModel::class.java) }
    private val faqAdapter by lazy { FaqAdapter(ArrayList()) }
    private lateinit var appAnalytics: AppAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            categoryList = it.getParcelableArrayList(ARG_CATEGORY_LIST) ?: ArrayList()
            selectedCategory = it.getParcelable(ARG_SELECTED_CATEGORY)
        }
        appAnalytics= AppAnalytics.create(AnalyticsEvent.FAQ_QUESTIONS_LIST_SCREEN.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.FAQ_CATEGORY_SELECTED.NAME, selectedCategory?.categoryName)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_faq, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRV()
        addObservers()
        renderView()
        viewModel.getFaq()
    }

    private fun initRV() {
        faqList.layoutManager = LinearLayoutManager(context)
        faqList.adapter = faqAdapter
        val divider = DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
        divider.setDrawable(
            ColorDrawable(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.seek_bar_background
                )
            )
        )
        faqList.addItemDecoration(divider)
    }

    private fun addObservers() {
        viewModel.faqListLiveData.observe(viewLifecycleOwner, Observer {
            faqAdapter.updateList(it.filter {
                it.categoryId == selectedCategory?.id
            })
        })

        chipGroupCategory.setOnCheckedChangeListener { group, checkedId ->
            selectedCategory = categoryList.filter { it.id == checkedId }[0]
            txtCategoryName.text = selectedCategory?.categoryName
            faqAdapter.updateList(viewModel.faqListLiveData.value?.filter {
                it.categoryId == selectedCategory?.id
            } ?: emptyList())
        }
    }

    private fun renderView() {
        txtCategoryName.text = selectedCategory?.categoryName
        chipGroupCategory.removeAllViews()
        categoryList.sortedBy { it.sortOrder }.forEach {
            val chip = LayoutInflater.from(context)
                .inflate(R.layout.faq_category_item, null, false) as Chip
            chip.text = it.categoryName
            chip.tag = it.id
            chip.id = it.id
            chipGroupCategory.addView(chip)
        }
        selectedCategory?.id?.run {
            chipGroupCategory.check(this)
        }
    }

    companion object {
        const val ARG_CATEGORY_LIST = "category-list"
        const val ARG_SELECTED_CATEGORY = "selected-category"

        @JvmStatic
        fun newInstance(selectedCategory: FAQCategory, categoryList: ArrayList<FAQCategory>) =
            FaqFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(ARG_CATEGORY_LIST, categoryList)
                    putParcelable(ARG_SELECTED_CATEGORY, selectedCategory)
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        appAnalytics.push()
    }
}
