package utils;

import android.content.Context;
import android.graphics.Typeface;

public class FontUtils {

    public static final String robotoMedium = "Roboto-Medium.ttf";
    public static final String robotoBlack = "Roboto-Regular.ttf";
    public static final String robotoRegular = "Roboto-Regular.ttf";
    public static final String robotoBold = "Roboto-Bold.ttf";
    public static final String robotoLight = "Roboto-Light.ttf";
    public static final String robotoThin = "Roboto-Thin.ttf";
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
            typeface = Typeface.createFromAsset(context.getAssets(), fontName);
        return typeface;
    }

}
