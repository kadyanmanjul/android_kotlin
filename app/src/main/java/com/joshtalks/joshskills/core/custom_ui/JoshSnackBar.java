package com.joshtalks.joshskills.core.custom_ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import com.google.android.material.snackbar.Snackbar;
import com.joshtalks.joshskills.R;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


public class JoshSnackBar {

    public static final int LENGTH_INDEFINITE = Snackbar.LENGTH_INDEFINITE;
    public static final int LENGTH_SHORT = Snackbar.LENGTH_SHORT;
    public static final int LENGTH_LONG = Snackbar.LENGTH_LONG;
    private final Builder builder;

    private JoshSnackBar(Builder builder) {
        this.builder = builder;
    }

    public static Builder builder() {
        return new Builder();
    }

    private static Drawable tintDrawable(@NonNull Drawable drawable, @ColorInt int color) {
        drawable = DrawableCompat.wrap(drawable);
        drawable = drawable.mutate();
        DrawableCompat.setTint(drawable, color);
        return drawable;
    }

    private Snackbar make() {

        Snackbar chocolate = Snackbar.make(builder.view, builder.text, builder.duration);

        if (builder.actionClickListener != null || builder.actionText != null) {
            if (builder.actionClickListener == null)
                builder.actionClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                };

            chocolate.setAction(builder.actionText, builder.actionClickListener);

            if (builder.actionTextColor == null)
                builder.actionTextColor = builder.type.getStandardTextColor();

            if (builder.actionTextColors != null)
                chocolate.setActionTextColor(builder.actionTextColors);

            else if (builder.actionTextColor != null)
                chocolate.setActionTextColor(builder.actionTextColor);

        }

        Snackbar.SnackbarLayout chocolateLayout = (Snackbar.SnackbarLayout) chocolate.getView();

        if (builder.backgroundColor == null)
            builder.backgroundColor = builder.type.getColor();

        if (builder.backgroundColor != null)
            chocolateLayout.setBackgroundColor(builder.backgroundColor);

//        TextView actionText = chocolateLayout.findViewById(R.id.snackbar_action);
//
//        if (builder.actionTextSize != null) {
//            if (builder.actionTextSizeUnit != null)
//                actionText.setTextSize(builder.actionTextSizeUnit, builder.actionTextSize);
//
//            else
//                actionText.setTextSize(builder.actionTextSize);
//        }
//
//        Typeface actionTextTypeface = actionText.getTypeface();
//
//        if (builder.actionTextTypeface != null)
//            actionTextTypeface = builder.actionTextTypeface;
//
//        if (builder.actionTextTypefaceStyle != null)
//            actionText.setTypeface(actionTextTypeface, builder.actionTextTypefaceStyle);
//
//        else
//            actionText.setTypeface(actionTextTypeface);


       // TextView text = chocolateLayout.findViewById(R.id.snackbar_text);

//        if (builder.textSize != null) {
//            if (builder.textSizeUnit != null)
//                text.setTextSize(builder.textSizeUnit, builder.textSize);
//
//            else
//                text.setTextSize(builder.textSize);
//        }

       // Typeface textTypeface = text.getTypeface();

//        if (builder.textTypeface != null)
//            textTypeface = builder.textTypeface;
//
//        if (builder.textTypefaceStyle != null)
//            text.setTypeface(textTypeface, builder.textTypefaceStyle);
//
//        else
//            text.setTypeface(textTypeface);


        if (builder.textColor == null)
            builder.textColor = builder.type.getStandardTextColor();

//        if (builder.textColors != null)
//            text.setTextColor(builder.textColors);
//
//        else if (builder.textColor != null)
//            text.setTextColor(builder.textColor);

//        text.setMaxLines(builder.maxLines);
//        text.setGravity(builder.centerText ? Gravity.CENTER : Gravity.CENTER_VERTICAL);
//
//        if (builder.centerText)
//            text.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        if (builder.icon == null)
            builder.icon = builder.type.getIcon(builder.view.getContext());

        if (builder.icon != null) {

            Drawable transparentHelperDrawable = null;

            if (builder.centerText && TextUtils.isEmpty(builder.actionText)) {
                transparentHelperDrawable = makeTransparentDrawable(builder.view.getContext(),
                        builder.icon.getIntrinsicWidth(),
                        builder.icon.getIntrinsicHeight());
            }

            Configuration configuration = chocolateLayout.getResources().getConfiguration();
           // if (configuration.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL)
               // text.setCompoundDrawablesWithIntrinsicBounds(transparentHelperDrawable, null, builder.icon, null);
           // else
                //text.setCompoundDrawablesWithIntrinsicBounds(builder.icon, null, transparentHelperDrawable, null);
           // text.setCompoundDrawablePadding(text.getResources().getDimensionPixelOffset(R.dimen.dp16));
        }


