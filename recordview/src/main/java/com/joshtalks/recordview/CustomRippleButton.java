package com.joshtalks.recordview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ScaleDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import androidx.core.content.ContextCompat;

import info.kimjihyok.ripplelibrary.VoiceRippleView;
import info.kimjihyok.ripplelibrary.renderer.TimerCircleRippleRenderer;

public class CustomRippleButton extends VoiceRippleView implements View.OnTouchListener, View.OnClickListener {

    public static final int FIRST_STATE = 1;
    public static final int SECOND_STATE = 2;
    private static final Interpolator interpolator = new DecelerateInterpolator();
    private static final float SCALE_TOTAL = 1f;
    private static final float ALPHA_TOTAL = 255;
    private final boolean init = false;
    private ScaleAnim scaleAnim;
    private RecordView recordView;
    private boolean listenForRecord = true;
    private OnRecordClickListener onRecordClickListener;
    private OnRecordTouchListener onRecordTouchListener;
    private int state = FIRST_STATE;
    private int duration = 200;


    public CustomRippleButton(Context context) {
        super(context);
        init(context, null);
    }

    public CustomRippleButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CustomRippleButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public void setRecordView(RecordView recordView) {
        this.recordView = recordView;
        reset();
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.CustomImageButton, 0, 0);
            duration = array.getInteger(R.styleable.CustomImageButton_duration, duration);
            array.recycle();

            TimerCircleRippleRenderer currentRenderer = new TimerCircleRippleRenderer(getDefaultRipplePaint(), getDefaultRippleBackgroundPaint(), getButtonPaint(), getArcPaint(), 10000000.0, 0.0);
            currentRenderer.setStrokeWidth(10);
            setRenderer(currentRenderer);

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
        try {
            if (isListenForRecord()) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        recordView.onActionDown(v, event);
                        break;

                    case MotionEvent.ACTION_MOVE:
                        recordView.onActionMove(v, event);
                        break;

                    case MotionEvent.ACTION_UP:
                        recordView.onActionUp(v);
                        break;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (onRecordTouchListener != null) {
            onRecordTouchListener.onTouch(event.getAction());
        }
        return isListenForRecord();
    }


    protected void startScale() {
        scaleAnim.start();
    }

    protected void stopScale() {
        scaleAnim.stop();
    }

    public boolean isListenForRecord() {
        return listenForRecord;
    }

    public void setListenForRecord(boolean listenForRecord) {
        this.listenForRecord = listenForRecord;
    }

    public void setOnRecordClickListener(OnRecordClickListener onRecordClickListener) {
        this.onRecordClickListener = onRecordClickListener;
    }

    public void setOnTouchListener(OnRecordTouchListener onRecordTouchListener) {
        this.onRecordTouchListener = onRecordTouchListener;
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
//                animate(firstDrawable, secondDrawable);
                break;
            case SECOND_STATE:
//                animate(secondDrawable, firstDrawable);
                break;
        }

        this.state = state;
    }

    public int getState() {
        return state;
    }

    private Drawable makeDrawable(Drawable drawable, float scale, int alpha) {
        ScaleDrawable scaleDrawable = new ScaleDrawable(drawable, 0, scale, scale);
        scaleDrawable.setLevel(1);
        scaleDrawable.setAlpha(alpha);
        return scaleDrawable;
    }


    private Paint getArcPaint() {
        Paint paint = new Paint();
        paint.setColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        paint.setStrokeWidth(10);
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.SQUARE);
        paint.setStyle(Paint.Style.STROKE);
        return paint;
    }

    private Paint getDefaultRipplePaint() {
        Paint ripplePaint = new Paint();
        ripplePaint.setStyle(Paint.Style.FILL);
        ripplePaint.setColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        ripplePaint.setAntiAlias(true);

        return ripplePaint;
    }

    private Paint getDefaultRippleBackgroundPaint() {
        Paint rippleBackgroundPaint = new Paint();
        rippleBackgroundPaint.setStyle(Paint.Style.FILL);
        rippleBackgroundPaint.setColor((ContextCompat.getColor(getContext(), R.color.colorPrimary) & 0x00FFFFFF) | 0x40000000);
        rippleBackgroundPaint.setAntiAlias(true);

        return rippleBackgroundPaint;
    }

    private Paint getButtonPaint() {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        paint.setStyle(Paint.Style.FILL);
        return paint;
    }

}