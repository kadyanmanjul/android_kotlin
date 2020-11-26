package com.joshtalks.joshskills.ui.groupchat.utils;

import android.content.Context;
import android.graphics.Typeface;

public class FontUtils {

    public static final String robotoMedium = "OpenSans-SemiBold.ttf";
    public static final String robotoRegular = "OpenSans-Regular.ttf";
    public static final String robotoLight = "OpenSans-Regular.ttf";
    private static FontUtils _instance;
    private final Context context;

    private FontUtils(Context context) {
        this.context = context;
    }

    public static FontUtils getInstance(Context context) {
        if (_instance == null) {
            _instance = new FontUtils(context);
        }
        return _instance;
    }

    public Typeface getTypeFace(String fontName) {
        Typeface typeface = null;
        if (context != null)
            typeface = Typeface.createFromAsset(context.getAssets(), "fonts/OpenSans-SemiBold.ttf");
        return typeface;
    }

}