        return chocolate;
    }

    private static Drawable makeTransparentDrawable(Context context, int width, int height) {
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(width, height, conf);
        return new BitmapDrawable(context.getResources(), bmp);
    }


    private enum Type {
        DEFAULT(null, null, null), GREEN(Color.parseColor("#388E3C"), 0, Color.WHITE), RED(Color.parseColor("#D50000"), 0, Color.WHITE), CYAN(Color.parseColor("#e0ffff"), 0, Color.WHITE), ORANGE(Color.parseColor("#ffa500"), 0, Color.BLACK), GOOD(Color.parseColor("#C5BEBE"), 0, Color.WHITE), BAD(Color.parseColor("#C5BEBE"), 0, Color.WHITE);

        private Integer color;
        private Integer iconResId;
        private Integer standardTextColor;

        Type(@ColorInt Integer color, @DrawableRes Integer iconResId, @ColorInt Integer standardTextColor) {
            this.color = color;

            this.iconResId = iconResId;
            this.standardTextColor = standardTextColor;
        }

        public Integer getColor() {
            return color;
        }

        public Drawable getIcon(Context context) {
            if (iconResId == null)
                return null;

            Drawable drawable = ContextCompat.getDrawable(context, iconResId);

            if (drawable != null)
                drawable = tintDrawable(drawable, standardTextColor);

            return drawable;
        }


        public Integer getStandardTextColor() {
            return standardTextColor;
        }
    }

    @IntDef({LENGTH_INDEFINITE, LENGTH_SHORT, LENGTH_LONG})
    @IntRange(from = 1)
    @Retention(RetentionPolicy.SOURCE)
    public @interface Duration {
    }

    public static class Builder {

        private View view = null;
        private Type type = Type.DEFAULT;
        private int duration = Snackbar.LENGTH_SHORT;
        private CharSequence text = "";
        private int textResId = 0;
        private Integer textColor = null;
        private ColorStateList textColors = null;
        private Integer textSizeUnit = null;
        private Float textSize = null;
        private Integer textTypefaceStyle = null;
        private Typeface textTypeface = null;
        private Integer actionTextSizeUnit = null;
        private Float actionTextSize = null;
        private CharSequence actionText = "";
        private int actionTextResId = 0;
        private Integer actionTextTypefaceStyle = null;
        private Typeface actionTextTypeface = null;
        private View.OnClickListener actionClickListener = null;
        private Integer actionTextColor = null;
        private ColorStateList actionTextColors = null;
        private int maxLines = Integer.MAX_VALUE;
        private boolean centerText = false;
        private Drawable icon = null;
        private int iconResId = 0;
        private Integer backgroundColor = null;

        private Builder() {
        }

        public Builder setActivity(Activity activity) {
            return setView(((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0));
        }

        public Builder setView(View view) {
            this.view = view;
            return this;
        }

        public Builder setText(@StringRes int resId) {
            this.textResId = resId;
            return this;
        }

        public Builder setText(CharSequence text) {
            this.textResId = 0;
            this.text = text;
            return this;
        }

        public Builder setTextColor(@ColorInt int color) {
            this.textColor = color;
            return this;
        }

        public Builder setTextColor(ColorStateList colorStateList) {
            this.textColor = null;
            this.textColors = colorStateList;
            return this;
        }

        public Builder setTextSize(float textSize) {
            this.textSizeUnit = null;
            this.textSize = textSize;
            return this;
        }

        public Builder setTextSize(int unit, float textSize) {
            this.textSizeUnit = unit;
            this.textSize = textSize;
            return this;
        }

        public Builder setTextTypeface(Typeface typeface) {
            this.textTypeface = typeface;
            return this;
        }

        public Builder setTextTypefaceStyle(int style) {
            this.textTypefaceStyle = style;
            return this;
        }

        public Builder centerText() {
            this.centerText = true;
            return this;
        }

        public Builder setActionTextColor(ColorStateList colorStateList) {
            this.actionTextColor = null;
            this.actionTextColors = colorStateList;
            return this;
        }

        public Builder setActionTextColor(@ColorInt int color) {
            this.actionTextColor = color;
            return this;
        }

        public Builder setActionText(@StringRes int resId) {
            this.actionTextResId = resId;
            return this;
        }

        public Builder setActionText(CharSequence text) {
            this.textResId = 0;
            this.actionText = text;
            return this;
        }

        public Builder setActionTextSize(float textSize) {
            this.actionTextSizeUnit = null;
            this.actionTextSize = textSize;
            return this;
        }

        public Builder setActionTextSize(int unit, float textSize) {
            this.actionTextSizeUnit = unit;
            this.actionTextSize = textSize;
            return this;
        }

        public Builder setActionTextTypeface(Typeface typeface) {
            this.actionTextTypeface = typeface;
            return this;
        }


        public Builder setActionTextTypefaceStyle(int style) {
            this.actionTextTypefaceStyle = style;
            return this;
        }

        public Builder setActionClickListener(View.OnClickListener listener) {
            this.actionClickListener = listener;
            return this;
        }

        public Builder setMaxLines(int maxLines) {
            this.maxLines = maxLines;
            return this;
        }

        public Builder setDuration(@Duration int duration) {
            this.duration = duration;
            return this;
        }

        public Builder setIcon(@DrawableRes int resId) {
            this.iconResId = resId;
            return this;
        }

        public Builder setIcon(Drawable drawable) {
            this.icon = drawable;
            return this;
        }

        public Builder setBackgroundColor(@ColorInt int color) {
            this.backgroundColor = color;
            return this;
        }

        public Snackbar build() {
            return make();
        }

        private Snackbar make() {
            if (view == null)
                throw new IllegalStateException("ChocoBar Error: You must set an Activity or a View before making a snack");

            if (textResId != 0)
                text = view.getResources().getText(textResId);

            if (actionTextResId != 0)
                actionText = view.getResources().getText(actionTextResId);

            if (iconResId != 0)
                icon = ContextCompat.getDrawable(view.getContext(), iconResId);

            return new JoshSnackBar(this).make();
        }

        public Snackbar green() {
            type = Type.GREEN;
            return make();
        }

        public Snackbar cyan() {
            type = Type.CYAN;
            return make();
        }

        public Snackbar orange() {
            type = Type.ORANGE;
            return make();
        }

        public Snackbar red() {
            type = Type.RED;
            return make();
        }

        public Snackbar good() {
            type = Type.GOOD;
            return make();
        }

        public Snackbar bad() {
            type = Type.BAD;
            return make();
        }
    }

}
