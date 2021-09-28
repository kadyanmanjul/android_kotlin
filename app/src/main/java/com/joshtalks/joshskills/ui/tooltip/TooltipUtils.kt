package com.joshtalks.joshskills.ui.tooltip

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import android.view.View
import com.joshtalks.joshskills.ui.leaderboard.ItemOverlay

private const val TAG = "TooltipUtils"
class TooltipUtils {

    companion object {
        fun getOverlayItemFromView(view: View): ItemOverlay? {
            Log.d(TAG, "getBitmapFromView: Width -- ${view.width}")
            Log.d(TAG, "getBitmapFromView: Height -- ${view.height}")
            if(view.width <=0 || view.height <=0)
                return null
            val position = IntArray(2)
            view.getLocationOnScreen(position)
            Log.d(TAG, "getBitmapFromView: X -- ${position[0]}")
            Log.d(TAG, "getBitmapFromView: Y -- ${position[1]}")
            val bitmap: Bitmap = Bitmap.createBitmap(
                view.width, view.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            return ItemOverlay(bitmap, x = position[0], y = position[1])
        }
    }
}