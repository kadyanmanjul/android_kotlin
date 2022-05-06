package com.joshtalks.joshskills.core

import android.Manifest
import android.accounts.Account
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.PictureDrawable
import android.graphics.drawable.VectorDrawable
import android.media.AudioManager
import android.media.AudioManager.STREAM_MUSIC
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Browser
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.browser.customtabs.CustomTabsIntent
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.text.toSpannable
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYou
import com.google.android.material.tabs.TabLayout
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.FREE_TRIAL_ENDED_FEATURE_LOCKED
import com.joshtalks.joshskills.core.custom_ui.CustomTabHelper
import com.joshtalks.joshskills.core.custom_ui.TextDrawable
import com.joshtalks.joshskills.core.custom_ui.custom_textview.TouchableSpan
import com.joshtalks.joshskills.core.datetimeutils.DateTimeStyle
import com.joshtalks.joshskills.core.datetimeutils.DateTimeUtils
import com.joshtalks.joshskills.core.pstn_states.PSTNState
import com.joshtalks.joshskills.core.pstn_states.PstnObserver
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.ui.voip.WebRtcService
import com.muddzdev.styleabletoast.StyleableToast
import de.hdodenhof.circleimageview.CircleImageView
import github.nisrulz.easydeviceinfo.base.EasyConfigMod
import io.michaelrocks.libphonenumber.android.NumberParseException
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import kotlinx.coroutines.*
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.*
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.nio.charset.Charset
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.roundToInt

private val CHAT_TIME_FORMATTER = SimpleDateFormat("hh:mm aa")
private val DD_MMM = SimpleDateFormat("dd-MMM hh:mm aa")
private val MMM_DD_YYYY = SimpleDateFormat("MMM DD, yyyy")
val YYYY_MM_DD = SimpleDateFormat("yyyy-MM-dd")
val DD_MM_YYYY = SimpleDateFormat("dd/MM/yyyy")
val DATE_FORMATTER = SimpleDateFormat("yyyy-MM-dd")
val DATE_FORMATTER_2 = SimpleDateFormat("dd - MMM - yyyy")

const val IMPRESSION_OPEN_FREE_TRIAL_SCREEN = "OPEN_FREE_TRIAL_SCREEN"
const val IMPRESSION_START_FREE_TRIAL = "START_FREE_TRIAL"
const val IMPRESSION_REGISTER_FREE_TRIAL = "REGISTER_FREE_TRIAL"
const val IMPRESSION_START_TRIAL_YES = "START_TRIAL_YES"
const val IMPRESSION_START_TRIAL_NO = "START_TRIAL_NO"
const val IMPRESSION_OPEN_SPEAKING_SCREEN = "OPEN_SPEAKING_SCREEN"
const val IMPRESSION_SEARCHING_SCREEN_BACK_PRESS = "SEARCHING_SCREEN_BACK_PRESS"
const val IMPRESSION_OPEN_GRAMMAR_SCREEN = "OPEN_GRAMMAR_SCREEN"
const val IMPRESSION_OPEN_READING_SCREEN = "OPEN_READING_SCREEN"
const val IMPRESSION_OPEN_ROOM_SCREEN = "OPEN_ROOM_SCREEN"
const val IMPRESSION_OPEN_VOCABULARY_SCREEN = "OPEN_VOCABULARY_SCREEN"
const val IMPRESSION_OPEN_REFERRAL_SCREEN = "OPEN_REFERRAL_SCREEN"
const val IMPRESSION_REFERRAL_CODE_COPIED = "REFERRAL_CODE_COPIED"
const val IMPRESSION_REFER_VIA_WHATSAPP_CLICKED = "REFER_VIA_WHATSAPP_CLICKED"
const val IMPRESSION_REFER_VIA_OTHER_CLICKED = "REFER_VIA_OTHER_CLICKED"
const val IMPRESSION_REFER_VIA_INBOX_ICON = "REFER_VIA_INBOX_ICON"
const val IMPRESSION_REFER_VIA_INBOX_MENU = "REFER_VIA_INBOX_MENU"
const val IMPRESSION_REFER_VIA_CONVERSATION_ICON = "REFER_VIA_CONVERSATION_ICON"
const val IMPRESSION_REFER_VIA_CONVERSATION_MENU = "REFER_VIA_CONVERSATION_MENU"

