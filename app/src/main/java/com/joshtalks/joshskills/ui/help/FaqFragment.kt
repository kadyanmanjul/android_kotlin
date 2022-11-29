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
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.repository.server.FAQCategory

class FaqFragment : Fragment() {

    private lateinit var categoryList: List<FAQCategory>
    private var selectedCategory: FAQCategory? = null
    private val viewModel by lazy { ViewModelProvider(this).get(HelpViewModel::class.java) }
    private val faqAdapter by lazy { FaqAdapter(ArrayList()) }
    private lateinit var appAnalytics: AppAnalytics
    private val chipGroupCategory by lazy {
        view?.findViewById<ChipGroup>(R.id.chipGroupCategory)
    }
    private val txtCategoryName by lazy {
        view?.findViewById<JoshTextView>(R.id.chipGroupCategory)
    }
    private val faqList by lazy {
        view?.findViewById<RecyclerView>(R.id.faqList)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            categoryList = it.getParcelableArrayList(ARG_CATEGORY_LIST) ?: ArrayList()
            selectedCategory = it.getParcelable(ARG_SELECTED_CATEGORY)
        }
        appAnalytics = AppAnalytics.create(AnalyticsEvent.FAQ_QUESTIONS_LIST_SCREEN.NAME)
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
        faqList?.layoutManager = LinearLayoutManager(context)
        faqList?.adapter = faqAdapter
        val divider = DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
        divider.setDrawable(
            ColorDrawable(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.seek_bar_background
                )
            )
        )
        faqList?.addItemDecoration(divider)
    }

    private fun addObservers() {
        viewModel.faqListLiveData.observe(viewLifecycleOwner, Observer {
            faqAdapter.updateList(it.filter {
                it.categoryId == selectedCategory?.id
            })
        })

        chipGroupCategory?.setOnCheckedChangeListener { group, checkedId ->
            try {
                selectedCategory = categoryList.filter { it.id == checkedId }[0]
                logCategorySelectedEvent()
                txtCategoryName?.text = selectedCategory?.categoryName
                faqAdapter.updateList(viewModel.faqListLiveData.value?.filter {
                    it.categoryId == selectedCategory?.id
                } ?: emptyList())
            } catch (ex: Exception) {
            }
        }
    }

    private fun renderView() {
        txtCategoryName?.text = selectedCategory?.categoryName
        chipGroupCategory?.removeAllViews()
        categoryList.sortedBy { it.sortOrder }.forEach {
            val chip = LayoutInflater.from(context)
                .inflate(R.layout.faq_category_item, null, false) as Chip
            chip.text = it.categoryName
            chip.tag = it.id
            chip.id = it.id
            chipGroupCategory?.addView(chip)
        }
        selectedCategory?.id?.run {
            chipGroupCategory?.check(this)
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

    private fun logCategorySelectedEvent()  {
        when(selectedCategory?.id){
            1-> MixPanelTracker.publishEvent(MixPanelEvent.FAQ_TECHNICAL_ISSUES).push()
            2-> MixPanelTracker.publishEvent(MixPanelEvent.FAQ_ACC_SETUP).push()
            3-> MixPanelTracker.publishEvent(MixPanelEvent.FAQ_PAYMENT_AND_REFUND).push()
            4-> MixPanelTracker.publishEvent(MixPanelEvent.FAQ_COURSES).push()
        }
        AppAnalytics.create(AnalyticsEvent.FAQ_QUESTIONS_LIST_SCREEN.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.FAQ_CATEGORY_SELECTED.NAME, selectedCategory?.categoryName)
            .push()
    }

    override fun onStop() {
        appAnalytics.push()
        super.onStop()
    }
}
