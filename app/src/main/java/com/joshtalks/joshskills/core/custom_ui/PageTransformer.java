package com.joshtalks.joshskills.core.custom_ui;

import android.view.View;
import androidx.viewpager.widget.ViewPager;

public final class PageTransformer implements ViewPager.PageTransformer {
    @Override
    public void transformPage(View page, float position) {
        int width = page.getWidth();
        if (position < -1) {
            page.setScrollX((int) (width * 0.75 * -1));
        } else if (position <= 1) {
            if (position < 0) {
                page.setScrollX((int) (width * 0.75 * position));
            } else {
                page.setScrollX((int) (width * 0.75 * position));
            }
        } else {
            page.setScrollX((int) (width * 0.75));
        }
    }
}
