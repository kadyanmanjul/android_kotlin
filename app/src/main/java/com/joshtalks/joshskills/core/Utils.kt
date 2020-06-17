package com.joshtalks.joshskills.core

import android.Manifest
import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.app.ActivityManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.PackageManager
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
import android.provider.Browser
import android.provider.Settings
import android.text.format.DateUtils
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.custom_ui.CustomTabHelper
import com.joshtalks.joshskills.core.datetimeutils.DateTimeStyle
import com.joshtalks.joshskills.core.datetimeutils.DateTimeUtils
import com.joshtalks.joshskills.repository.local.model.User
import com.muddzdev.styleabletoast.StyleableToast
import github.nisrulz.easydeviceinfo.base.EasyConfigMod
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.pow


private val CHAT_TIME_FORMATTER = SimpleDateFormat("hh:mm aa")
private val DD_MM_YYYY = SimpleDateFormat("DD/MM/yyyy")
private val DD_MMM = SimpleDateFormat("dd-MMM hh:mm aa")
private val MMM_DD_YYYY = SimpleDateFormat("MMM DD, yyyy")


object Utils {

    fun formatToShort(number: Int?): String {

        try {
            var value = number!! * 1.0

            val power: Int
            val suffix = " kmbt"
            var formattedNumber: String

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
        return CHAT_TIME_FORMATTER.format(old.time).toLowerCase(Locale.getDefault())
            .replace("AM", "am").replace("PM", "pm")

    }


    fun createPartFromString(descriptionString: String): RequestBody {
        return descriptionString.toRequestBody(okhttp3.MultipartBody.FORM)
    }


    fun getMessageTime(epoch: Long): String {
        val date = Date(epoch)
        return when {
            DateUtils.isToday(epoch) -> {
                CHAT_TIME_FORMATTER.format(date.time).toLowerCase(Locale.getDefault())
            }
            isYesterday(date) -> {
                "Yesterday"
            }
            else -> {
                DateTimeUtils.formatWithStyle(date, DateTimeStyle.SHORT)
            }
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
    fun getDurationOfMedia(context: Context, mediaPath: String?): Long? {
        try {
            val uri = Uri.parse(mediaPath)
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(context, uri)
            val durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            return durationStr.toLong()
        } catch (ex: Exception) {
            // ex.printStackTrace()
        }
        if (mediaPath != null) {
            return 0
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
        m = 0
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
            0, 0, bitmapimg.width,
            bitmapimg.height
        )

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawCircle(
            (bitmapimg.width / 2).toFloat(),
            (bitmapimg.height / 2).toFloat(), (bitmapimg.width / 2).toFloat(), paint
        )
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
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

    fun openUri(context: Context, uri: Uri) {
        val intent =
            Intent(ACTION_VIEW, uri)
        intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.packageName)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w("URLSpan", "Actvity was not found for intent, $intent")
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

    private fun hasInternetConnection(): Single<Boolean> {
        return Single.fromCallable {
            try {
                // Connect to Google DNS to check for connection
                val timeoutMs = 500
                val socket = Socket()
                val socketAddress = InetSocketAddress("8.8.8.8", 53)
                socket.connect(socketAddress, timeoutMs)
                socket.close()

                true
            } catch (e: IOException) {
                false
            }
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }


    private fun getBitmapFromDrawable(context: Context, drawableId: Int): Bitmap {
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

    fun getUserPrimaryEmail(context: Context): String {
        val email = "english@joshtalks.com"
        try {
            val accountManager = AccountManager.get(context)
            val account = getAccount(accountManager)
            return if (account == null) {
                email
            } else {
                account.name
            }
        } catch (ex: Exception) {
        }
        return email

    }

    private fun getAccount(accountManager: AccountManager): Account? {
        val accounts: Array<Account> = accountManager.getAccountsByType("com.google")
        val account: Account?
        account = if (accounts.isNotEmpty()) {
            accounts[0]
        } else {
            null
        }
        return account
    }

    fun isAppRunning(context: Context, packageName: String): Boolean {
        try {
            val activityManager: ActivityManager? =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
            if (activityManager != null) {
                val procInfos: List<ActivityManager.RunningAppProcessInfo> =
                    activityManager.runningAppProcesses
                for (processInfo in procInfos) {
                    if (processInfo.processName == packageName) {
                        return true
                    }
                }
            }
        } catch (ex: Exception) {
        }
        return false
    }


    fun getFileNameFromURL(url: String?): String {
        if (url.isNullOrEmpty()) {
            return EMPTY
        }
        try {
            val resource = URL(url)
            val host = resource.host
            if (host.isNotEmpty() && url.endsWith(host)) {
                return EMPTY
            }
        } catch (e: Exception) {
            return ""
        }

        val startIndex = url.lastIndexOf('/') + 1
        val length = url.length

        // find end index for ?
        var lastQMPos = url.lastIndexOf('?')
        if (lastQMPos == -1) {
            lastQMPos = length
        }

        // find end index for #
        var lastHashPos = url.lastIndexOf('#')
        if (lastHashPos == -1) {
            lastHashPos = length
        }

        // calculate the end index
        val endIndex = lastQMPos.coerceAtMost(lastHashPos)
        return url.substring(startIndex, endIndex)
    }

    fun fileUrl(localFile: String?, serverFile: String?): String? {
        return if (localFile != null && File(localFile).exists() && checkFileStorage(
                AppObjectController.joshApplication
            )
        ) {
            localFile
        } else {
            serverFile
        }

    }

    private fun checkFileStorage(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) + ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }


    fun formatDuration(duration: Int): String {
        return String.format(
            Locale.getDefault(), "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(duration.toLong()),
            TimeUnit.MILLISECONDS.toSeconds(duration.toLong()) -
                    TimeUnit.MINUTES.toSeconds(
                        TimeUnit.MILLISECONDS.toMinutes(
                            duration.toLong()
                        )
                    )
        )
    }

    fun updateTextView(textView: TextView, text: String) {
        textView.post { textView.text = text }
    }

    /*fun printAllIntent(intent: Intent) {
        val bundle = intent.extras
        if (bundle != null) {
            for (key in bundle.keySet()) {
                Log.e(
                    "all intent",
                    key + " : " + (bundle.get(key) != null ?: bundle.get(key) ?: "NULL")
                )
            }
        }
    }*/

    fun openFile(activity: Activity, url: String) {
        try {
            activity.startActivity(getRequireFileOpeningIntent(url))
        } catch (e: ActivityNotFoundException) {
            StyleableToast.Builder(activity).gravity(Gravity.CENTER)
                .text(activity.getString(R.string.viewing_support_app_not_exist)).cornerRadius(16)
                .length(Toast.LENGTH_LONG)
                .solidBackground().show()
            e.printStackTrace()
        }
    }


    fun getRequireFileOpeningIntent(url: String): Intent {
        val uri = Uri.parse(url)
        val intent = Intent(ACTION_VIEW)
        if (url.contains(".doc") || url.contains(".docx")) {
            // Word document
            intent.setDataAndType(uri, "application/msword")
        } else if (url.contains(".pdf")) {
            // PDF file
            intent.setDataAndType(uri, "application/pdf")
        } else if (url.contains(".ppt") || url.contains(".pptx")) {
            // Powerpoint file
            intent.setDataAndType(uri, "application/vnd.ms-powerpoint")
        } else if (url.contains(".xls") || url.contains(".xlsx")) {
            // Excel file
            intent.setDataAndType(uri, "application/vnd.ms-excel")
        } else if (url.contains(".rtf")) {
            // RTF file
            intent.setDataAndType(uri, "application/rtf")
        } else if (url.contains(".txt")) {
            // Text file
            intent.setDataAndType(uri, "text/plain")
        } else {
            intent.setDataAndType(uri, "*/*")
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent

    }


    fun getDeviceInfo() {
        var easyConfigMod = EasyConfigMod(AppObjectController.joshApplication)
    }

    fun getExpectedLines(textSize: Float, value: String): Int {
        val bounds = Rect()
        val paint = Paint()
        paint.textSize = textSize
        paint.getTextBounds(value, 0, value.length, bounds)
        return ceil((bounds.width().toFloat() / textSize).toDouble()).toInt()

    }


    fun isUserInDaysOld(courseCreatedDate: Date?): Pair<Boolean, Int> {
        if (courseCreatedDate == null) {
            return Pair(false, 0)
        }
        val todayDate = Date()
        val diff = todayDate.time - courseCreatedDate.time
        val offerDays =
            AppObjectController.getFirebaseRemoteConfig().getLong("COURSE_BUY_MIN_OFFER_DAYS")
        val daysDiff = offerDays - TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)

        if (daysDiff >= 0) {
            return Pair(true, daysDiff.toInt())
        }
        return Pair(false, daysDiff.toInt())

    }

    fun diffFromToday(compareDay: Date): Int {
        val todayDate = Date()
        val diff = todayDate.time - compareDay.time
        return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).toInt()
    }


    fun isSameDate(startDate: Date, endDate: Date): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance()
        cal1.time = startDate
        cal2.time = endDate
        return cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
    }

    fun dateHeaderDateFormat(date: Date): String {
        return when {
            DateUtils.isToday(date.time) -> {
                "TODAY"
            }
            isYesterday(date) -> {
                "YESTERDAY"
            }
            else -> {
                DateTimeUtils.formatWithStyle(date, DateTimeStyle.LONG)
            }
        }
    }

    fun getBitmapFromVectorDrawable(
        context: Context,
        resource: Int,
        tintColor: Int = R.color.colorPrimary
    ): Bitmap {
        val drawable = ContextCompat.getDrawable(context, resource)
        if (drawable != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                drawable.setTint(context.resources.getColor(tintColor, null))
            }
        }
        val bitmap = Bitmap.createBitmap(
            drawable!!.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun isPackageInstalled(packageName: String, context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isTrueCallerAppExist(): Boolean {
        return isPackageInstalled("com.truecaller", AppObjectController.joshApplication)
    }
}

fun milliSecondsToSeconds(time: Long): Long {
    return TimeUnit.MILLISECONDS.toSeconds(time)
}

fun convertCamelCase(string: String): String {
    val words = string.split(" ").toMutableList()
    val output = StringBuilder()
    for (word in words) {
        output.append(word.capitalize()).append(" ")
    }
    return output.toString().trim()
}

fun showToast(message: String) {
    AppObjectController.uiHandler.post {
        StyleableToast.Builder(AppObjectController.joshApplication).gravity(Gravity.BOTTOM)
            .text(message).cornerRadius(16).length(Toast.LENGTH_SHORT)
            .solidBackground().show()
    }

}

fun getUserNameInShort(): String {
    val name = User.getInstance().firstName.trim().toUpperCase()
    if (name.contains(" ")) {
        val nameSplit = name.split(" ")
        return nameSplit[0].plus(nameSplit[1])
    }
    return try {
        name.substring(0, 2)
    } catch (e: IndexOutOfBoundsException) {
        name.substring(0, name.length)
    }
}

fun hideKeyboard(activity: Activity, view: View) {
    val inputManager: InputMethodManager =
        activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun alphaAnimation(view: View) {
    val animation = AlphaAnimation(0f, 1f)
    animation.duration = 850
    animation.interpolator = AccelerateDecelerateInterpolator()
    animation.repeatCount = 12
    animation.repeatMode = Animation.REVERSE
    view.startAnimation(animation)
}

fun dateStartOfDay(): Date {
    val c = Calendar.getInstance()
    c[Calendar.HOUR_OF_DAY] = 0
    c[Calendar.MINUTE] = 0
    c[Calendar.SECOND] = 0
    c[Calendar.MILLISECOND] = 0
    return c.time
}

fun getFBProfilePicture(id: String): String {
    return "http://graph.facebook.com/$id/picture?height=800&width=800&type=normal"
}


fun isValidFullNumber(countryCode: String, number: String? = EMPTY): Boolean {
    return try {
        val phoneUtil = PhoneNumberUtil.createInstance(AppObjectController.joshApplication)
        val phoneNumber = phoneUtil.parse(countryCode + number, null)
        phoneUtil.isValidNumber(phoneNumber)
    } catch (ex: Exception) {
        ex.printStackTrace()
        true
    }
}