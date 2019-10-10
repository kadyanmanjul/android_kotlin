package com.joshtalks.recordview;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;

public class CustomImageButton extends AppCompatImageView implements View.OnTouchListener, View.OnClickListener {

    private static final Interpolator interpolator = new DecelerateInterpolator();

    public static final int FIRST_STATE = 1;
    public static final int SECOND_STATE = 2;
    private static final float SCALE_TOTAL = 1f;
    private static final float ALPHA_TOTAL = 255;

    private ScaleAnim scaleAnim;
    private RecordView recordView;
    private boolean listenForRecord = true;
    private OnRecordClickListener onRecordClickListener;

    private Drawable firstDrawable;
    private Drawable secondDrawable;

    private int state = FIRST_STATE;
    private int duration = 200;
    private boolean init = false;


    public void setRecordView(RecordView recordView) {
        this.recordView = recordView;
    }

    public CustomImageButton(Context context) {
        super(context);
        init(context, null);
    }

    public CustomImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CustomImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);


    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {


            TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.CustomImageButton, 0, 0);

            int first = array.getResourceId(R.styleable.CustomImageButton_image_first, -1);
            int second = array.getResourceId(R.styleable.CustomImageButton_image_second, -1);
            duration = array.getInteger(R.styleable.CustomImageButton_duration, duration);

            array.recycle();

            if (first > 0 && second > 0) {
                init = true;
                firstDrawable = AppCompatResources.getDrawable(getContext(), first);
                secondDrawable = AppCompatResources.getDrawable(getContext(), second);
                setImageDrawable(firstDrawable);
            }


        }

        scaleAnim = new ScaleAnim(this);
        this.setOnTouchListener(this);
        this.setOnClickListener(this);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setClip(this);
    }

    public void setClip(View v) {
        if (v.getParent() == null) {
            return;
        }

        if (v instanceof ViewGroup) {
            ((ViewGroup) v).setClipChildren(false);
            ((ViewGroup) v).setClipToPadding(false);
        }

        if (v.getParent() instanceof View) {
            setClip((View) v.getParent());
        }
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (isListenForRecord()) {
            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    recordView.onActionDown((CustomImageButton) v, event);
                    break;


                case MotionEvent.ACTION_MOVE:
                    recordView.onActionMove((CustomImageButton) v, event);
                    break;

                case MotionEvent.ACTION_UP:
                    recordView.onActionUp((CustomImageButton) v);
                    break;

            }

        }
        return isListenForRecord();
    }


    protected void startScale() {
        scaleAnim.start();
    }

    protected void stopScale() {
        scaleAnim.stop();
    }

    public void setListenForRecord(boolean listenForRecord) {
        this.listenForRecord = listenForRecord;
    }

    public boolean isListenForRecord() {
        return listenForRecord;
    }

    public void setOnRecordClickListener(OnRecordClickListener onRecordClickListener) {
        this.onRecordClickListener = onRecordClickListener;
    }


    @Override
    public void onClick(View v) {
        if (onRecordClickListener != null)
            onRecordClickListener.onClick(v);
    }

    public void goToState(int state) {
        if (!init || this.state == state) return;

        switch (state) {
            case FIRST_STATE:
                animate(firstDrawable, secondDrawable);
                break;
            case SECOND_STATE:
                animate(secondDrawable, firstDrawable);
                break;
        }

        this.state = state;
    }

    public int getState() {
        return state;
    }


    private void animate(final Drawable from, final Drawable to) {
        //setScaleType(ScaleType.CENTER_INSIDE);

        ValueAnimator animator = ValueAnimator.ofFloat(0, SCALE_TOTAL);
        animator.setDuration(duration);
        animator.setInterpolator(interpolator);

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                float left = SCALE_TOTAL - value;

                Drawable firstInset = makeDrawable(from, left, (int) (value * ALPHA_TOTAL));
                Drawable secondInset = makeDrawable(to, value, (int) (left * ALPHA_TOTAL));

                setImageDrawable(new LayerDrawable(new Drawable[]{firstInset, secondInset}));
            }
        });

        animator.start();
    }

    private Drawable makeDrawable(Drawable drawable, float scale, int alpha) {
        ScaleDrawable scaleDrawable = new ScaleDrawable(drawable, 0, scale, scale);
        scaleDrawable.setLevel(1);
        scaleDrawable.setAlpha(alpha);

        return scaleDrawable;
    }

}