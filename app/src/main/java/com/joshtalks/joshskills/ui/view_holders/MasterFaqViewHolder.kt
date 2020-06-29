package com.joshtalks.joshskills.ui.view_holders

import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.esafirm.imagepicker.view.GridSpacingItemDecoration
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.LandingPageCategorySelectEventBus
import com.joshtalks.joshskills.repository.server.FAQ
import com.joshtalks.joshskills.repository.server.course_detail.FAQData
import com.mindorks.placeholderview.ExpandablePlaceHolderView
import com.mindorks.placeholderview.PlaceHolderView
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Recycle
import com.mindorks.placeholderview.annotations.Resolve
import com.vanniktech.emoji.Utils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

@Layout(R.layout.layout_expandable_view_holder)
class MasterFaqViewHolder(
    override val sequenceNumber: Int,
    val faqData: FAQData
) : CourseDetailsBaseCell(sequenceNumber) {

    @com.mindorks.placeholderview.annotations.View(R.id.expandableView)
    lateinit var expndableRV: ExpandablePlaceHolderView

    @com.mindorks.placeholderview.annotations.View(R.id.recycler_view)
    lateinit var recyclerView: PlaceHolderView

    var categoryId = 1
    private var compositeDisposable = CompositeDisposable()

    @Resolve
    fun onViewInflated() {
        initRV()
        initExpandableRV()
        addViews()
        observeBus()
    }

    private fun addViews() {
        recyclerView.removeAllViews()
        faqData.categoryList.sortedBy { it.sortOrder }.forEach { typeOfHelpModel ->
            recyclerView.addView(
                FaqCategoryViewHolder(
                    faqData.categoryList,
                    typeOfHelpModel,
                    typeOfHelpModel.sortOrder
                )
            )
        }
        addExpandableList(faqData.faqList, categoryId)
    }

    private fun addExpandableList(faqList: List<FAQ>, categoryId: Int) {
        expndableRV.removeAllViews()
        faqList.forEach { faq ->
            if (faq.categoryId == categoryId) {
                expndableRV.addView(
                    ParentItemExpandableList(
                        faq.question
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
            RxBus2.listen(LandingPageCategorySelectEventBus::class.java)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    highlightAndShowFaq(it.position)
                })
    }

    private fun highlightAndShowFaq(
        position: Int
    ) {
        addExpandableList(faqData.faqList, position)
        val a = recyclerView.allViewResolvers as List<*>
        a.forEach {
            if (it is FaqCategoryViewHolder) {
                if (it.position == position) {

                    it.cardView.strokeColor = ResourcesCompat.getColor(
                        AppObjectController.joshApplication.resources,
                        R.color.button_primary_color,
                        null
                    )

                } else {

                    it.cardView.strokeColor = ResourcesCompat.getColor(
                        AppObjectController.joshApplication.resources,
                        R.color.white,
                        null
                    )

                }
            }
        }
    }


    private fun initRV() {
        val layoutManager = GridLayoutManager(AppObjectController.joshApplication, 2)
        recyclerView.builder.setHasFixedSize(true)
            .setLayoutManager(layoutManager)
        recyclerView.addItemDecoration(
            GridSpacingItemDecoration(
                2,
                Utils.dpToPx(AppObjectController.joshApplication, 12f),
                true
            )
        )
    }

    private fun initExpandableRV() {
        val linearLayoutManager = LinearLayoutManager(AppObjectController.joshApplication)
        linearLayoutManager.isSmoothScrollbarEnabled = true
        expndableRV.builder
            .setHasFixedSize(true)
            .setLayoutManager(linearLayoutManager)
        expndableRV.addItemDecoration(
            LayoutMarginDecoration(
                Utils.dpToPx(
                    AppObjectController.joshApplication,
                    2f
                )
            )
        )
    }

    @Recycle
    fun onRecycled() {
        compositeDisposable.clear()
    }
}