const val IMPRESSION_TRUECALLER_FREETRIAL_LOGIN = "TRUECALLER_FT_LOGIN"
const val IMPRESSION_TRUECALLER_NAMECHANGED = "NAME_CHANGED"
const val IMPRESSION_TRUECALLER_P2P = "TC_P2P_CALL"
const val IMPRESSION_ALREADY_NEWUSER = "ALREADY_NEWUSER_LOGIN"
const val IMPRESSION_ALREADY_NEWUSER_ENROLL = "ALREADY_NEWUSER_ENROLL"
const val IMPRESSION_ALREADY_NEWUSER_STARTED = "ALREADY_NEWUSER_STARTEDFT"
const val IMPRESSION_ALREADY_ALREADYUSER = "ALREADY_ALREADYUSER"
const val IMPRESSION_TC_NOT_INSTALLED_JI_HAAN= "TC_NOT_INSTALLED_JI_HAAN"
const val IMPRESSION_TC_NOT_INSTALLED= "TC_NOT_INSTALLED"
const val IMPRESSION_TC_USER_ANOTHER = "TC_USEANOTHER"

const val SPEAKING_TAB_CLICKED_FOR_FIRST_TIME = "SPEAKING_TAB_CLICKED_FOR_FIRST_TIME"
const val INTRO_VIDEO_STARTED_PLAYING = "INTRO_VIDEO_STARTED_PLAYING"
const val TIME_SPENT_ON_INTRO_VIDEO = "TIME_SPENT_ON_INTRO_VIDEO"
const val CALL_BUTTON_CLICKED_FROM_NEW_SCREEN = "CALL_BUTTON_CLICKED_FROM_NEW_SCREEN"
const val HOW_TO_SPEAK_TEXT_CLICKED = "HOW_TO_SPEAK_TEXT_CLICKED"
const val CALL_DURATION_FROM_NEW_SCREEN = "CALL_DURATION_FROM_NEW_SCREEN"
const val IMPRESSION_UNDO_ATS_OPTION = "UNDO_ATS_OPTIONS"
const val IMPRESSION_CLICKED_APPLY_COUPON = "APPLY_COUPON"
const val IMPRESSION_APPLY_COUPON_SUCCESS = "APPLY_COUPON_SUCCESS"
const val IMPRESSION_PAY_FULL_FEES = "PAY_FULL"
const val IMPRESSION_PAY_DISCOUNT = "PAY_DISCOUNT"
const val SEE_COURSE_LIST_BUTTON_CLICKED = "SEE_COURSE_LIST_BUTTON_CLICKED"

const val BUY_ENGLISH_COURSE_BUTTON_CLICKED = "BUY_ENGLISH_COURSE_BUTTON_CLICKED"
const val D2P_COURSE_SYLLABUS_OPENED = "D2P_COURSE_SYLLABUS_OPENED"
const val SYLLABUS_OPENED_AND_ENGLISH_COURSE_BOUGHT = "SYLLABUS_OPENED_AND_ENGLISH_COURSE_BOUGHT"

const val POINTS_100_OBTAINED_ENGLISH_COURSE_BOUGHT = "POINTS_100_OBTAINED_ENGLISH_COURSE_BOUGHT"
const val IMPRESSION_CLICK_RESTART_COURSE_DOT = "CLICK_RESTART_COURSE_DOT"
const val IMPRESSION_SUCCESS_RESTART_COURSE_DOT = "SUCCESS_RESTART_COURSE_DOT"
const val IMPRESSION_CLICK_RESTART_90LESSONS = "CLICK_RESTART_90LESSONS"
const val IMPRESSION_SUCCESS_RESTART_90LESSONS = "SUCCESS_RESTART_90LESSONS"

