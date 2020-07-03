package com.joshtalks.joshskills.core.custom_ui;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

public class JoshThumbnail extends AppCompatImageView {


    private static final String TAG = JoshThumbnail.class.getCanonicalName();
    private double ratio;
    private double maxHeight = -1;

    public JoshThumbnail(Context context) {
        super(context);
    }

    public JoshThumbnail(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public JoshThumbnail(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setMaxHeight(double maxHeight) {
        this.maxHeight = maxHeight;
    }

    public void setRatio(double ratio) {
        this.ratio = ratio;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        int newHeight = (int) (width * ratio);

        if (maxHeight != -1 && newHeight > maxHeight) {
            newHeight = (int) maxHeight;
        }


        setMeasuredDimension(width, newHeight);
    }

}
