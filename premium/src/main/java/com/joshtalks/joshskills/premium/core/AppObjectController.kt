package com.joshtalks.joshskills.premium.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.airbnb.lottie.L
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.*
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.joshtalks.joshskills.premium.BuildConfig
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.base.constants.DIR
import com.joshtalks.joshskills.premium.core.abTest.ABTestNetworkService
import com.joshtalks.joshskills.premium.core.analytics.LogException
import com.joshtalks.joshskills.premium.core.datetimeutils.DateTimeUtils
import com.joshtalks.joshskills.premium.core.firestore.FirestoreNotificationDB
import com.joshtalks.joshskills.premium.core.firestore.NotificationAnalytics
import com.joshtalks.joshskills.premium.core.firestore.NotificationListener
import com.joshtalks.joshskills.premium.core.io.LastSyncPrefManager
import com.joshtalks.joshskills.premium.core.notification.NotificationUtils
import com.joshtalks.joshskills.premium.core.service.DownloadUtils
import com.joshtalks.joshskills.premium.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.premium.core.service.video_download.DownloadTracker
import com.joshtalks.joshskills.premium.core.service.video_download.VideoDownloadController
import com.joshtalks.joshskills.premium.core.*
import com.joshtalks.joshskills.premium.repository.local.AppDatabase
import com.joshtalks.joshskills.premium.repository.local.AppDatabaseConsistents
import com.joshtalks.joshskills.premium.repository.local.entity.ChatModel
import com.joshtalks.joshskills.premium.repository.local.model.FirestoreNewNotificationObject
import com.joshtalks.joshskills.premium.repository.local.model.Mentor
import com.joshtalks.joshskills.premium.repository.service.ChatNetworkService
import com.joshtalks.joshskills.premium.repository.service.CommonNetworkService
import com.joshtalks.joshskills.premium.repository.service.MediaDUNetworkService
import com.joshtalks.joshskills.premium.repository.service.P2PNetworkService
import com.joshtalks.joshskills.premium.repository.service.SignUpNetworkService
import com.joshtalks.joshskills.premium.repository.service.UtilsAPIService
import com.joshtalks.joshskills.premium.ui.group.data.GroupApiService
import com.joshtalks.joshskills.premium.ui.signup.SignUpActivity
import com.joshtalks.joshskills.premium.ui.voip.analytics.data.network.VoipAnalyticsService
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2.HttpUrlConnectionDownloader
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2core.Downloader
import com.tonyodev.fetch2okhttp.OkHttpDownloader
import com.userexperior.UserExperior
/*import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider*/
import io.branch.referral.Branch
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
import io.github.inflationx.viewpump.ViewPump
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit

private const val JOSH_SKILLS_CACHE = "joshskills-cache"
private const val READ_TIMEOUT = 30L
private const val WRITE_TIMEOUT = 30L
private const val CONNECTION_TIMEOUT = 30L
private const val CALL_TIMEOUT = 60L
private val IGNORE_UNAUTHORISED = setOf(
    "$DIR/reputation/vp_rp_snackbar",
    "$DIR/voicecall/agora_call_feedback/",
    "$DIR/voicecall/agora_call_feedback_submit/",
    "$DIR/voicecall/call_rating/",
    "$DIR/fpp/block/",
    "$DIR/ab_test/track_conversion/",
    "$DIR/impression/tcflow_track_impressions/",
    "$DIR/notification/client_side/",
    "$DIR/impression/track_impressions/",
    "$DIR/impression/launcher_screen/"
)

class AppObjectController {

