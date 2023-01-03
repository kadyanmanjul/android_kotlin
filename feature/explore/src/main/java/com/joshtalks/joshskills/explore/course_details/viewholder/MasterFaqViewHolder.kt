package com.joshtalks.joshskills.explore.course_details.viewholder

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonObject
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.PrefManager
import com.joshtalks.joshskills.common.core.Utils
import com.joshtalks.joshskills.common.core.VERSION
import com.joshtalks.joshskills.common.core.analytics.*
import com.joshtalks.joshskills.common.core.custom_ui.decorator.GridSpacingItemDecoration
import com.joshtalks.joshskills.common.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.common.databinding.LayoutExpandableViewHolderBinding
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.local.eventbus.LandingPageCategorySelectEventBus
import com.joshtalks.joshskills.common.repository.server.FAQ
import com.joshtalks.joshskills.explore.course_details.adapters.MasterFaqAdapter
import com.joshtalks.joshskills.explore.course_details.models.FAQData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class MasterFaqViewHolder(val item: LayoutExpandableViewHolderBinding, private val testId: Int) :
    DetailsBaseViewHolder(item) {

    var categoryId: Int? = null
    private var compositeDisposable = CompositeDisposable()

    override fun bindData(sequence: Int, cardData: JsonObject) {
        val data = AppObjectController.gsonMapperForLocal.fromJson(
            cardData.toString(),
            FAQData::class.java
        )
        item.txtTitle.text = data.title
        initRV()
        initExpandableRV()
        addViews(data)
        observeBus(data)
    }

    private fun addViews(data: FAQData) {
        val listData = data.categoryList.sortedBy { it.sortOrder }
        listData.forEach { typeOfHelpModel ->
            if (categoryId == null) {
                categoryId = typeOfHelpModel.id
            }
        }
        item.recyclerView.adapter = MasterFaqAdapter(listData)
//        addView(
//            FaqCategoryViewHolder(
//                data.categoryList,
//                typeOfHelpModel,
//                typeOfHelpModel.sortOrder
//            )
//        )
        addExpandableList(data.faqList, categoryId!!)
    }

    private fun addExpandableList(faqList: List<FAQ>, categoryId: Int) {
        // TODO: Uncomment -- Imp code (Sukesh)
//        item.expandableView.removeAllViews()
//        faqList.forEach { faq ->
//            if (faq.categoryId == categoryId) {
//                item.expandableView.addView(
//                    ParentItemExpandableList(
//                        faq.question,
//                        testId,
//                        categoryId
//                    )
//                )
//                item.expandableView.addView(
//                    ChildItemExpandableList(
//                        faq.answer
//                    )
//                )
//            }
//        }
    }

    private fun observeBus(data: FAQData) {
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(LandingPageCategorySelectEventBus::class.java)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    logAnalyticsEvent()
                    MixPanelTracker.publishEvent(MixPanelEvent.COURSE_QNA_CLICKED)
                        .addParam(ParamKeys.TEST_ID, testId)
                        .addParam(ParamKeys.CATEGORY_NAME, it.selectedCategory)
                        .addParam(ParamKeys.CATEGORY_ID, it.categoryId)
                        .addParam(ParamKeys.CATEGORY_POSITION, it.position)
                        .push()

                    highlightAndShowFaq(it.position, it.categoryId, data)
                }, {
                    it.printStackTrace()
                })
        )
    }

    private fun logAnalyticsEvent() {
        AppAnalytics.create(AnalyticsEvent.QNA_CLICKED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(VERSION, PrefManager.getStringValue(VERSION))
            .push()
    }

    private fun highlightAndShowFaq(
        position: Int,
        categoryId: Int,
        data: FAQData
    ) {
        addExpandableList(data.faqList, categoryId)
//        val viewHolders = item.recyclerView.allViewResolvers as List<*>
//        viewHolders.forEach {
//            if (it is FaqCategoryViewHolder) {
//                if (it.position == position) {
//                    it.cardView.strokeColor = ResourcesCompat.getColor(
//                        getAppContext().resources,
//                        R.color.primary_500,
//                        null
//                    )
//                    TextViewCompat.setTextAppearance(
//                        it.categoryNameTV,
//                        R.style.TextAppearance_JoshTypography_Body_Text_Small_Bold
//                    )
//
//                } else {
//                    it.cardView.strokeColor = ResourcesCompat.getColor(
//                        getAppContext().resources,
//                        R.color.pure_white,
//                        null
//                    )
//                    TextViewCompat.setTextAppearance(
//                        it.categoryNameTV,
//                        R.style.TextAppearance_JoshTypography_BodyRegular20
//                    )
//                }
//            }
//        }
    }

    private fun initRV() {
        val layoutManager = GridLayoutManager(getAppContext(), 2)
        item.recyclerView.setHasFixedSize(true)
        item.recyclerView.layoutManager = layoutManager
        item.recyclerView.addItemDecoration(
            GridSpacingItemDecoration(
                2,
                Utils.dpToPx(getAppContext(), 12f),
                true
            )
        )
    }

    private fun initExpandableRV() {
        //TODO: Imp code, uncomment -- Sukesh
//        if (item.expandableView.viewAdapter == null || item.expandableView.viewAdapter.itemCount == 0) {
//            val linearLayoutManager = LinearLayoutManager(getAppContext())
//            linearLayoutManager.isSmoothScrollbarEnabled = true
//            item.expandableView.builder
//                .setHasFixedSize(true)
//                .setLayoutManager(linearLayoutManager)
//            item.expandableView.addItemDecoration(
//                LayoutMarginDecoration(
//                    Utils.dpToPx(
//                        getAppContext(),
//                        8f
//                    )
//                )
//            )
//        }
    }
}
