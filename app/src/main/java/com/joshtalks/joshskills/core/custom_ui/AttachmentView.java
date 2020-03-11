package com.joshtalks.joshskills.core.custom_ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.joshtalks.joshskills.R;

public class AttachmentView extends FrameLayout {

    private final AppCompatImageView imageView;
    private final AppCompatTextView textView;
    private String tag;

    public AttachmentView(Context context) {
        this(context, null);
    }

    public AttachmentView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AttachmentView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.attachment_item_view, this);
        this.imageView = findViewById(R.id.image_view);
        this.textView = findViewById(R.id.text_view);
        TypedArray attributes = context.obtainStyledAttributes(R.styleable.AttachmentView);

        Drawable drawable = attributes.getDrawable(R.styleable.AttachmentView_image_src);
        this.imageView.setImageDrawable(drawable);

        CharSequence text = attributes.getText(R.styleable.AttachmentView_text);
        this.textView.setText(text);

//        tag = attributes.getText(R.styleable.AttachmentView_tag).toString();

        attributes.recycle();


    }

}
