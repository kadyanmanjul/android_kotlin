package com.joshtalks.joshskills.core.custom_ui.progress;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.joshtalks.joshskills.R;
import java.util.ArrayList;
import java.util.List;


public class FlipProgressDialog extends DialogFragment {

    ImageView image;
    int counter = 0;
    Handler handler = new Handler();

    Runnable r;

    //Set Animation stuff
    private int duration = 600;
    private String orientation = "rotationY";
    private float startAngle = 0.0f;
    private float endAngle = 180.0f;
    private float minAlpha = 0.0f;
    private float maxAlpha = 1.0f;

    private int backgroundColor = -1;
    private float backgroundAlpha = 0.5f;
    private int backgroundColorWIthAlpha = 452984831;
    private int borderStroke = 0;
    private int borderColor = -1;
    private int cornerRadius = 16;
    private float dimAmount = 0.7f;

    // Set Image settings
    private int imageMargin = 10;
    private int imageSize = 200;
    private List<Integer> imageList = new ArrayList<Integer>();

    // Set cancelOnTouch
    private boolean canceledOnTouchOutside = true;


    public FlipProgressDialog() {

    }

    public void setImage(ImageView image) {
        this.image = image;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setOrientation(String orientation) {
        this.orientation = orientation;
    }

    public void setStartAngle(float startAngle) {
        this.startAngle = startAngle;
    }

    public void setEndAngle(float endAngle) {
        this.endAngle = endAngle;
    }

    public void setMinAlpha(float minAlpha) {
        this.minAlpha = minAlpha;
    }

    public void setMaxAlpha(float maxAlpha) {
        this.maxAlpha = maxAlpha;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setBackgroundAlpha(float backgroundAlpha) {
        this.backgroundAlpha = backgroundAlpha;
    }

    public void setBorderStroke(int borderStroke) {
        this.borderStroke = borderStroke;
    }

    public void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
    }

    public void setCornerRadius(int cornerRadius) {
        this.cornerRadius = cornerRadius;
    }

    public void setDimAmount(float dimAmount) {
        this.dimAmount = dimAmount;
    }

    public void setImageMargin(int imageMargin) {
        this.imageMargin = imageMargin;
    }

    public void setImageSize(int imageSize) {
        this.imageSize = imageSize;
    }

    public void setImageList(List<Integer> imageList) {
        this.imageList = imageList;
    }

    public void setCanceledOnTouchOutside(boolean canceledOnTouchOutside) {
        this.canceledOnTouchOutside = canceledOnTouchOutside;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_dialog, container, false);

        try {
            getDialog().setCanceledOnTouchOutside(canceledOnTouchOutside);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return view;
    }

 /*   override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            dialog.window?.setLayout(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
    }*/

    private void animateAnimatorSetSample(ImageView target) {

        // Set AnimatorList(Will set on AnimatorSet)
        List<Animator> animatorList = new ArrayList<Animator>();

        // alphaプロパティを0fから1fに変化させます
        PropertyValuesHolder alphaAnimator = PropertyValuesHolder.ofFloat("alpha", minAlpha, maxAlpha, minAlpha);

        PropertyValuesHolder flipAnimator = PropertyValuesHolder.ofFloat(orientation, startAngle, endAngle);

        ObjectAnimator translationAnimator =
                ObjectAnimator.ofPropertyValuesHolder(target, alphaAnimator, flipAnimator);
        translationAnimator.setDuration(duration);
        translationAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
//		// faster
//		translationAnimator.setInterpolator(new AccelerateInterpolator());
//		// slower
//		translationAnimator.setInterpolator(new DecelerateInterpolator());
//		// fast->slow->fast
//		translationAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        // repeat translationAnimator
        translationAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        // Set all animation to animatorList
        animatorList.add(translationAnimator);

        final AnimatorSet animatorSet = new AnimatorSet();
        // Set animatorList to animatorSet
        animatorSet.playSequentially(animatorList);

        // Start Animation
        animatorSet.start();
    }

    public void fullRound() {
        this.duration = duration * 2;
        this.endAngle = endAngle * 2;
    }

    @Override
    public void dismiss() {
        super.dismiss();

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, R.style.full_dialog);

    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            setBackgroundDim();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onDestroyView() {
        try {
            Dialog dialog = getDialog();
            if (dialog != null && getRetainInstance()) {
                dialog.setDismissMessage(null);
            }
            handler.removeCallbacks(r);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroyView();
    }

    private void setBackgroundDim() {
        try {
            Window window = getDialog().getWindow();
            WindowManager.LayoutParams windowParams = window.getAttributes();
            windowParams.dimAmount = dimAmount;
            windowParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            window.setAttributes(windowParams);
        } catch (Exception e) {
        }
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        try {
            FragmentTransaction ft = manager.beginTransaction();
            ft.add(this, tag);
            ft.commit();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }
}