package com.joshtalks.joshskills.core.custom_ui;

import android.content.Context;
import android.graphics.PointF;
import android.util.DisplayMetrics;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

public class SmoothLinearLayoutManager extends LinearLayoutManager {

    public SmoothLinearLayoutManager(Context context) {
        super(context, RecyclerView.VERTICAL, false);
    }

    public SmoothLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state,
                                       int position) {
        RecyclerView.SmoothScroller smoothScroller = new TopSnappedSmoothScroller(recyclerView.getContext());
        smoothScroller.setTargetPosition(position);
        startSmoothScroll(smoothScroller);
    }

    private class TopSnappedSmoothScroller extends LinearSmoothScroller {

        public TopSnappedSmoothScroller(Context context) {
            super(context);

        }

        @Override
        public PointF computeScrollVectorForPosition
                (int targetPosition) {
            return SmoothLinearLayoutManager.this
                    .computeScrollVectorForPosition(targetPosition);
        }

        //This returns the milliseconds it takes to
        //scroll one pixel.
        @Override
        protected float calculateSpeedPerPixel
        (DisplayMetrics displayMetrics) {
            return 250f / displayMetrics.densityDpi;
        }

        @Override
        protected int getVerticalSnapPreference() {
            return SNAP_TO_END;
        }
    }
}