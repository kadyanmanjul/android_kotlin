package com.joshtalks.joshskills.ui.userprofile

import android.content.Context
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.server.Award
import com.mindorks.placeholderview.PlaceHolderView
import com.mindorks.placeholderview.SmoothLinearLayoutManager
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View

@Layout(R.layout.award_view_holder)
class AwardViewHolder(var listAward: List<Award>, var context: Context) {

    @View(R.id.title)
    lateinit var title: AppCompatTextView

    @View(R.id.award_rv)
    lateinit var recyclerView: PlaceHolderView

    lateinit var linearLayoutManager: SmoothLinearLayoutManager

    @Resolve
    fun onViewInflated() {
        initRv()
        initView()
    }

    private fun initRv() {
        val linearLayoutManager = SmoothLinearLayoutManager(
            context,
            LinearLayoutManager.HORIZONTAL,
            true
        )
        linearLayoutManager.isSmoothScrollbarEnabled = true
        recyclerView.builder.setHasFixedSize(true)
            .setLayoutManager(linearLayoutManager)
    }

    private fun initView() {
        var text = listAward.get(0).awardText
        if (listAward == null) {
            text = "Certificates"
        }
        title.text = text
        listAward.forEach {
            recyclerView.addView(AwardItemViewHolder(it, context))
        }
        recyclerView.getLayoutManager()?.scrollToPosition(listAward.size);
    }
}

