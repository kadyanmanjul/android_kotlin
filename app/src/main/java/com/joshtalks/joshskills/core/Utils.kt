package com.joshtalks.joshskills.core

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.media.AudioManager
import android.media.AudioManager.STREAM_MUSIC
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.text.format.DateUtils
import android.util.Log
import android.util.TypedValue
import androidx.appcompat.content.res.AppCompatResources
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.custom_ui.CustomTabHelper
import com.joshtalks.joshskills.core.customlauncher.CustomTabsLauncher
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import saschpe.android.customtabs.CustomTabsHelper
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow


private val CHAT_TIME_FORMATTER = SimpleDateFormat("hh:mm aa")
private val DD_MM_YYYY = SimpleDateFormat("DD/MM/yyyy")
private val DD_MMM = SimpleDateFormat("dd-MMM hh:mm aa")


object Utils {

    fun formatToShort(number: Int?): String {

        try {
            var value = number!! * 1.0

            val power: Int
            val suffix = " kmbt"
            var formattedNumber = ""

            val formatter = DecimalFormat("#,###.#")
            power = StrictMath.log10(value).toInt()
            value /= 10.0.pow((power / 3 * 3).toDouble())
            formattedNumber = formatter.format(value)
            formattedNumber += suffix[power / 3]
            val output = if (formattedNumber.length > 4) formattedNumber.replace(
                "\\.[0-9]+".toRegex(),
                ""
            ) else formattedNumber

            return output.trim { it <= ' ' }

        } catch (e: Exception) {

        }

        return "0"
    }


