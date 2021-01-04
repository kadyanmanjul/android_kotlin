package com.joshtalks.joshskills.core.custom_ui.blurdialog;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.renderscript.Allocation;
import androidx.renderscript.Element;
import androidx.renderscript.RSRuntimeException;
import androidx.renderscript.RenderScript;
import androidx.renderscript.ScriptIntrinsicBlur;

/**
 * Simple helper used to blur a bitmap thanks to render script.
 */
final class RenderScriptBlurHelper {

    /**
     * Log cat
     */
    private static final String TAG = RenderScriptBlurHelper.class.getSimpleName();

    /**
     * Non instantiable class.
     */
    private RenderScriptBlurHelper() {

    }

    /**
     * blur a given bitmap
     *
     * @param sentBitmap       bitmap to blur
     * @param radius           blur radius
     * @param canReuseInBitmap true if bitmap must be reused without blur
     * @param context          used by RenderScript, can be null if RenderScript disabled
     * @return blurred bitmap
     */
    public static Bitmap doBlur(Bitmap sentBitmap, int radius, boolean canReuseInBitmap, Context context) {
        Bitmap bitmap;

        if (canReuseInBitmap) {
            bitmap = sentBitmap;
        } else {
            bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
        }

        if (bitmap.getConfig() == Bitmap.Config.RGB_565) {
            // RenderScript hates RGB_565 so we convert it to ARGB_8888
            // (see http://stackoverflow.com/questions/21563299/
            // defect-of-image-with-scriptintrinsicblur-from-support-library)
            bitmap = convertRGB565toARGB888(bitmap);
        }

        try {

            RenderScript rs = RenderScript.create(context);
            ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            Allocation tmpIn = Allocation.createFromBitmap(rs, sentBitmap);
            Allocation tmpOut = Allocation.createFromBitmap(rs, bitmap);
            blurScript.setRadius(radius);
            blurScript.setInput(tmpIn);
            blurScript.forEach(tmpOut);
            tmpOut.copyTo(bitmap);
            return bitmap;
        } catch (RSRuntimeException e) {
            Log.e(TAG, "RenderScript known error : https://code.google.com/p/android/issues/detail?id=71347 "
                    + "continue with the FastBlur approach.");
        }

        return null;
    }

    private static Bitmap convertRGB565toARGB888(Bitmap bitmap) {
        return bitmap.copy(Bitmap.Config.ARGB_8888, true);
    }
}
