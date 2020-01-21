package com.joshtalks.joshskills.core.custom_ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.joshtalks.joshskills.R;


public class AnimatingToggle extends FrameLayout {

    private final Animation inAnimation;
    private final Animation outAnimation;
    private View current;

    public AnimatingToggle(Context context) {
        this(context, null);
    }

    public AnimatingToggle(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnimatingToggle(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.outAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.animation_toggle_out);
        this.inAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.animation_toggle_in);
        this.outAnimation.setInterpolator(new FastOutSlowInInterpolator());
        this.inAnimation.setInterpolator(new FastOutSlowInInterpolator());
    }

    @Override
    public void addView(@NonNull View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);

        if (getChildCount() == 1) {
            current = child;
            child.setVisibility(View.VISIBLE);
        } else {
            child.setVisibility(View.GONE);
        }
        child.setClickable(false);
    }

    public void display(@Nullable View view) {
        if (view == current) return;
        if (current != null) animateOut(current, outAnimation, View.GONE);
        if (view != null) animateIn(view, inAnimation);

        current = view;
    }

    public void displayQuick(@Nullable View view) {
        if (view == current) return;
        if (current != null) current.setVisibility(View.GONE);
        if (view != null) view.setVisibility(View.VISIBLE);

        current = view;
    }


    public void animateOut(final @NonNull View view, final @NonNull Animation animation, final int visibility) {
        if (view.getVisibility() == visibility) {
        } else {
            view.clearAnimation();
            animation.reset();
            animation.setStartTime(0);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    view.setVisibility(visibility);
                }
            });
            view.startAnimation(animation);
        }

    }

    public void animateIn(final @NonNull View view, final @NonNull Animation animation) {
        if (view.getVisibility() == View.VISIBLE) return;

        view.clearAnimation();
        animation.reset();
        animation.setStartTime(0);
        view.setVisibility(View.VISIBLE);
        view.startAnimation(animation);
    }

}