    fun getDeviceId(): String {
        if (PrefManager.hasKey("deviceId"))
            return PrefManager.getStringValue("deviceId")
        val deviceId = Settings.Secure.getString(
            AppObjectController.joshApplication.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        PrefManager.put("deviceId", deviceId)

        return deviceId
    }

    fun getDeviceName(): String {
        return Build.MODEL + " - " + Build.MANUFACTURER + " - " + Build.BRAND
    }


    fun messageTimeConversion(old: Date): String {
        if (DateUtils.isToday(old.time)) {
            return CHAT_TIME_FORMATTER.format(old.time).toLowerCase(Locale.getDefault())
                .replace("AM", "am").replace("PM", "pm")
        } else {
            return DD_MMM.format(old)
        }
    }


    fun createPartFromString(descriptionString: String): RequestBody {
        return descriptionString.toRequestBody(okhttp3.MultipartBody.FORM)
    }


    fun getMessageTime(epoch: Long): String {
        val time = epoch
        val date = Date(time)

        if (DateUtils.isToday(time)) {
            return CHAT_TIME_FORMATTER.format(date.time).toLowerCase(Locale.getDefault())
        } else if (isYesterday(date)) {
            return "Yesterday"
        } else {
            return DD_MM_YYYY.format(date)
        }


    }

    fun getMessageTimeInHours(date: Date): String {
        return CHAT_TIME_FORMATTER.format(date.time).toLowerCase(Locale.getDefault())
    }


    private fun isYesterday(d: Date): Boolean {
        return DateUtils.isToday(d.time + DateUtils.DAY_IN_MILLIS)
    }

    fun isTomorrow(d: Date): Boolean {
        return DateUtils.isToday(d.time - DateUtils.DAY_IN_MILLIS)
    }

    @JvmStatic
    fun getDurationOfMedia(context: Context, mediaPath: String): Long? {
        try {
            val uri = Uri.parse(mediaPath)
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(context, uri)
            val durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            return durationStr.toLong()
        } catch (ex: Exception) {
            // ex.printStackTrace()

        }
        return null

    }

    fun convertSecondsToHMmSs(seconds: Long): String {
        val s = seconds % 60
        var m = seconds / 60 % 60
        val h = seconds / (60 * 60) % 24
        if (h > 0) {
            return String.format("%d:%02d:%02d", h, m, s)
        } else if (m > 0) {
            return String.format("%02d:%02d", m, s)
        }
        m = 0;
        return String.format("%02d:%02d", m, s)
    }


    fun getPathFromUri(path: String): String {
        return Environment.getExternalStorageDirectory().toString().plus("/")
            .plus(path.split(Regex("/"), 3)[2])

    }

    fun getCurrentMediaVolume(context: Context): Int {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audio.getStreamVolume(STREAM_MUSIC)
    }

    fun getRoundedDrawable(context: Context, iconResource: Int): RoundedBitmapDrawable {
        val res = context.resources
        val src = BitmapFactory.decodeResource(res, iconResource)
        val dr = RoundedBitmapDrawableFactory.create(res, src)
        dr.cornerRadius = Math.max(src.width, src.height) / 2.0f
        return dr
    }

    fun createCircleBitmap(bitmapimg: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(
            bitmapimg.width,
            bitmapimg.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)

        val color = -0xbdbdbe
        val paint = Paint()
        val rect = Rect(
            0, 0, bitmapimg.getWidth(),
            bitmapimg.getHeight()
        )

        paint.setAntiAlias(true)
        canvas.drawARGB(0, 0, 0, 0)
        paint.setColor(color)
        canvas.drawCircle(
            (bitmapimg.getWidth() / 2).toFloat(),
            (bitmapimg.getHeight() / 2).toFloat(), (bitmapimg.getWidth() / 2).toFloat(), paint
        )
        paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.SRC_IN))
        canvas.drawBitmap(bitmapimg, rect, rect, paint)
        return output
    }


    fun writeBitmapIntoFile(bitmap: Bitmap, filePath: String): String {
        var fOut: OutputStream? = null
        val file = File(filePath)
        fOut = FileOutputStream(file)

        bitmap.compress(
            Bitmap.CompressFormat.JPEG,
            85,
            fOut
        )
        fOut.flush()
        fOut.close()
        // bitmap.recycle()
        return filePath
    }

    fun dpToPx(dp: Int): Int {
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }

    fun pxToDp(px: Int): Int {
        return (px / Resources.getSystem().displayMetrics.density).toInt()
    }

    fun dpToPx(context: Context, dp: Float): Int {
        return Math.round(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                context.resources.displayMetrics
            ) + 0.5f
        )
    }

    fun call(context: Context, phoneNumber: String) {


        val intent = Intent(Intent.ACTION_DIAL).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        intent.data = Uri.parse("tel:$phoneNumber")
        context.startActivity(intent)
    }


    fun openUrl(url: String, activity: Activity) {

        try {
            val intent = Intent(ACTION_VIEW).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (url.trim().startsWith("http://").not()) {
                intent.data = Uri.parse("http://" + url.replace("https://", "").trim())
            } else {
                intent.data = Uri.parse(url.trim())
            }
            AppObjectController.joshApplication.startActivity(intent)
        } catch (ex: Exception) {
            ex.printStackTrace()

        }
    }

    private fun openInApplicationBrowser(url: String, activity: Activity) {
        val updateUrl: String = if (url.trim().startsWith("http://").not()) {
            "http://" + url.replace("https://", "").trim()
        } else {
            url.trim()
        }
        val builder = CustomTabsIntent.Builder()
        builder.setToolbarColor(
            ContextCompat.getColor(
                AppObjectController.joshApplication,
                R.color.colorPrimary
            )
        )
        builder.setShowTitle(true)
        builder.setExitAnimations(activity, android.R.anim.fade_in, android.R.anim.fade_out)
        val packageName = CustomTabHelper.getPackageNameToUse(activity, updateUrl)
        val customTabsIntent = builder.build()
        if (packageName == null) {
            val intent = Intent(ACTION_VIEW)
            intent.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            intent.data = Uri.parse(updateUrl)
            activity.startActivity(intent)
        } else {
            customTabsIntent.intent.setPackage(packageName)
            customTabsIntent.launchUrl(activity, Uri.parse(updateUrl))
        }

    }

    fun isInternetAvailable(): Boolean {
        val connectivityManager =
            AppObjectController.joshApplication.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw = connectivityManager.activeNetwork ?: return false
            val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
            return when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                //for other device how are able to connect with Ethernet
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            val nwInfo = connectivityManager.activeNetworkInfo ?: return false
            return nwInfo.isConnected
        }
    }

    fun isHelplineOnline(): Boolean {
        val timeFormat = "HH:mm:ss"
        val simpleDateFormat = SimpleDateFormat(timeFormat, Locale.ENGLISH)
        val startTime =
            simpleDateFormat.parse(AppObjectController.getFirebaseRemoteConfig().getString("helpline_start_time"))
        val startCalender = Calendar.getInstance()
        startCalender.time = startTime
        startCalender.add(Calendar.DATE, 1)
        val endTime =
            simpleDateFormat.parse(AppObjectController.getFirebaseRemoteConfig().getString("helpline_end_time"))
        val endCalender = Calendar.getInstance()
        endCalender.time = endTime
        endCalender.add(Calendar.DATE, 1)


        val cTime = simpleDateFormat.parse(simpleDateFormat.format(Date()))
        val cCalendar = Calendar.getInstance()
        cCalendar.time = cTime
        cCalendar.add(Calendar.DATE, 1)

        if (cCalendar.time.after(startCalender.time) && cCalendar.time.before(endCalender.time)) {
            return true
        }
        return false
    }

    fun getBitmapFromDrawable(context: Context, drawableId: Int): Bitmap {
        val drawable = AppCompatResources.getDrawable(context, drawableId)

        if (drawable is BitmapDrawable) {
            return (drawable).bitmap
        } else if (drawable is VectorDrawableCompat || drawable is VectorDrawable) {
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            return bitmap
        } else {
            throw  IllegalArgumentException("unsupported drawable type")
        }
    }

    fun openWbView(activityContext: Activity, url: String) {
        try {
            val updateUrl: String = if (url.trim().startsWith("http://").not()) {
                "http://" + url.replace("https://", "").trim()
            } else {
                url.trim()
            }
            val builder = CustomTabsIntent.Builder()
            builder.setToolbarColor(
                ContextCompat.getColor(
                    AppObjectController.joshApplication,
                    R.color.colorPrimaryDark
                )
            )
            builder.setShowTitle(true)
            val actionIntent = Intent(Intent.ACTION_DIAL).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val phoneNumber =
                AppObjectController.getFirebaseRemoteConfig().getString("helpline_number")
            actionIntent.data = Uri.parse("tel:$phoneNumber")

            val pi =
                PendingIntent.getActivity(activityContext, 0, actionIntent, 0)
            val icon = getBitmapFromDrawable(
                AppObjectController.joshApplication,
                R.drawable.ic_local_phone
            )
            builder.setActionButton(icon, "Call helpline", pi, true)

            builder.setStartAnimations(
                AppObjectController.joshApplication,
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
            builder.setExitAnimations(
                AppObjectController.joshApplication,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )

            val customTabsIntent = builder.build()
            com.joshtalks.joshskills.core.chrome.CustomTabsHelper.addKeepAliveExtra(
                activityContext,
                customTabsIntent.intent
            )
            customTabsIntent.launchUrl(activityContext, Uri.parse(updateUrl))
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

}