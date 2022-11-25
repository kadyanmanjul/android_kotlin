package com.joshtalks.joshskills.common.core.custom_ui.decorator

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class GridSpacingQuestionsDecoration( var spanCount: Int,  var spacing: Int, var includeEdge: Boolean) :
    RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        val position = parent.getChildAdapterPosition(view)
        outRect.top = spacing
        outRect.bottom = spacing
        outRect.right = spacing
    }
}