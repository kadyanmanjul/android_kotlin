package com.joshtalks.joshskills.ui.chat;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import com.joshtalks.joshskills.databinding.ActivityConversationBinding;

import java.util.ArrayList;

public class AttachmentUtil {


    public static void revealAttachments(boolean reveal, ActivityConversationBinding conversationBinding) {

        int w = conversationBinding.attachmentContainer.getWidth();
        int h = conversationBinding.attachmentContainer.getHeight();


        int finalRadius = (int) Math.hypot(w, h);


        int cx = (int) (conversationBinding.quickToggle.getX() + (conversationBinding.quickToggle.getWidth()/2));
        int cy = (int) (conversationBinding.quickToggle.getY())+ conversationBinding.quickToggle.getHeight() + 56;

        if (!reveal) {
            Animator anim = ViewAnimationUtils.createCircularReveal(conversationBinding.attachmentContainer, cx, cy, 0, finalRadius);
            anim.addListener(new AnimatorListenerAdapter() {

                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    childAnimate((ViewGroup) conversationBinding.attachmentContainer);
                }

            });
            conversationBinding.attachmentContainer.setVisibility(View.VISIBLE);
            anim.setDuration(350);
            anim.start();
        } else {
            Animator anim = ViewAnimationUtils.createCircularReveal(conversationBinding.attachmentContainer, cx, cy, finalRadius, 0);
            anim.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    setViewVisibility(conversationBinding.attachmentContainer, false);

                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            anim.setDuration(350);
            anim.start();
        }
    }

    static void childAnimate(ViewGroup viewGroup) {
        LinearLayout subChild = (LinearLayout) viewGroup.getChildAt(0);
        int startTime=0;
        for (int i = 0; i < subChild.getChildCount(); i++) {
            animateView(subChild.getChildAt(i),startTime);
            startTime+=50;
        }
    }

    private static void animateView(View view, int delay) {

        view.setScaleY(0f);
        view.setScaleX(0f);
        view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setInterpolator(new OvershootInterpolator(1.f))
                .setDuration(200)
                .setStartDelay(0);
    }

    public static void setViewVisibility(View v, boolean isVisible) {
        v.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }
}