package com.joshtalks.joshskills.ui.voip.favorite

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.core.interfaces.RecyclerViewItemClickListener

class RecyclerTouchListener(
    context: Context,
    private val recyclerView: RecyclerView,
    private val listener: RecyclerViewItemClickListener?
) :
    RecyclerView.OnItemTouchListener {
    private var gestureDetector: GestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                super.onLongPress(e)
                val child: View? = recyclerView.findChildViewUnder(e.x, e.y)
                child?.let {
                    listener?.onItemLongClick(child, recyclerView.getChildAdapterPosition(child))
                }
            }
        })

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        val child: View? = rv.findChildViewUnder(e.x, e.y)
        if (child != null && gestureDetector.onTouchEvent(e)) {
            listener?.onItemClick(child, rv.getChildAdapterPosition(child))
        }

        return false

    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
    }
}