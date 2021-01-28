package com.joshtalks.recordview;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;

import info.kimjihyok.ripplelibrary.VoiceRippleView;
import info.kimjihyok.ripplelibrary.renderer.TimerCircleRippleRenderer;

public class CustomRippleButton extends VoiceRippleView implements View.OnTouchListener, View.OnClickListener {

    private RecordView recordView;
    private boolean listenForRecord = true;
    private OnRecordClickListener onRecordClickListener;
    private OnRecordTouchListener onRecordTouchListener;

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
            TimerCircleRippleRenderer currentRenderer = new TimerCircleRippleRenderer(getDefaultRipplePaint(), getDefaultRippleBackgroundPaint(), getButtonPaint(), getArcPaint(), 10000000.0, 0.0);
            currentRenderer.setStrokeWidth(16);
            setRenderer(currentRenderer);

        }

        setBackgroundRippleRatio(2.0);
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

    private Paint getArcPaint() {
        Paint paint = new Paint();
        paint.setColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        paint.setStrokeWidth(16);
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