package com.joshtalks.joshskills.ui.course_details.viewholder

import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.VERSION
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.analytics.ParamKeys
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.core.custom_ui.decorator.GridSpacingItemDecoration
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.LandingPageCategorySelectEventBus
import com.joshtalks.joshskills.repository.server.FAQ
import com.joshtalks.joshskills.repository.server.course_detail.CardType
import com.joshtalks.joshskills.repository.server.course_detail.FAQData
import com.joshtalks.joshskills.ui.help.viewholder.FaqCategoryViewHolder
import com.mindorks.placeholderview.ExpandablePlaceHolderView
import com.mindorks.placeholderview.PlaceHolderView
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Recycle
import com.mindorks.placeholderview.annotations.Resolve
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

@Layout(R.layout.layout_expandable_view_holder)
class MasterFaqViewHolder(
    override val type: CardType,
    override val sequenceNumber: Int,
    private val faqData: FAQData,
    private val testId : Int
) : CourseDetailsBaseCell(type, sequenceNumber) {

    @com.mindorks.placeholderview.annotations.View(R.id.txt_title)
    lateinit var txtTitle: JoshTextView

    @com.mindorks.placeholderview.annotations.View(R.id.expandableView)
    lateinit var expndableRV: ExpandablePlaceHolderView

    @com.mindorks.placeholderview.annotations.View(R.id.recycler_view)
    lateinit var recyclerView: PlaceHolderView

    var categoryId: Int? = null
    private var compositeDisposable = CompositeDisposable()

    @Resolve
    fun onViewInflated() {
        txtTitle.text = faqData.title
        initRV()
        initExpandableRV()
        addViews()
        observeBus()
    }

    private fun addViews() {
        if (recyclerView.viewAdapter == null || recyclerView.viewAdapter.itemCount == 0) {
            faqData.categoryList.sortedBy { it.sortOrder }.forEach { typeOfHelpModel ->
                if (categoryId == null) {
                    categoryId = typeOfHelpModel.id
                }
                recyclerView.addView(
                    FaqCategoryViewHolder(
                        faqData.categoryList,
                        typeOfHelpModel,
                        typeOfHelpModel.sortOrder
                    )
                )
            }
            addExpandableList(faqData.faqList, categoryId!!)
        }
    }

    private fun addExpandableList(faqList: List<FAQ>, categoryId: Int) {
        expndableRV.removeAllViews()
        faqList.forEach { faq ->
            if (faq.categoryId == categoryId) {
                expndableRV.addView(
                    ParentItemExpandableList(
                        faq.question,
                        testId,
                        categoryId
                    )
                )
                expndableRV.addView(
                    ChildItemExpandableList(
                        faq.answer
                    )
                )
            }
        }
    }

    private fun observeBus() {
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(LandingPageCategorySelectEventBus::class.java)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    logAnalyticsEvent()
                    MixPanelTracker.publishEvent(MixPanelEvent.COURSE_QNA_CLICKED)
                        .addParam(ParamKeys.TEST_ID,testId)
                        .addParam(ParamKeys.CATEGORY_NAME,it.selectedCategory)
                        .addParam(ParamKeys.CATEGORY_ID,it.categoryId)
                        .addParam(ParamKeys.CATEGORY_POSITION,it.position)
                        .push()

                    highlightAndShowFaq(it.position, it.categoryId)
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
        categoryId: Int
    ) {
        addExpandableList(faqData.faqList, categoryId)
        val viewHolders = recyclerView.allViewResolvers as List<*>
        viewHolders.forEach {
            if (it is FaqCategoryViewHolder) {
                if (it.position == position) {
                    it.cardView.strokeColor = ResourcesCompat.getColor(
                        getAppContext().resources,
                        R.color.button_color,
                        null
                    )
                    TextViewCompat.setTextAppearance(
                        it.categoryNameTV,
                        R.style.TextAppearance_JoshTypography_Body_Text_Small_Bold
                    )

                } else {
                    it.cardView.strokeColor = ResourcesCompat.getColor(
                        getAppContext().resources,
                        R.color.white,
                        null
                    )
                    TextViewCompat.setTextAppearance(
                        it.categoryNameTV,
                        R.style.TextAppearance_JoshTypography_Body_Text_Small_Regular
                    )
                }
            }
        }
    }


    private fun initRV() {
        if (recyclerView.viewAdapter == null || recyclerView.viewAdapter.itemCount == 0) {
            val layoutManager = GridLayoutManager(getAppContext(), 2)
            recyclerView.builder.setHasFixedSize(true)
                .setLayoutManager(layoutManager)
            recyclerView.addItemDecoration(
                GridSpacingItemDecoration(
                    2,
                    Utils.dpToPx(getAppContext(), 12f),
                    true
                )
            )
        }
    }

    private fun initExpandableRV() {
        if (expndableRV.viewAdapter == null || expndableRV.viewAdapter.itemCount == 0) {
            val linearLayoutManager = LinearLayoutManager(getAppContext())
            linearLayoutManager.isSmoothScrollbarEnabled = true
            expndableRV.builder
                .setHasFixedSize(true)
                .setLayoutManager(linearLayoutManager)
            expndableRV.addItemDecoration(
                LayoutMarginDecoration(
                    Utils.dpToPx(
                        getAppContext(),
                        8f
                    )
                )
            )
        }
    }

    @Recycle
    fun onRecycled() {
        compositeDisposable.clear()
    }
}