    companion object {

        @JvmStatic
        lateinit var joshApplication: JoshApplication
        //private set

        @JvmStatic
        val appDatabase: AppDatabase by lazy { AppDatabase.getDatabase(joshApplication)!! }

        @JvmStatic
        val appDatabaseConsistents: AppDatabaseConsistents by lazy { AppDatabaseConsistents.getDatabase(joshApplication) }

        val applicationLevelScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

        val gsonMapper: Gson by lazy {
            GsonBuilder()
                .enableComplexMapKeySerialization()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                .registerTypeAdapter(Date::class.java, object : JsonDeserializer<Date> {
                    @Throws(JsonParseException::class)
                    override fun deserialize(
                        json: JsonElement,
                        typeOfT: Type,
                        context: JsonDeserializationContext
                    ): Date {
                        return Date(json.asJsonPrimitive.asLong * 1000)
                    }
                })
                .excludeFieldsWithModifiers(
                    Modifier.TRANSIENT
                )
                .setDateFormat(DateFormat.LONG)
                .setPrettyPrinting()
                .serializeNulls()
                .create()
        }

        private var isRemoteConfigInitialize = false
        private var isCrashAnalyticsInitialize = false
        private var isFontsInitialize = false
        private var isGroupInitialize = false
        private var isObservingFirestore = false
        private var isListeningBroadCast = false

        @JvmStatic
        val gsonMapperForLocal: Gson by lazy {
            GsonBuilder()
                .serializeNulls()
                .setDateFormat(DateFormat.LONG)
                .setPrettyPrinting()
                .setLenient()
                .create()
        }

        val builder: OkHttpClient.Builder by lazy {
            val builder = OkHttpClient().newBuilder()
                .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .callTimeout(CALL_TIMEOUT, TimeUnit.SECONDS)
                // .retryOnConnectionFailure(true)
                .followSslRedirects(true)
                .addInterceptor(StatusCodeInterceptor())
                .addInterceptor(HeaderInterceptor())
                .hostnameVerifier { _, _ -> true }
                .cache(cache())

            if (BuildConfig.DEBUG) {
                builder.addInterceptor(getOkhhtpToolInterceptor())
                getDebugLogsInterceptor()?.let { builder.addInterceptor(it) }
                val logging = HttpLoggingInterceptor { message ->
                    Timber.tag("OkHttp_Builder").d(message)
                }.apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
                builder.addInterceptor(logging)
                builder.addNetworkInterceptor(getStethoInterceptor())
                builder.eventListener(PrintingEventListener())
            }
            builder
        }

        val p2pBuilder: OkHttpClient.Builder by lazy {
            val builder = OkHttpClient().newBuilder()
                .connectTimeout(5L, TimeUnit.SECONDS)
                .writeTimeout(5L, TimeUnit.SECONDS)
                .readTimeout(5L, TimeUnit.SECONDS)
                .callTimeout(5L, TimeUnit.SECONDS)
                .followSslRedirects(true)
                .addInterceptor(StatusCodeInterceptor())
                .addInterceptor(HeaderInterceptor())
                .hostnameVerifier { _, _ -> true }
                .cache(cache())

            if (BuildConfig.DEBUG) {
                builder.addInterceptor(getOkhhtpToolInterceptor())
                val logging = HttpLoggingInterceptor { message ->
                    Timber.tag("OkHttp_P2P_Builder").d(message)
                }.apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
                builder.addInterceptor(logging)
                builder.addNetworkInterceptor(getStethoInterceptor())
                builder.eventListener(PrintingEventListener())
            }
            builder
        }

        val retrofit: Retrofit by lazy {
            Retrofit.Builder()
                .baseUrl(
                    if (BuildConfig.DEBUG && PrefManager.hasKey(DEBUG_BASE_URL))
                        PrefManager.getStringValue(DEBUG_BASE_URL)
                    else BuildConfig.BASE_URL
                )
                .client(builder.build())
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .addConverterFactory(GsonConverterFactory.create(gsonMapper))
                .build()
        }

        val signUpNetworkService: SignUpNetworkService by lazy {
            retrofit.create(SignUpNetworkService::class.java)
        }

        val commonNetworkService: CommonNetworkService by lazy {
            retrofit.create(CommonNetworkService::class.java)
        }

        val voipAnalyticsService: VoipAnalyticsService by lazy {
            retrofit.create(VoipAnalyticsService::class.java)
        }

        val chatNetworkService: ChatNetworkService by lazy {
            retrofit.create(ChatNetworkService::class.java)
        }

        val abTestNetworkService: ABTestNetworkService by lazy {
            retrofit.create(ABTestNetworkService::class.java)
        }

        val utilsAPIService: UtilsAPIService by lazy {
            retrofit.create(UtilsAPIService::class.java)
        }

        val groupsNetworkService: GroupApiService by lazy {
            retrofit.create(GroupApiService::class.java)
        }

        val p2pNetworkService: P2PNetworkService by lazy {
            Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(p2pBuilder.build())
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .addConverterFactory(GsonConverterFactory.create(gsonMapper))
                .build()
                .create(P2PNetworkService::class.java)
        }

        val mediaDUNetworkService: MediaDUNetworkService by lazy {
            val mediaOkhttpBuilder = OkHttpClient().newBuilder()
            mediaOkhttpBuilder.connectTimeout(45, TimeUnit.SECONDS)
                .writeTimeout(45, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .followRedirects(true)
                .addInterceptor(StatusCodeInterceptor())

            if (BuildConfig.DEBUG) {
                val logging =
                    HttpLoggingInterceptor { message ->
                        Timber.tag("OkHttp").d(message)
                    }.apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                mediaOkhttpBuilder.addInterceptor(logging)
                mediaOkhttpBuilder.addNetworkInterceptor(getStethoInterceptor())
                mediaOkhttpBuilder.addInterceptor(getOkhhtpToolInterceptor())
            }

            mediaOkhttpBuilder.addInterceptor(object : Interceptor {
                override fun intercept(chain: Interceptor.Chain): Response {
                    return chain.request().safeCall {
                        val newRequest: Request.Builder = it.newBuilder()
                        newRequest.addHeader("Connection", "close")
                        chain.proceed(newRequest.build())
                    }
                }
            })

            Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(mediaOkhttpBuilder.build())
                .build().create(MediaDUNetworkService::class.java)
        }

        @JvmStatic
        private var fetch: Fetch? = null

        @JvmStatic
        var uiHandler: Handler = Handler(Looper.getMainLooper())
            private set

        @JvmStatic
        var screenWidth: Int = 0

        @JvmStatic
        var screenHeight: Int = 0

        @JvmStatic
        lateinit var videoDownloadTracker: DownloadTracker
            private set

        @JvmStatic
        var currentActivityClass: String? = null

        @JvmStatic
        var currentPlayingAudioObject: ChatModel? = null

        @JvmStatic
        var isSettingUpdate: Boolean = false

        @JvmStatic
        var isRecordingOngoing: Boolean = false

        @JvmStatic
        @Volatile
        var appUsageStartTime: Long = 0L

        private const val cacheSize = 10 * 1024 * 1024.toLong()

        fun getVideoTracker():DownloadTracker?{
            return if(Companion::videoDownloadTracker.isInitialized) {
                videoDownloadTracker
            } else {
                null
            }
        }

        fun init() {
            CoroutineScope(Dispatchers.IO).launch {
                ActivityLifecycleCallback.register(joshApplication)
                initUserExperionCam()
                initFacebookService(joshApplication)
                observeFirestore()
            }
        }

        fun registerBroadcastReceiver() {
            if (isListeningBroadCast.not()) {
                val intentFilter = IntentFilter()
//        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE")
                intentFilter.addAction(Intent.ACTION_USER_PRESENT)
                intentFilter.addAction(Intent.ACTION_SCREEN_ON)
                intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    intentFilter.addAction(Intent.ACTION_USER_UNLOCKED)
                }
//        registerReceiver(ServiceStartReceiver(), intentFilter)
                isListeningBroadCast = true
            }
        }

        fun getLocalBroadcastManager(): LocalBroadcastManager {
            return LocalBroadcastManager.getInstance(joshApplication)
        }

        fun observeFirestore() {
            try {
                if (isObservingFirestore.not()) {
                    FirestoreNotificationDB.setNotificationListener(listener = object :
                        NotificationListener {
                        override fun onReceived(fNotification: FirestoreNewNotificationObject) {
                            CoroutineScope(Dispatchers.IO).launch {
                                val isFistTimeNotification = NotificationAnalytics().addAnalytics(
                                    notificationId = fNotification.id.toString(),
                                    mEvent = NotificationAnalytics.Action.RECEIVED,
                                    channel = NotificationAnalytics.Channel.FIRESTORE
                                )
                                if (isFistTimeNotification) {
                                    try {
                                        val nc =
                                            fNotification.toNotificationObject(fNotification.id.toString())
                                        NotificationUtils(joshApplication).sendNotification(nc)
                                    } catch (ex: java.lang.Exception) {
                                        ex.printStackTrace()
                                    }
                                }
                            }
                        }
                    })
                    isObservingFirestore = true
                }
            } catch (ex: Exception) {
                LogException.catchException(ex)
            }
        }

        fun initGroups() {
            if (isGroupInitialize.not()) {
                //EmojiManager.install(IosEmojiProvider())
                isGroupInitialize = true
            }
        }

        fun getNewArchVoipFlag() {
            try {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val resp = p2pNetworkService.getVoipNewArchFlag()
                        com.joshtalks.joshskills.voip.data.local.PrefManager.initServicePref(
                            joshApplication
                        )
                        PrefManager.put(
                            SPEED_TEST_FILE_URL,
                            resp.speedTestFile ?: "https://s3.ap-south-1.amazonaws.com/www.static.skills.com/speed_test.jpg"
                        )
                        PrefManager.put(THRESHOLD_SPEED_IN_KBPS, resp.thresholdSpeed ?: 128)
                        PrefManager.put(SPEED_TEST_FILE_SIZE, resp.testFileSize ?: 100)
                        PrefManager.put(IS_GAME_ON, resp.isGameOn ?: 1)
                        PrefManager.put(IS_LEVEL_DETAILS_ENABLED, resp.isLevelFormOn ?: 0)
                        PrefManager.put(IS_INTEREST_FORM_ENABLED, resp.isInterestFormOn ?: 0)
                        com.joshtalks.joshskills.voip.data.local.PrefManager.setBeepTimerStatus(resp.isBeepTimerEnabled ?: 0)
                    } catch (ex: Exception) {
                        when (ex) {
                            is HttpException -> {
                                showToast(joshApplication.getString(R.string.something_went_wrong))
                            }
                            is SocketTimeoutException, is UnknownHostException -> {
                                showToast(joshApplication.getString(R.string.internet_not_available_msz))
                            }
                            else -> {
                                try {
                                    FirebaseCrashlytics.getInstance().recordException(ex)
                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                }
                            }
                        }
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        @SuppressLint("RestrictedApi")
        private fun initDebugService() {
            if (BuildConfig.DEBUG) {
                StrictMode.setVmPolicy(
                    StrictMode.VmPolicy.Builder()
                        .detectAll()
                        .penaltyLog()
                        .build()
                )
                StrictMode.setThreadPolicy(
                    StrictMode.ThreadPolicy.Builder()
                        .detectAll()
                        .penaltyLog()
                        .build()
                )
                Timber.plant(Timber.DebugTree())
                Branch.enableLogging()
                Branch.enableTestMode()
                L.DBG = true
                L.setTraceEnabled(true)
            }
        }

        private fun initFacebookService(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                if (BuildConfig.DEBUG) {
                    initStethoLibrary(context)
                }
            }
        }

        fun initFonts() {
            CoroutineScope(Dispatchers.IO).launch {
                if (isFontsInitialize.not()) {
                    isFontsInitialize = true
                    ViewPump.init(
                        ViewPump.builder().addInterceptor(
                            CalligraphyInterceptor(
                                CalligraphyConfig.Builder()
                                    .setDefaultFontPath("fonts/JoshOpenSans-Regular.ttf")
                                    .setFontAttrId(R.attr.fontPath)
                                    .build()
                            )
                        ).build()
                    )
                }
            }
        }

        fun getHostOfUrl(): String {
            val aURL = URL(BuildConfig.BASE_URL)
            return aURL.host
        }

        // TODO: Need to be test the logic if internet is offline
        fun initFirebaseRemoteConfig() {
            CoroutineScope(Dispatchers.IO).launch {
                if (isRemoteConfigInitialize.not()) {
                    isRemoteConfigInitialize = true
                    val configSettingsBuilder = FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(60 * 3600)
                    getFirebaseRemoteConfig().setConfigSettingsAsync(configSettingsBuilder.build())
                    getFirebaseRemoteConfig().setDefaultsAsync(R.xml.remote_config_defaults)
                    getFirebaseRemoteConfig().fetchAndActivate().addOnFailureListener {
                        isRemoteConfigInitialize = false
                    }
                }
            }
        }

        fun configureCrashlytics() {
            CoroutineScope(Dispatchers.IO).launch {
                if (isCrashAnalyticsInitialize.not()) {
                    isCrashAnalyticsInitialize = true
                    FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
                }
            }
        }

        fun clearDownloadMangerCallback() {
            try {
                DownloadUtils.objectFetchListener.forEach { (key, value) ->
                    fetch?.removeListener(value)
                    DownloadUtils.objectFetchListener.remove(key)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        fun getFirebaseRemoteConfig(): FirebaseRemoteConfig {
            return FirebaseRemoteConfig.getInstance()
        }

        private fun initUserExperionCam() {
            UserExperior.startRecording(
                joshApplication,
                "942a0473-e1ca-40e5-af83-034cb7f57ee9"
            )
            UserExperior.setUserIdentifier(Mentor.getInstance().getId())
        }

        private fun getOkHttpDownloader(): OkHttpDownloader {
            val mediaOkhttpBuilder = OkHttpClient().newBuilder()
            mediaOkhttpBuilder.connectTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES)
                .readTimeout(1, TimeUnit.MINUTES)
                .followRedirects(true)
            val okHttpClient = mediaOkhttpBuilder.build()
            return OkHttpDownloader(
                okHttpClient,
                Downloader.FileDownloaderType.PARALLEL
            )
        }

        fun getFetchObject(): Fetch {
            if (fetch == null || fetch!!.isClosed) {
                val fetchConfiguration = FetchConfiguration.Builder(joshApplication)
                    .enableRetryOnNetworkGain(true)
                    .setDownloadConcurrentLimit(50)
                    .enableLogging(true)
                    .setAutoRetryMaxAttempts(1)
                    .setGlobalNetworkType(NetworkType.ALL)
                    .setHttpDownloader(HttpUrlConnectionDownloader(Downloader.FileDownloaderType.PARALLEL))
                    .setHttpDownloader(getOkHttpDownloader())
                    .setNamespace("JoshTalks")
                    .enableHashCheck(false)
                    .enableFileExistChecks(false)
                    .build()
                Fetch.setDefaultInstanceConfiguration(fetchConfiguration)
                fetch = Fetch.getInstance(fetchConfiguration)
            }
            return fetch as Fetch
        }

        fun createDefaultCacheDir(): String {
            val cache = File(joshApplication.cacheDir, JOSH_SKILLS_CACHE)
            if (!cache.exists()) {
                cache.mkdirs()
            }
            return getAppCachePath()
        }

        private fun cache(): Cache {
            return Cache(
                File(joshApplication.cacheDir, "api_cache"),
                cacheSize
            )
        }

        fun getAppCachePath(): String {
            return "${joshApplication.cacheDir}/${JOSH_SKILLS_CACHE}"
        }

        fun initObjectInThread() {
            Log.i(TAG, "initObjectInThread: ")
            Thread {
                DateTimeUtils.setTimeZone("UTC")
                try {
                    if (VideoDownloadController.getInstance().downloadTracker != null)
                        videoDownloadTracker = VideoDownloadController.getInstance().downloadTracker
                } catch (ex: Exception) {
                    Log.e("AppObjectController", "initObjectInThread: referrer")
                }
                InstallReferralUtil.installReferrer(joshApplication)
            }.start()
        }

        private fun getCertificatePins() =
            arrayOf(
                "sha256/dSlQHeoe4vMRu/nWoQvU9oQAMgtJ7ZJBjy8qeERS9BU=",
                "sha256/JSMzqOOrtyOT1kmau6zKhgT676hGgczD5VMdRMyJZFA=",
                "sha256/++MBgDH5WGvL9Bcn5Be30cRcL0f5O+NyoXuWtQdX1aI=",
                "sha256/KwccWaCgrnaw6tsrrSO61FgLacNgG2MMLq8GE6+oP5I="
            )

        fun releaseInstance() {
            fetch = null
            FirestoreNotificationDB.unsubscribe()
        }
    }
}

class HeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.request().safeCall {
            val newRequest: Request.Builder = it.newBuilder()
            if (PrefManager.getStringValue(API_TOKEN).isNotEmpty()) {
                newRequest.addHeader(
                    KEY_AUTHORIZATION, "JWT " + PrefManager.getStringValue(API_TOKEN)
                )
            }
            newRequest.addHeader(KEY_APP_VERSION_NAME, BuildConfig.VERSION_NAME)
                .addHeader(KEY_APP_VERSION_CODE, BuildConfig.VERSION_CODE.toString())
                .addHeader(
                    KEY_APP_USER_AGENT,
                    "APP_" + BuildConfig.VERSION_NAME + "_" + BuildConfig.VERSION_CODE.toString()
                )
                .addHeader(KEY_APP_ACCEPT_LANGUAGE, PrefManager.getStringValue(USER_LOCALE))
            if (Utils.isInternetAvailable()) {
                newRequest.cacheControl(CacheControl.FORCE_NETWORK)
            } else {
                if (it.headers.none().not()) {
                    newRequest.cacheControl(CacheControl.FORCE_CACHE)
                }
            }
            chain.proceed(newRequest.build())
        }
    }
}

inline fun Request.safeCall(block: (Request) -> Response): Response {
    try {
        return block(this)
    } catch (e: Exception) {
        e.printStackTrace()
        if (e is IOException) {
            val msg = "Unable to make a connection. Please check your internet"
            return Response.Builder()
                .request(this)
                .protocol(Protocol.HTTP_1_1)
                .code(999)
                .message(msg)
                .body("JoshSafeCallException: {${e}}".toResponseBody(null)).build()
        }
        throw e
    }
}

class StatusCodeInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.request().safeCall {
            val response = chain.proceed(it)
            if (response.code in 401..403) {
                if (Utils.isAppRunning(AppObjectController.joshApplication, AppObjectController.joshApplication.packageName)) {
                    if (IGNORE_UNAUTHORISED.none { path -> chain.request().url.toString().contains(path) }) {
                        WorkManagerAdmin.logNextActivity(chain.request().url.toString())
                        PrefManager.logoutUser()
                        LastSyncPrefManager.clear()
                        if (JoshApplication.isAppVisible) {
                            val intent = Intent(
                                AppObjectController.joshApplication,
                                SignUpActivity::class.java
                            )
                            intent.apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                putExtra("Flow", "StatusCodeInterceptor")
                            }
                            AppObjectController.joshApplication.startActivity(intent)
                        }
                    }
                }
            }
            Timber.i("Status code: %s", response.code)
            response
        }
    }
}

fun initStethoLibrary(context: Context) {
    val cls = Class.forName("com.facebook.stetho.Stetho")
    val m: Method = cls.getMethod("initializeWithDefaults", Context::class.java)
    m.invoke(null, context)
}

fun getStethoInterceptor(): Interceptor {
    val clazz = Class.forName("com.facebook.stetho.okhttp3.StethoInterceptor")
    val ctor: Constructor<*> = clazz.getConstructor()
    return ctor.newInstance() as Interceptor
}

fun getOkhhtpToolInterceptor(): Interceptor {
    val clazz = Class.forName("com.itkacher.okhttpprofiler.OkHttpProfilerInterceptor")
    val ctor: Constructor<*> = clazz.getConstructor()
    return ctor.newInstance() as Interceptor
}

fun getDebugLogsInterceptor(): Interceptor? {
    return try {
        val clazz = Class.forName("com.joshtalks.joshskills.premium.util.DebugLogsInterceptor")
        val ctor: Constructor<*> = clazz.getConstructor()
        ctor.newInstance() as Interceptor
    } catch (e: Exception) {
        null
    }
}