const val FREE_TRIAL_TEST_ID = "FREE_TRIAL_TEST_ID"
const val FREE_TRIAL_DEFAULT_TEST_ID = "784"

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

    const val MEGABYTE = 1024L * 1024L

    fun bytesToMB(bytes: Long): Long {
        return bytes / MEGABYTE
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
        return CHAT_TIME_FORMATTER.format(old.time).lowercase(Locale.getDefault())
            .replace("AM", "am").replace("PM", "pm")
    }

    fun createPartFromString(descriptionString: String): okhttp3.RequestBody {
        return descriptionString.toRequestBody(okhttp3.MultipartBody.FORM)
    }

    fun getMessageTime(epoch: Long, timeNeeded : Boolean = true, style: DateTimeStyle = DateTimeStyle.SHORT): String {
        val date = Date(epoch)
        return when {
            DateUtils.isToday(epoch) -> {
                if(timeNeeded)
                    CHAT_TIME_FORMATTER.format(date.time).lowercase(Locale.getDefault())
                else
                    "Today"
            }
            isYesterday(date) -> {
                "Yesterday"
            }
            else -> {
                DateTimeUtils.formatWithStyle(date, style)
            }
        }
    }

    fun getMessageTimeInHours(date: Date): String {
        return CHAT_TIME_FORMATTER.format(date.time).lowercase(Locale.getDefault())
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
            return durationStr?.toLong()
        } catch (ex: Exception) {
            ex.printStackTrace()
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
        return AppObjectController.joshApplication.getExternalFilesDir(null)?.path.toString()
            .plus("/")
            .plus(path.split(Regex("/"), 3)[2])
    }

    fun getCurrentMediaVolume(context: Context): Int {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audio.getStreamVolume(STREAM_MUSIC)
    }

    fun getCurrentMediaMaxVolume(context: Context): Int {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audio.getStreamMaxVolume(STREAM_MUSIC)
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
        return (
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, dp,
                    context.resources.displayMetrics
                ) + 0.5f
                ).roundToInt()
    }

    // Usage : Utils.sdpToPx(R.dimen._24sdp)
    fun sdpToPx(dimen: Int) = AppObjectController.joshApplication.resources.getDimension(dimen)

    fun call(context: Context, phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        intent.data = Uri.parse("tel:$phoneNumber")
        context.startActivity(intent)
    }

    fun openUrl(url: String, context: Context) {
        try {
            val intent = Intent(ACTION_VIEW).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (url.trim().startsWith("http://").not()) {
                intent.data = Uri.parse("http://" + url.replace("https://", "").trim())
            } else {
                intent.data = Uri.parse(url.trim())
            }
            context.startActivity(intent)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
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
            activity.startActivity(intent)
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
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> true
                // for other device how are able to connect with Ethernet
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
            throw IllegalArgumentException("unsupported drawable type")
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
                    R.color.colorPrimary
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
                "Today"
            }
            isYesterday(date) -> {
                "Yesterday"
            }
            else -> {
                DateTimeUtils.formatWithStyle(date, DateTimeStyle.LONG)
            }
        }
    }

    fun dateDifferenceInDays(minDate: String, maxDate: String, dateFormat: SimpleDateFormat): Long {
        return try {
            val min = dateFormat.parse(minDate)
            val max = dateFormat.parse(maxDate)
            val daysDiff = TimeUnit.DAYS.convert(max!!.time - min!!.time, TimeUnit.MILLISECONDS)
            daysDiff
        } catch (ex: Exception) {
            0L
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

    fun setImage(imageView: ImageView, url: String?) {
        url?.let {
            Glide.with(AppObjectController.joshApplication)
                .load(url)
                .fitCenter()
                .into(imageView)
        }
    }

    fun formatDate(inputDate: String, inputFormat: String, outputFormat: String): String {
        if (inputDate.isEmpty())
            return ""
        val inputFormat: DateFormat = SimpleDateFormat(inputFormat)
        val outputFormat: DateFormat = SimpleDateFormat(outputFormat)
        val inputDateStr = inputDate
        val date: Date = inputFormat.parse(inputDateStr)
        return outputFormat.format(date)
    }

    fun getDrawableFromUrl(url: String?): Drawable? {
        return try {
            val bitmap: Bitmap
            val connection: HttpURLConnection = URL(url).openConnection() as HttpURLConnection
            connection.connect()
            val input: InputStream = connection.inputStream
            bitmap = BitmapFactory.decodeStream(input)
            BitmapDrawable(Resources.getSystem(), bitmap)
        } catch (ex: Exception) {
            Timber.e(ex)
            null
        }
    }
}

fun milliSecondsToSeconds(time: Long): Long {
    return TimeUnit.MILLISECONDS.toSeconds(time)
}

fun convertCamelCase(string: String): String {
    val words = string.split(" ").toMutableList()
    val output = StringBuilder()
    for (word in words) {
        output.append(word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
            .append(" ")
    }
    return output.toString().trim()
}

fun showToast(message: String, length: Int = Toast.LENGTH_SHORT) {
    AppObjectController.uiHandler.post {
        StyleableToast.Builder(AppObjectController.joshApplication)
            .gravity(Gravity.BOTTOM)
            .text(message)
            .cornerRadius(16)
            .length(length)
            .solidBackground()
            .show()
    }
}

fun getUserNameInShort(
    name: String = User.getInstance().firstName?.trim()?.uppercase(Locale.ROOT) ?: EMPTY
): String {
    return try {
        val nameSplit = name.split(" ")
        if (nameSplit.size > 1) {
            nameSplit[0][0].plus(nameSplit[1][0].toString())
        } else {
            name.substring(0, 1)
        }
    } catch (e: IndexOutOfBoundsException) {
        name.substring(0, name.length)
    }
}

fun hideKeyboard(activity: Activity, view: View) {
    val inputManager: InputMethodManager =
        activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun hideKeyboard(context: Context) {
    if (context is Activity) {
        val activity = context
        val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        //Find the currently focused view, so we can grab the correct window token from it.
        var view = activity.currentFocus
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
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

fun getPhoneNumber() =
    when {
        User.getInstance().phoneNumber.isNullOrEmpty().not() ->
            User.getInstance().phoneNumber ?: EMPTY
        PrefManager.getStringValue(PAYMENT_MOBILE_NUMBER).isNotBlank() ->
            PrefManager.getStringValue(PAYMENT_MOBILE_NUMBER).replace(SINGLE_SPACE, EMPTY)
        else ->
            EMPTY
    }

fun getCountryIsoCode(number: String, countryRegion: String): String {
    val validatedNumber = if (number.startsWith("+")) number else "+$number"
    val phoneNumberUtil: PhoneNumberUtil =
        PhoneNumberUtil.createInstance(AppObjectController.joshApplication)
    val phoneNumber = try {
        phoneNumberUtil.parse(validatedNumber, null)
    } catch (e: NumberParseException) {
        null
    }
    return if (phoneNumber != null) {
        phoneNumberUtil.getRegionCodeForCountryCode(phoneNumber.countryCode)
    } else {
        try {
            getDefaultCountryIso(AppObjectController.joshApplication)
        } catch (ex: Exception) {
            countryRegion
        }
    }
}

fun isCallOngoing(message: Int = R.string.call_engage_record_message): Boolean {
    if (WebRtcService.isCallOnGoing.value == true) {
        showToast(
            message = AppObjectController.joshApplication.getString(message),
            length = Toast.LENGTH_SHORT
        )
        return true
    }
    return false
}

fun loadJSONFromAsset(fileName: String): String? {
    return try {
        val `is`: InputStream = AppObjectController.joshApplication.assets.open(fileName)
        val size: Int = `is`.available()
        val buffer = ByteArray(size)
        `is`.read(buffer)
        `is`.close()

        val charset: Charset = Charsets.UTF_8
        String(buffer, charset)
    } catch (ex: IOException) {
        ex.printStackTrace()
        return null
    }
}

fun ImageView.setImage(url: String, context: Context = AppObjectController.joshApplication) {
    val requestOptions = RequestOptions().placeholder(R.drawable.ic_call_placeholder)
        .error(R.drawable.ic_call_placeholder)
        .format(DecodeFormat.PREFER_RGB_565)
        .disallowHardwareConfig().dontAnimate().encodeQuality(75)
    Glide.with(context)
        .load(url)
        .optionalTransform(
            WebpDrawable::class.java,
            WebpDrawableTransformation(CircleCrop())
        )
        .apply(requestOptions)
        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
        .into(this)
}

fun CircleImageView.setImage(url: String, context: Context = AppObjectController.joshApplication) {
    val requestOptions = RequestOptions().placeholder(R.drawable.group_default_icon)
        .error(R.drawable.group_default_icon)
        .format(DecodeFormat.PREFER_RGB_565)
        .disallowHardwareConfig().dontAnimate().encodeQuality(75)
    Glide.with(context)
        .load(url)
        .optionalTransform(
            WebpDrawable::class.java,
            WebpDrawableTransformation(CircleCrop())
        )
        .apply(requestOptions)
        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
        .into(this)
}

fun ImageView.setPreviousProfileImage(url: String, context: Context = AppObjectController.joshApplication,loader: LottieAnimationView) {
    val requestOptions = RequestOptions()
        .format(DecodeFormat.PREFER_RGB_565)
        .disallowHardwareConfig().dontAnimate().encodeQuality(75)
    Glide.with(context)
        .load(url)
        .addListener(imageLoadingListener(loader))
        .optionalTransform(
            WebpDrawable::class.java,
            WebpDrawableTransformation(CircleCrop())
        )
        .apply(requestOptions)
        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
        .into(this)
}
fun imageLoadingListener(pendingImage: LottieAnimationView): RequestListener<Drawable?>? {
    return object : RequestListener<Drawable?> {
        override fun onLoadFailed(e: GlideException?, model: Any?, target: com.bumptech.glide.request.target.Target<Drawable?>?, isFirstResource: Boolean): Boolean {
            return false
        }
        override fun onResourceReady(
            resource: Drawable?,
            model: Any?,
            target: com.bumptech.glide.request.target.Target<Drawable?>?,
            dataSource: DataSource?,
            isFirstResource: Boolean
        ): Boolean {
            pendingImage.pauseAnimation()
            pendingImage.visibility = View.GONE
            return false
        }
    }
}

fun ImageView.setUserInitial(userName: String, dpToPx: Int = 16) {
    val font = Typeface.createFromAsset(
        AppObjectController.joshApplication.assets,
        "fonts/OpenSans-SemiBold.ttf"
    )
    val drawable: TextDrawable = TextDrawable.builder()
        .beginConfig()
        .textColor(ContextCompat.getColor(AppObjectController.joshApplication, R.color.white))
        .useFont(font)
        .fontSize(Utils.dpToPx(dpToPx))
        .toUpperCase()
        .endConfig()
        .buildRound(
            getUserNameInShort(userName),
            ContextCompat.getColor(AppObjectController.joshApplication, R.color.button_color)
        )
    this.background = drawable
    this.setImageDrawable(drawable)
}

fun ImageView.setUserInitialInRect(
    userName: String,
    dpToPx: Int = 16,
    radius: Int = 16,
    textColor: Int = R.color.white,
    bgColor: Int = R.color.button_color
) {
    val font = Typeface.createFromAsset(
        AppObjectController.joshApplication.assets,
        "fonts/OpenSans-SemiBold.ttf"
    )
    val drawable: TextDrawable = TextDrawable.builder()
        .beginConfig()
        .textColor(ContextCompat.getColor(AppObjectController.joshApplication, textColor))
        .useFont(font)
        .fontSize(Utils.dpToPx(dpToPx))
        .toUpperCase()
        .endConfig()
        .buildRoundRect(
            getUserNameInShort(userName),
            ContextCompat.getColor(AppObjectController.joshApplication, bgColor),
            radius
        )
    this.background = drawable
    this.setImageDrawable(drawable)
}

fun ImageView.setUserImageOrInitials(
    url: String?,
    userName: String,
    dpToPx: Int = 16,
    isRound: Boolean = false
) {
    if (url.isNullOrEmpty()) {
        setUserInitial(userName, dpToPx)
    } else {
        if (isRound) {
            val requestOptions = RequestOptions().placeholder(R.drawable.ic_call_placeholder)
                .error(R.drawable.ic_call_placeholder)
                .format(DecodeFormat.PREFER_RGB_565)
                .disallowHardwareConfig().dontAnimate().encodeQuality(75)
            Glide.with(context)
                .load(url)
                .optionalTransform(
                    WebpDrawable::class.java,
                    WebpDrawableTransformation(CircleCrop())
                )
                .circleCrop()
                .apply(requestOptions)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(this)
        } else {
            this.setImage(url)
        }
    }
}

fun ImageView.setUserImageRectOrInitials(
    url: String?,
    userName: String,
    dpToPx: Int = 16,
    isRound: Boolean = false,
    radius: Int = 16,
    textColor: Int = R.color.white,
    bgColor: Int = R.color.button_color
) {
    if (url.isNullOrEmpty()) {
        setUserInitialInRect(userName, dpToPx, radius, textColor, bgColor)
    } else {
        this.setImage(url)
    }
}

fun ImageView.setRoundImage(
    url: String,
    context: Context = AppObjectController.joshApplication,
    roundCorner: Int = 16
) {

    val multi = MultiTransformation(
        RoundedCornersTransformation(
            Utils.dpToPx(roundCorner), 0,
            RoundedCornersTransformation.CornerType.ALL
        )
    )
    Glide.with(context)
        .load(url)
        .override(Target.SIZE_ORIGINAL)
        .optionalTransform(
            WebpDrawable::class.java,
            WebpDrawableTransformation(CircleCrop())
        )
        .apply(RequestOptions.bitmapTransform(multi))
        .into(this)
}

fun TextView.textColorSet(colorCode: Int) {
    this.setTextColor(ContextCompat.getColor(AppObjectController.joshApplication, colorCode))
}

fun Long.bytesToMb(): Double {
    return (this / Utils.MEGABYTE).toDouble()
}

fun Long.bytesToKB(): Double {
    return (this / 1024).toDouble()
}

fun ImageView.setVectorImage(
    url: String,
    tintColor: Int = R.color.white,
    context: Context = AppObjectController.joshApplication
) {
    GlideToVectorYou
        .init()
        .with(context)
        .requestBuilder.load(Uri.parse(url))
        // .transition(DrawableTransitionOptions.withCrossFade())
        //    .apply(RequestOptions().centerCrop())
        .listener(object : RequestListener<PictureDrawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<PictureDrawable>?,
                isFirstResource: Boolean
            ): Boolean {
                return false
            }

            override fun onResourceReady(
                resource: PictureDrawable?,
                model: Any?,
                target: Target<PictureDrawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                resource?.setTint(ContextCompat.getColor(context, tintColor))
                this@setVectorImage.setImageDrawable(resource)
                this@setVectorImage.visibility = View.VISIBLE
                return false
            }
        })
        .into(this)
}

fun Context.changeLocale(language: String) {
    val config: Configuration = resources.configuration
    val locale = Locale(language)
    Locale.setDefault(locale)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        config.setLocale(locale)
    } else {
        config.locale = locale
    }
    createConfigurationContext(config)
    resources.updateConfiguration(config, resources.displayMetrics)
}

fun Map<String, String?>.printAll() {
    forEach {
        Timber.tag("Hashmap").e(it.key + "    " + it.value)
    }
}

fun String.getExtension(): String {
    return this.substring(this.lastIndexOf("."))
}

internal fun View?.findSuitableParent(): ViewGroup? {
    var view = this
    var fallback: ViewGroup? = null
    do {
        if (view is CoordinatorLayout) {
            // We've found a CoordinatorLayout, use it
            return view
        } else if (view is FrameLayout) {
            if (view.id == android.R.id.content) {
                // If we've hit the decor content view, then we didn't find a CoL in the
                // hierarchy, so use it.
                return view
            } else {
                // It's not the content view but we'll use it as our fallback
                fallback = view
            }
        }

        if (view != null) {
            // Else, we will loop and crawl up the view hierarchy and try to find a parent
            val parent = view.parent
            view = if (parent is View) parent else null
        }
    } while (view != null)

    // If we reach here then we didn't find a CoL or a suitable content view so we'll fallback
    return fallback
}

fun Intent.serviceStart() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        this.also { intent ->
            AppObjectController.joshApplication.startForegroundService(intent)
        }
    } else {
        AppObjectController.joshApplication.startService(this)
    }
}

