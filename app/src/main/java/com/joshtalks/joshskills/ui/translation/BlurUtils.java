package com.joshtalks.joshskills.ui.translation;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.FloatRange;
import androidx.renderscript.Allocation;
import androidx.renderscript.Element;
import androidx.renderscript.RenderScript;
import androidx.renderscript.ScriptIntrinsicBlur;


public class BlurUtils {

    public static Bitmap blur(Context context, Bitmap bitmap, @FloatRange(from = 0, to = 25) float radius) {
//            final Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);

        Bitmap inputBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), false);
        Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);
        RenderScript rs = RenderScript.create(context);
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        Allocation tmpIn = Allocation.createFromBitmap(rs, inputBitmap);
        Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);
        blurScript.setRadius(radius);
        blurScript.setInput(tmpIn);
        blurScript.forEach(tmpOut);
        tmpOut.copyTo(outputBitmap);
        return outputBitmap;
    }

}