package com.greentoad.turtlebody.mediapicker.util

import java.util.concurrent.TimeUnit


/**
 * Created by niraj on 15-10-2018.
 */
object UtilTime {

    @JvmStatic fun timeFormatted(millis: Long): String {
        if(millis> 3600000L) {
            return String.format("%02d:%02d:%02d",
                    TimeUnit.MILLISECONDS.toHours(millis),
                    TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)), // The change is in this line
                    TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)))
        }else{
            return String.format("%02d:%02d"
                    , TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis))
                    , TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)))
        }
    }

    @JvmStatic
    fun getRemainingHours(millis: Long): String {
        return String.format("%02d", TimeUnit.MILLISECONDS.toHours(millis))
    }

    @JvmStatic
    fun getRemainingMinutes(millis: Long): String {
        return String.format("%02d", TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)))
    }

    @JvmStatic
    fun getRemainingSeconds(millis: Long): String {
        return String.format("%02d", (TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))))
    }
}