fun Intent.startServiceForWebrtc() {

    if (JoshApplication.isAppVisible) {
        AppObjectController.joshApplication.startService(this)
    } else {
        ContextCompat.startForegroundService(
            AppObjectController.joshApplication,
            this
        )
    }
}

fun getTouchableSpannable(
    string: String,
    currentColor: Int,
    defaultSelectedColor: Int,
    isUnderLineEnabled: Boolean,
    clickListener: OnWordClick? = null
): TouchableSpan {
    return object : TouchableSpan(currentColor, defaultSelectedColor, isUnderLineEnabled) {
        override fun onClick(widget: View) {
            clickListener?.clickedWord(string)
        }
    }
}

suspend fun String.getSpannableString(
    separatorRegex: String,
    startSeparator: String,
    endSeparator: String,
    selectedColor: Int = Color.LTGRAY,
    defaultSelectedColor: Int = Color.BLUE,
    isUnderLineEnable: Boolean = true,
    clickListener: OnWordClick? = null
): Spannable {
    return CoroutineScope(Dispatchers.IO).async(Dispatchers.IO) {
        var sourString = this@getSpannableString
        val pattern: Pattern = Pattern.compile(separatorRegex)
        val splitted = mutableListOf<String>()
        val matcher = pattern.matcher(sourString)
        while (matcher.find()) {
            splitted.add(matcher.group().removePrefix(startSeparator).removeSuffix(endSeparator))
        }

        sourString = sourString.replace(startSeparator, EMPTY).replace(endSeparator, EMPTY)
        val generatedSpanString = SpannableStringBuilder(sourString)
        var lastIndex = 0
        splitted.forEach { word ->
            val index = sourString.indexOf(word, startIndex = lastIndex, ignoreCase = false)
            if (index <= 0) {
                return@forEach
            }
            generatedSpanString.setSpan(
                getTouchableSpannable(
                    word,
                    selectedColor,
                    defaultSelectedColor,
                    isUnderLineEnable,
                    clickListener
                ),
                index, index + word.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            lastIndex = (index + word.length)
        }
        delay(150)
        return@async generatedSpanString.toSpannable()
    }.await()
}

@SuppressLint("ClickableViewAccessibility")
fun TabLayout.updateEnable(enable: Boolean) {
    val tabStrip = this.getChildAt(0) as LinearLayout
    for (i in 0..tabStrip.childCount) {
        tabStrip.getChildAt(i).setOnTouchListener { _, _ ->
            return@setOnTouchListener enable
        }
    }
}

fun String.textDrawableBitmap(
    width: Int = 48,
    height: Int = 48,
    bgColor: Int = -1
): Bitmap? {
    val rnd = Random()
    val color = if (bgColor == -1)
        Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
    else
        bgColor

    val font = Typeface.createFromAsset(
        AppObjectController.joshApplication.assets,
        "fonts/OpenSans-SemiBold.ttf"
    )
    val drawable = TextDrawable.builder()
        .beginConfig()
        .textColor(Color.WHITE)
        .fontSize(20)
        .useFont(font)
        .toUpperCase()
        .endConfig()
        .buildRound(this, color)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

fun getRandomName(): String {
    val name = "ABCDFGHIJKLMNOPRSTUVZ"
    val ename = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    return name.random().toString().plus(ename.random().toString())
}

fun playSnackbarSound(context: Context) {
    try {
        val mediaplayer: MediaPlayer = MediaPlayer.create(
            context,
            R.raw.right_a
        )

        mediaplayer.setOnCompletionListener(object : MediaPlayer.OnCompletionListener {
            override fun onCompletion(mediaPlayer: MediaPlayer) {
                mediaPlayer.reset()
                mediaPlayer.release()
            }
        })
        mediaplayer.start()
    } catch (ex: Exception) {
        Timber.d(ex)
    }
}

fun playWrongAnswerSound(context: Context) {
    try {
        val mediaplayer: MediaPlayer = MediaPlayer.create(
            context,
            R.raw.wrong_answer
        )

        mediaplayer.setOnCompletionListener(object : MediaPlayer.OnCompletionListener {
            override fun onCompletion(mediaPlayer: MediaPlayer) {
                mediaPlayer.reset()
                mediaPlayer.release()
            }
        })
        mediaplayer.start()
    } catch (ex: Exception) {
        Timber.d(ex)
    }
}

fun String.urlToBitmap(
    width: Int = 80,
    height: Int = 80,
    context: Context = AppObjectController.joshApplication
): Bitmap? {

    val requestOptions =
        RequestOptions()
            .circleCrop()
            .format(DecodeFormat.PREFER_RGB_565)
            .disallowHardwareConfig().dontAnimate().encodeQuality(75)

    return Glide.with(context)
        .asBitmap()
        .load(this)
        .override(Utils.dpToPx(width), Utils.dpToPx(height))
        .apply(
            requestOptions
        )
        // .override(Target.SIZE_ORIGINAL)
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .optionalTransform(
            WebpDrawable::class.java,
            WebpDrawableTransformation(CircleCrop())
        ).submit().get(1500, TimeUnit.MILLISECONDS)
}

fun Int.toBoolean() = this == 1

fun Intent.printAllIntent() {
    val bundle = extras
    if (bundle != null) {
        for (key in bundle.keySet()) {
            Log.e(
                "all intent",
                key + " : " + (bundle.get(key) != null ?: bundle.get(key) ?: "NULL")
            )
        }
    }
}

private const val WIDTH_INDEX = 0
private const val HEIGHT_INDEX = 1

fun getScreenSize(context: Context): IntArray {
    val widthHeight = IntArray(2)
    widthHeight[WIDTH_INDEX] = 0
    widthHeight[HEIGHT_INDEX] = 0
    val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val display: Display = windowManager.defaultDisplay
    val size = Point()
    display.getSize(size)
    widthHeight[WIDTH_INDEX] = size.x
    widthHeight[HEIGHT_INDEX] = size.y
    if (!isScreenSizeRetrieved(widthHeight)) {
        val metrics = DisplayMetrics()
        display.getMetrics(metrics)
        widthHeight[0] = metrics.widthPixels
        widthHeight[1] = metrics.heightPixels
    }
    // Last defense. Use deprecated API that was introduced in lower than API 13
    if (!isScreenSizeRetrieved(widthHeight)) {
        widthHeight[0] = display.width // deprecated
        widthHeight[1] = display.height // deprecated
    }
    return widthHeight
}

private fun isScreenSizeRetrieved(widthHeight: IntArray): Boolean {
    return widthHeight[WIDTH_INDEX] != 0 && widthHeight[HEIGHT_INDEX] != 0
}

fun getDefaultCountryIso(context: Context): String {
    val telephoneManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
    val simState: Int? = telephoneManager?.simState
    return if (simState == 5) telephoneManager.simCountryIso.uppercase(Locale.ROOT) else Locale.getDefault().country
}

 fun checkPstnState(): PSTNState {
     Log.d(TAG, "checkPstnState: ${PstnObserver.getCurrentPstnState()}")
   return PstnObserver.getCurrentPstnState()
  }

fun getFeatureLockedText(courseId: String, name: String = EMPTY) = "$name ${AppObjectController.getFirebaseRemoteConfig().getString(FREE_TRIAL_ENDED_FEATURE_LOCKED.plus(courseId))}"
